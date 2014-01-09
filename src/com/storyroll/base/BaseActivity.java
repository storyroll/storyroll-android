package com.storyroll.base;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.androidquery.util.AQUtility;
import com.storyroll.PQuery;
import com.storyroll.R;
import com.storyroll.model.Profile;
import com.storyroll.util.ActionBarUtility;

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
		if (settings.contains(Constants.PREF_AVATAR)) {
			p.avatar = settings.getInt(Constants.PREF_AVATAR, 0);
		}
		else { p.avatar = null; }
		Log.v(LOGTAG, "profile: "+p.toString());
		return p;
	}
	
	protected void persistProfile(Profile profile) {
		persistProfile(profile.email, profile.username, profile.avatar, profile.authMethod, profile.loggedIn);
	}
	protected void persistProfile(String email, String username, Integer avatar, Integer authMethod, Boolean isLoggedIn) {
		SharedPreferences settings = getSharedPreferences(Constants.PREF_PROFILE_FILE, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(Constants.PREF_EMAIL, email);
		editor.putString(Constants.PREF_USERNAME, username);
		editor.putInt(Constants.PREF_AUTH_METHOD, authMethod);
		editor.putBoolean(Constants.PREF_IS_LOGGED_IN, isLoggedIn);
		if (avatar==null) {
			editor.remove(Constants.PREF_AVATAR);
		}
		else {
			editor.putInt(Constants.PREF_AVATAR, avatar);
	}
	
		editor.commit();	
	}
	
}
