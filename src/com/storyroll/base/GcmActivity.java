package com.storyroll.base;

// TODO: move to activities
import java.io.IOException;

import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

import com.androidquery.callback.AjaxStatus;
import com.bugsense.trace.BugSenseHandler;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.storyroll.R;
import com.storyroll.util.AppUtility;
import com.storyroll.util.PrefUtility;
import com.storyroll.util.ServerUtility;

public class GcmActivity extends BaseActivity {
    protected static final String LOGTAG = "GcmActivity";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

	protected GoogleCloudMessaging gcm;

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    protected void gcmRegisterInBackground() {
        new AsyncTask<Void, Void, String>() {
            Exception e = null;

            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
                    }
                    String regid = gcm.register(Constants.GCM_SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    sendGcmRegistrationIdToBackend(regid, getUuid());

                    // For this demo: we don't need to send it because the device will send
                    // upstream messages to a server that echo back the message using the
                    // 'from' address in the message.

                    // Persist the regID - no need to register again.
//                    storeRegistrationId(context, regid);
                    // TODO ...
                } catch (IOException ex) {
                	e = ex;
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                    
                    // TODO
                    BugSenseHandler.sendException(ex);
                }
                
                return "Currently app is not registering with GCM";
            }

            @Override
            protected void onPostExecute(String msg) {
//                mDisplay.append(msg + "\n");
            	if (e != null) {
            		Log.e(LOGTAG, msg, e);
            	}
            	else {
            		Log.i(LOGTAG, msg);
            	}
            }
        }.execute(null, null, null);
    }


	protected void sendGcmRegistrationIdToBackend(String regid, String email) {
		// update profile with new reg id
		String apiRegUrl = PrefUtility.getApiUrl(ServerUtility.API_PROFILE_UPDATE, "uuid="+email+"&registrationId="+regid);
		aq.progress(R.id.progress).ajax(apiRegUrl, JSONObject.class, this, "updateProfileGcmRegCb");
	}
	
	public void updateProfileGcmRegCb(String url, JSONObject json, AjaxStatus status){
		Log.v(LOGTAG, "updateProfileGcmRegCb");
		fireGAnalyticsEvent("profile", "gcm_save", json != null?"success":"fail", null);
		if (isAjaxErrorThenReport(status)) return;
		
        if(json != null){
            //successful ajax call
        	Log.v(LOGTAG, "updateProfileGcmRegCb success");
        }else{          
        	apiError(LOGTAG, "Could not update GCM registration id in profile", status, true, Log.WARN);
        }
	}
	
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    protected boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.w(LOGTAG, "This device is not supported (GooglePlayServices).");
                BugSenseHandler.sendException(new Exception(GooglePlayServicesUtil.getErrorString(resultCode)));
//                finish();
            }
            return false;
        }
        return true;
    }
}
