package com.storyroll.base;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.androidquery.callback.AjaxStatus;
import com.androidquery.util.AQUtility;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;
import com.storyroll.PQuery;
import com.storyroll.model.Profile;
import com.storyroll.shake.ShakeService;
import com.storyroll.util.ActionBarUtility;
import com.storyroll.util.AppUtility;
import com.storyroll.util.ErrorUtility;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class BaseActivity extends Activity {
	
	private static final String LOGTAG = "BaseActivity";
    protected static boolean isTrial=false;
	
	public com.storyroll.PQuery aq;

	private static final boolean FORCE_SHAKE_SERVICE_ALIVE = false;

	@Override
    protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		aq = new PQuery(this);
//		if(isActionBar()){
	    	ActionBarUtility.initCustomActionBar(this, true);
//	    }
	    isTrial = getIntent().getBooleanExtra("TRIAL", false);
		
	    // start shake check service if it's not running
		if ( FORCE_SHAKE_SERVICE_ALIVE &&
				//AppUtility.isFirstRun() && 
				!AppUtility.isShakeServiceRunning(this)) 
		{
			Log.i(LOGTAG, "Starting ShakeService");
			Intent i = new Intent(getApplicationContext(), ShakeService.class);
	        this.startService(i);
		}
	}
	
//    @Override
//	protected void onDestroy(){
//    	
//    	super.onDestroy();
//    	aq.dismiss();
//    	
//    	if(isTaskRoot()){
//    		AQUtility.cleanCacheAsync(this);
//    	}
//    	
//    }
    
    @Override
	protected void onDestroy(){
        
        super.onDestroy();
        aq.dismiss();
        
        if(isTaskRoot())
        {
        	Log.i(LOGTAG, "cache cleanup");
            //clean the file cache with advance option
            long triggerSize = 3000000; //starts cleaning when cache size is larger than 3M
            long targetSize = 2000000;      //remove the least recently used files until cache size is less than 2M
            AQUtility.cleanCacheAsync(this, triggerSize, targetSize);
        }
        
    }
    
    @Override
    public void onStart() {
      super.onStart();
      // The rest of your onStart() code.
      getGTracker().activityStart(this);  // Add this method.
      
      // Send a screen view when the Activity is displayed to the user.
      getGTracker().send(MapBuilder.createAppView().build());
    }
    
    @Override
    public void onStop() {
      super.onStop();
      // The rest of your onStop() code.
      getGTracker().activityStop(this);  // Add this method.
    }
    
    // ------- protected methods
    
    protected void fireGAnalyticsEvent(String category, String action, String label, Long value) {
    	getGTracker().send(MapBuilder
			    .createEvent(category, action, label, value)
			    .build()
			);
    }
    
    // - - - helper methods
    
    protected EasyTracker getGTracker() {
    	return EasyTracker.getInstance(this);
    }
    
    public boolean isRoot(){
    	return false;
    }
    
    public boolean isLoggedIn(){
//		SharedPreferences settings = getSharedPreferences(Constants.PREF_PROFILE_FILE, 0);
//		String uuid = settings.getString(Constants.PREF_EMAIL, null);
//		Boolean loggedIn = settings.getBoolean(Constants.PREF_IS_LOGGED_IN, false);
//		return (uuid!=null) && loggedIn;
    	return AppUtility.isLoggedIn();
    }
    
	protected String getUuid() {
		SharedPreferences settings = getSharedPreferences(Constants.PREF_PROFILE_FILE, 0);
		String uuid = settings.getString(Constants.PREF_EMAIL, null);
		String username = settings.getString(Constants.PREF_USERNAME, null);
		Log.i(LOGTAG, "uuid: " + uuid + ", username: " + username);
		return uuid;
	}
	
	protected Profile getPersistedProfile() {
		Profile p = new Profile();
		SharedPreferences settings = getSharedPreferences(Constants.PREF_PROFILE_FILE, 0);
		p.email = settings.getString(Constants.PREF_EMAIL, "");
		p.username = settings.getString(Constants.PREF_USERNAME, "");
		p.authMethod = settings.getInt(Constants.PREF_AUTH_METHOD, Profile.AUTH_UNKNOWN);
		p.loggedIn = settings.getBoolean(Constants.PREF_IS_LOGGED_IN, false);
		p.location = settings.getString(Constants.PREF_LOCATION, "");
		p.gcmRegistrationId = settings.getString(Constants.PREF_GCM_REG_ID, "");
		if (settings.contains(Constants.PREF_AVATAR)) {
			p.avatar = settings.getInt(Constants.PREF_AVATAR, 0);
		}
		else { p.avatar = null; }
		Log.v(LOGTAG, "profile: "+p.toString());
		return p;
	}
	
	protected void persistProfile(Profile profile) {
		persistProfile(profile.email, profile.username, profile.avatar, profile.authMethod, profile.location, profile.loggedIn, profile.gcmRegistrationId);
	}
	protected void persistProfile(String email, String username, Integer avatar, Integer authMethod, String location, Boolean isLoggedIn, String GCMRegId) {
		SharedPreferences settings = getSharedPreferences(Constants.PREF_PROFILE_FILE, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(Constants.PREF_EMAIL, email);
		editor.putString(Constants.PREF_USERNAME, username);
		editor.putInt(Constants.PREF_AUTH_METHOD, authMethod);
		editor.putBoolean(Constants.PREF_IS_LOGGED_IN, isLoggedIn);
		editor.putString(Constants.PREF_LOCATION, location);
		editor.putString(Constants.PREF_GCM_REG_ID, GCMRegId);
		if (avatar==null) {
			editor.remove(Constants.PREF_AVATAR);
		}
		else {
			editor.putInt(Constants.PREF_AVATAR, avatar);
	}
	
		editor.commit();	
	}
	
    // populate profile from StoryRoll API response
	public Profile populateProfileFromSrJson(JSONObject json, boolean addAuthMethod) throws JSONException
	{
		// TODO: more sensible, null-proof field reading
		Profile	profile = new Profile();
		profile.email = json.getString("uuid");
		profile.username = json.getString("username");
		// TODO
//		profile.birthday = json.getString("birthday");
//		profile.gender = json.getString("gender");

		// in case no name set yet, make one from email
		if (profile.username==null || "".equals(profile.username.trim())) {
			profile.username = profile.email.trim().split("@")[0];
		}
		profile.location = json.getString("location");
		if (addAuthMethod) {
			profile.authMethod = json.getInt("authMethod");
		}
		// set avatar id
		if (json.has("avatar") && !json.isNull("avatar")) {
			JSONObject avatarJson = json.getJSONObject("avatar");
			profile.avatar = avatarJson.getInt("id");
		}
		// set gcm reg id
		if (json.has("gcmRegistrationId") && !json.isNull("gcmRegistrationId")) {
			profile.gcmRegistrationId = json.getString("gcmRegistrationId").trim();
			if ("null".equals(profile.gcmRegistrationId) || "".equals(profile.gcmRegistrationId) )
				profile.gcmRegistrationId = null;
		}
		return profile;
	}

	// error reporter wrappers
	protected boolean isAjaxErrorThenReport(AjaxStatus status) {
		return ErrorUtility.isAjaxErrorThenReport(LOGTAG, status, this);
	}
	
	protected void apiError(String logtag, String s, AjaxStatus status, boolean toast, int logLevel) {
		ErrorUtility.apiError(logtag, s, status, this, toast, logLevel);
	}
	
	public void swallowCb(String url, JSONObject json, AjaxStatus status) {
		// just swallow it
	}

}
