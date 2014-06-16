package co.storyroll.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import co.storyroll.R;
import co.storyroll.model.Profile;
import co.storyroll.util.DataUtility;
import co.storyroll.util.PrefUtility;
import co.storyroll.util.ServerUtility;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.bugsense.trace.BugSenseHandler;
import com.google.analytics.tracking.android.Fields;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

public class RegistrationActivity extends ProfileActivity {
	private final String LOGTAG = "REGISTER";
	private static final String SCREEN_NAME = "Registration";
	
	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		
		// Fields set on a tracker persist for all hits, until they are
	    // overridden or cleared by assignment to null.
	    getGTracker().set(Fields.SCREEN_NAME, SCREEN_NAME);
	}

	
	// - - - callbacks
	
	@Override
	public void doneButtonClicked(View button)
    {
		Log.v(LOGTAG, "doneButtonClicked");
        aq.id(R.id.done_button).enabled(false);
		
		fireGAnalyticsEvent("ui_activity", "click", "doneButton", null);
		
		String formUsername = aq.id(R.id.user_name).getText().toString().trim();
		boolean unameChanged = !formUsername.equals(profile.username);
		Log.v(LOGTAG, "unameChanged: "+unameChanged);
		
		// TODO: store/send to server new/updated profile
		profile.email = aq.id(R.id.email).getText().toString().trim();
		profile.username = formUsername;
		profile.location = aq.id(R.id.location).getText().toString().trim();
		profile.password = aq.id(R.id.password).getText().toString().trim();
		
		if (registration) 
		{
			if (profile.isAuthEmail() && (TextUtils.isEmpty(profile.password) || TextUtils.isEmpty(profile.email)) ) 
			{
				Toast.makeText(aq.getContext(), R.string.msg_password_email_required, Toast.LENGTH_SHORT).show();
                aq.id(R.id.done_button).enabled(true);
                return;
			}
            if (profile.isAuthFacebook())
            {
                // todo: hack
                // fix in the future to use Facebook Auth
                // generates "recoverable" password, so that a user is able to pass base-authentication
                profile.password = DataUtility.getMD5Hex(profile.email);
                Log.v(LOGTAG, "pass: "+profile.password);
            }
			Log.d(LOGTAG, "profile: "+profile.toString()+", params: "+profile.toParamString(false, true, true));
			doServerProfileAdd(profile);
		}
		else 
		{
			persistProfile(profile);
			profile = getPersistedProfile();
            // this should pass, because the BasicAuth data (u:p) was stored in context erlier
			aq.auth(basicHandle).progress(R.id.progress).ajax(PrefUtility.getApiUrl(
					ServerUtility.API_PROFILE_UPDATE, profile.toParamString(unameChanged, false, false)),
					JSONObject.class, this, "updateProfileCb");
		}		
	}

//    private void doServerProfileAdd() {
//
//        aq.progress(R.id.progress).ajax(PrefUtility.getApiUrl(
//                        ServerUtility.API_PROFILE_ADD, profile.toParamString(false, true, true)),
//                JSONObject.class, this, "createProfileCb");
//    }

    // collect additional id information and send everything to server
    private void doServerProfileAdd(Profile profile)
    {
        // collect id strings
        Set<String> idStrings = new HashSet<String>();

        // Returns the phone number string for line 1, for example, the MSISDN for a GSM phone. Return null if it is unavailable.
        TelephonyManager tMgr = (TelephonyManager)getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        String phoneNumber = tMgr.getLine1Number();
        Log.d(LOGTAG, "phone num: "+phoneNumber);
        if (!TextUtils.isEmpty(phoneNumber)) {
            idStrings.add(phoneNumber);
        }

        // account emails

        AccountManager manager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
        Account[] list = manager.getAccounts();
        for (Account account:list) {
            if (DataUtility.isEmailValid(account.name)
                    && !account.name.equals(profile.email)) // ignore uuid from the account list
            {
                idStrings.add(account.name.toLowerCase());
            }
        }

        try {
            // put them in request body
            JSONArray idsJson = new JSONArray();
            for (String s : idStrings) idsJson.put(DataUtility.md5(s));

            Log.v(LOGTAG, "uploading " + idsJson.length() + " id strings");
            StringEntity entity = new StringEntity(idsJson.toString());
            String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_PROFILE_ADD, profile.toParamString(false, true, true));

            // perform server call
            aq.progress(R.id.progress).post(apiUrl, "application/json", entity, JSONObject.class,
                    new  AjaxCallback<JSONObject>() {
                        @Override
                        public void callback(String url, JSONObject json, AjaxStatus status) {
                            createProfileCb(url, json, status);
                        }
                    }
                );
        }
        catch (UnsupportedEncodingException e)
        {
            BugSenseHandler.sendException(e);
            e.printStackTrace();
        }
    }
    
	@Override
	public void createProfileCb(String url, JSONObject json, AjaxStatus status) {
        Log.v(LOGTAG, "createProfileCb");
        // profile register successfull or fail?

        fireGAnalyticsEvent("profile", "create", json != null ? "success" : "fail", null);

        if (updateProfileGeneral(url, json, status)) {

            // registered successfull, now register with GCM and update required field
            Log.v(LOGTAG, "register success, now register with GCM");

            // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
            if (checkPlayServices()) {
                gcm = GoogleCloudMessaging.getInstance(this);
                //            regid = getRegistrationId(context);
                //
                //            if (regid.isEmpty()) {
                gcmRegisterInBackground();
                //            }
            } else {
                Log.w(LOGTAG, "No valid Google Play Services APK found.");
            }
        }
        aq.id(R.id.done_button).enabled(true);
    }
	

}
