package co.storyroll.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;
import co.storyroll.R;
import co.storyroll.base.GcmActivity;
import co.storyroll.model.Profile;
import co.storyroll.util.AppUtility;
import co.storyroll.util.DataUtility;
import co.storyroll.util.PrefUtility;
import co.storyroll.util.ServerUtility;
import com.androidquery.auth.BasicHandle;
import com.androidquery.auth.FacebookHandle;
import com.androidquery.callback.AjaxStatus;
import com.google.analytics.tracking.android.Fields;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends GcmActivity {
	private final static String LOGTAG = "LOGIN";
	private final static String SCREEN_NAME = "Login";
    public final static String EXTRA_OVERRIDE_BACKPRESS = "overrideBackPress";
    public final static String EXTRA_LOGOUT = "logout";

    private final static String facebookGraphUrl = "https://graph.facebook.com/me?fields=first_name,last_name,name,email,location,birthday,gender";
	private final int ACTIVITY_SSO = 1000;

	private FacebookHandle facebookHandle;
	private Profile profile = null;
	private boolean connectedViaFacebook = false;
    private boolean isOverrideBackPress = true;
    private boolean isRecreatedAfterFacebookSuccess = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		isHomeEnabled = !getIntent().getBooleanExtra(EXTRA_LOGOUT, false);
        isOverrideBackPress = getIntent().getBooleanExtra(EXTRA_OVERRIDE_BACKPRESS, true);
        super.onCreate(savedInstanceState);

        Log.v(LOGTAG, "isRecreatedAfterFacebookSuccess: "+isRecreatedAfterFacebookSuccess);
        if (isRecreatedAfterFacebookSuccess)
        {
            setContentView(R.layout.activity_login_facebook);
        }
        else {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_login);

            aq.id(R.id.facebook_button).clicked(this, "facebookButtonClicked");
            aq.id(R.id.done_button).clicked(this, "doneButtonClicked");
        }
        facebookHandle = AppUtility.makeHandle(this);

		// Fields set on a tracker persist for all hits, until they are
	    // overridden or cleared by assignment to null.
		getGTracker().set(Fields.SCREEN_NAME, SCREEN_NAME);
	}

	  
	// - - - callbacks
	
	public void facebookButtonClicked(View button)
    {
        enableButtons(false);
        authFacebookSSO();
    }

    public void authFacebookSSO(){
		Log.v(LOGTAG, "authFacebookSSO");
		
		facebookHandle.sso(ACTIVITY_SSO);
		Log.d(LOGTAG, "SSO available: " + facebookHandle.isSSOAvailable());

		aq.auth(facebookHandle).progress(R.id.progress).ajax(facebookGraphUrl, JSONObject.class, this, "facebookProfileCb");
	}
	
	public void doneButtonClicked(View button){
		// login via API
		profile = new Profile();
		profile.email = aq.id(R.id.email).getText().toString().trim();
		profile.password = aq.id(R.id.password).getText().toString().trim();
		profile.authMethod = Profile.AUTH_EMAIL;

        // update handle
//        basicHandle = new BasicHandle(profile.email, profile.password);
		
		if (TextUtils.isEmpty(profile.password) || TextUtils.isEmpty(profile.email) ) {
            Toast.makeText(aq.getContext(), R.string.msg_password_email_required, Toast.LENGTH_SHORT).show();
			return;
		}
		String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_USER_EXISTS, "uuid=" + profile.email);
		aq.ajax(apiUrl, JSONObject.class, LoginActivity.this, "userExistsCb");
	}

	// - - - callbacks & helpers

    private void enableButtons(boolean enable) {
        aq.id(R.id.facebook_button).enabled(enable);
        aq.id(R.id.done_button).enabled(enable);
    }

    //  callback checks if a user is registered with StoryRoll, if so - proceeds
    public void userExistsCb(String url, JSONObject json, AjaxStatus status) throws JSONException
    {
        Log.v(LOGTAG, "userExistsCb");
        if (isAjaxErrorThenReport(status)) return;

        boolean userExists = false;
        if(json!=null){
            userExists = json.getBoolean("result");
        }
        Log.i(LOGTAG, "userExistsCb user exists: "+ userExists);

        if (userExists) {
            // update auth handle
            Log.v(LOGTAG, "creating handle for "+profile.email+" with: "+profile.password);
            basicHandle = new BasicHandle(profile.email, profile.password);
            // now get the profile
            String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_PROFILE, "uuid=" + profile.email);
            aq.auth(basicHandle).ajax(apiUrl, JSONObject.class, LoginActivity.this, "getSrProfileCb");
        }
        else {
            // user not in db, go to registration
            nextActionRegister();
        }
    }

    // this callback checks if given user exists and then checks its login
    public void getSrProfileCb(String url, JSONObject json, AjaxStatus status) throws JSONException{
        Log.v(LOGTAG, "getSrProfileCb");
        if (status.getCode()==401)
        {
            // Existing user, auth error, means password incorrect
            fireGAnalyticsEvent("login", "valid", "false", null);
            Toast.makeText(aq.getContext(), "Password incorrect, review and try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isAjaxErrorThenReport(status)) return;

        fireGAnalyticsEvent("login", "valid", "true", null);
        if(json != null)
        { // everything ok
            Log.v(LOGTAG, "user exists");
            // check login
            String md5 = DataUtility.md5(profile.password);
            Log.d(LOGTAG, "md5: "+md5);

            // update profile
            profile = populateProfileFromSrJson(json, true, profile.password);
            profile.loggedIn = true;
            persistProfile(profile);

            // double checking the gcm reg id, and update if necessary
            checkAndUpdateGcmReg();

            nextActionHome();
        }else{
            // shouldn't really happen
            apiError(LOGTAG, "Error logging in", status, true, Log.ERROR);
        }
    }

    public void facebookProfileCb(String url, JSONObject json, AjaxStatus status) {
		Log.v(LOGTAG, "facebookProfileCb");
		fireGAnalyticsEvent("facebook", "login", json==null?"fail":"success", null);

        if (status.getCode()==AjaxStatus.AUTH_ERROR) {
            Toast.makeText(this, "Facebook error. It's either Facebook misbehaving, or you have entered your login information incorrectly. We'll be investigating the error immediately, please try again later.", Toast.LENGTH_LONG).show();
        }
    	if (isAjaxErrorThenReport(status)) {
            enableButtons(true);
            return;
        }
            
        if(json != null){
			try {
                    //successful ajax call
					Log.v(LOGTAG, "facebookProfileCb: Facebook connected");
                    Toast.makeText(aq.getContext(), "Facebook connected", Toast.LENGTH_SHORT).show();
                    connectedViaFacebook = true;
                    
                    populateProfileFromFbJson(json);
                    
                    // query API, user exists?
    				String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_USER_EXISTS, "uuid="+profile.email);
					aq.progress(R.id.progress).ajax(apiUrl, JSONObject.class, LoginActivity.this, "hasFbUserInSrCb");
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
                    enableButtons(true);
				}
        }else{
        	apiError(LOGTAG, "Facebook Error: "+status.getMessage(), status, true, Log.ERROR);
            enableButtons(true);
        }
    }
    
    // this callback checks if a FB user in registered with StoryRoll
	public void hasFbUserInSrCb(String url, JSONObject json, AjaxStatus status) throws JSONException
	{
		Log.v(LOGTAG, "hasFbUserInSrCb");
        enableButtons(true);
		if (isAjaxErrorThenReport(status)) return;
		
		boolean userExists = false;
		if(json!=null){
			userExists = json.getBoolean("result");
		}
		Log.i(LOGTAG, "hasFbUserInSrCb user exists: "+ userExists);

		if (userExists) {
			// ...
			profile.loggedIn = true;
            // regenerate password
            profile.password = DataUtility.getMD5Hex(profile.email);
			persistProfile(profile);
			
			// TODO: double check the gcm reg id, and update if necessary
			checkAndUpdateGcmReg();
			
			nextActionHome();
		}
		else {
			// user not in db, go to registration
			nextActionRegister();
		}
    }


	
	private void nextActionRegister() {
		if (connectedViaFacebook) {
			profile.authMethod = Profile.AUTH_FACEBOOK;
		}
		else {
			profile.authMethod = Profile.AUTH_EMAIL;
		}
		Intent intent =  new Intent(this, RegistrationActivity.class);
		intent.putExtra("registration", true);
		intent.putExtra("profile", profile);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}
	
	private void nextActionHome() {
		Intent intent = new Intent(getApplicationContext(), AppUtility.ACTIVITY_HOME);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}
    
    // populate profile from facebook response
	protected void populateProfileFromFbJson(JSONObject json) throws JSONException{
//		String tb = ImageUtility.getFbProfileTb(handle);
		Log.i(LOGTAG, json.toString());
		if (profile==null) {
			profile = new Profile();
		}
		profile.email = json.getString("email");
		profile.username = json.getString("first_name");
        if (json.has("location")) {
            JSONObject location = json.getJSONObject("location");
            profile.location = location.getString("name");
        }
		// TODO: age, gender
        // ...
	}
	
	private void checkAndUpdateGcmReg() {
		if (profile.gcmRegistrationId==null) {
			
			// register with GCM and update required field
			Log.v(LOGTAG, "register with GCM");
			
	        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
	        if (checkPlayServices()) {
	            gcm = GoogleCloudMessaging.getInstance(this);
//	            regid = getRegistrationId(context);
	//
//	            if (regid.isEmpty()) {
	                gcmRegisterInBackground();
//	            }
	        } else {
	            Log.w(LOGTAG, "No valid Google Play Services APK found.");
	        }

		}
		// TODO: else, check if reg id changed, or update regardless
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v(LOGTAG, "onActivityResult");

	        switch(requestCode) {
	                
	        case ACTIVITY_SSO: {
	    		Log.v(LOGTAG, "ACTIVITY_SSO");

	                if(facebookHandle != null){
	                	facebookHandle.onActivityResult(requestCode, resultCode, data);   
	                }

                    isRecreatedAfterFacebookSuccess = facebookHandle.authenticated();
        			fireGAnalyticsEvent("facebook", "authenticated", facebookHandle.authenticated()+"", null);

	        		if (facebookHandle.authenticated())
                    {
                        setContentView(R.layout.activity_login_facebook);
	        			aq.auth(facebookHandle).progress(R.id.progress).ajax(facebookGraphUrl, JSONObject.class, this, "facebookProfileCb");
	        		}
	        		else {
	        			apiError(LOGTAG, "Facebook not authenticated", null, false, Log.ERROR);
	        		}

	                break;
	        }
	
	        }
	}
	
	// back button
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            event.startTracking();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.v(LOGTAG, "onKeyUp, isOverrideBackPress: "+isOverrideBackPress);
        if (keyCode == KeyEvent.KEYCODE_BACK && event.isTracking()
                && !event.isCanceled() /* && isOverrideBackPress */) {
            // Back button press complete, handle
    		Intent intent = new Intent(Intent.ACTION_MAIN);
    		intent.addCategory(Intent.CATEGORY_HOME);
    		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    		startActivity(intent);
        	return true;
        }
        return super.onKeyUp(keyCode, event);
    }


}
