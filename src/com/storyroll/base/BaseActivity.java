package com.storyroll.base;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
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
import com.storyroll.util.ActionBarUtility;
import com.storyroll.util.ErrorUtility;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class BaseActivity extends Activity {
	
	private static final String LOGTAG = "BaseActivity";
	
	public com.storyroll.PQuery aq;
	
	@Override
    protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		aq = new PQuery(this);
//		if(isActionBar()){
	    	ActionBarUtility.initCustomActionBar(this);
//	    }
		
	}
	
    @Override
    public void onDestroy(){
    	
    	super.onDestroy();
    	aq.dismiss();
    	
    	if(isTaskRoot()){
    		AQUtility.cleanCacheAsync(this);
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
		SharedPreferences settings = getSharedPreferences(Constants.PREF_PROFILE_FILE, 0);
		String uuid = settings.getString(Constants.PREF_EMAIL, null);
		Boolean loggedIn = settings.getBoolean(Constants.PREF_IS_LOGGED_IN, false);
		return (uuid!=null) && loggedIn;
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
		p.email = settings.getString(Constants.PREF_EMAIL, null);
		p.username = settings.getString(Constants.PREF_USERNAME, null);
		p.authMethod = settings.getInt(Constants.PREF_AUTH_METHOD, Profile.AUTH_UNKNOWN);
		p.loggedIn = settings.getBoolean(Constants.PREF_IS_LOGGED_IN, false);
		p.location = settings.getString(Constants.PREF_LOCATION, null);
		if (settings.contains(Constants.PREF_AVATAR)) {
			p.avatar = settings.getInt(Constants.PREF_AVATAR, 0);
		}
		else { p.avatar = null; }
		Log.v(LOGTAG, "profile: "+p.toString());
		return p;
	}
	
	protected void persistProfile(Profile profile) {
		persistProfile(profile.email, profile.username, profile.avatar, profile.authMethod, profile.location, profile.loggedIn);
	}
	protected void persistProfile(String email, String username, Integer avatar, Integer authMethod, String location, Boolean isLoggedIn) {
		SharedPreferences settings = getSharedPreferences(Constants.PREF_PROFILE_FILE, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(Constants.PREF_EMAIL, email);
		editor.putString(Constants.PREF_USERNAME, username);
		editor.putInt(Constants.PREF_AUTH_METHOD, authMethod);
		editor.putBoolean(Constants.PREF_IS_LOGGED_IN, isLoggedIn);
		editor.putString(Constants.PREF_LOCATION, location);
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
		Profile	profile = new Profile();
		profile.email = json.getString("uuid");
		profile.username = json.getString("username");
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
		return profile;
	}

	// error reporter wrappers
	protected boolean isAjaxErrorThenReport(AjaxStatus status) {
		return ErrorUtility.isAjaxErrorThenReport(LOGTAG, status, this);
	}
	
	protected void apiError(String logtag, String s, AjaxStatus status, boolean toast, int logLevel) {
		ErrorUtility.apiError(logtag, s, status, this, toast, logLevel);
	}
}
