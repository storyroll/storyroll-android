/*******************************************************************************
 * Copyright 2014 StoryRoll
 * 
 ******************************************************************************/
package co.storyroll.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Environment;
import android.util.Log;
import co.storyroll.MainApplication;
import co.storyroll.R;
import co.storyroll.activity.MainChannelsActivity;
import co.storyroll.base.Constants;
import com.androidquery.auth.BasicHandle;
import com.androidquery.auth.FacebookHandle;
import com.androidquery.util.AQUtility;
import com.flurry.android.FlurryAgent;

import java.io.File;
import java.util.Locale;

//import com.androidquery.simplefeed.data.Entity;
//import com.androidquery.simplefeed.enums.FeedMode;

public class AppUtility {

	public static final String USER_NAME = "aq.fb.user.name";
	public static final String APP_PACKAGE_NAME = "co.storyroll";
	
	//private static String PERMISSIONS = "read_stream,read_friendlists,manage_friendlists,manage_notifications,publish_stream,publish_checkins,offline_access,user_about_me,friends_about_me,user_activities,friends_activities,user_checkins,friends_checkins,user_events,friends_events,user_groups,friends_groups,user_interests,friends_interests,user_likes,friends_likes,user_notes,friends_notes,user_photos,friends_photos,user_status,friends_status,user_videos,friends_videos";
	
	//	Note that Facebook access token expire every two hours unless "offline_access" is granted.
//	private static final String PERMISSIONS = "email,offline_access";
	private static final String PERMISSIONS = "email,user_about_me,user_location";
	
//	public final static String API_URL="http://192.168.1.64:8080/storyroll/api/";
	
	public final static Class ACTIVITY_HOME = MainChannelsActivity.class;
//	public final static Class ACTIVITY_HOME = TabbedChannelsActivity.class;
	private static final String LOGTAG = "AppUtility";

	public static FacebookHandle makeHandle(Activity act){
		
		FacebookHandle handle = new FacebookHandle(act, Constants.APP_ID, PERMISSIONS);
		handle.message(act.getString(R.string.connecting_facebook));

		return handle;
		
	}

    public static BasicHandle makeBasicHandle(Activity act) {
        BasicHandle handle = new BasicHandle(getUuid(act), getPassword(act));

        return handle;
    }

    public static String getUuid(Activity act) {
        SharedPreferences settings = act.getSharedPreferences(Constants.PREF_PROFILE_FILE, 0);
        String uuid = settings.getString(Constants.PREF_EMAIL, null);
        String username = settings.getString(Constants.PREF_USERNAME, null);
//        Log.i(LOGTAG, "uuid: " + uuid + ", username: " + username);
        return uuid;
    }

    public static String getPassword(Activity act) {
        SharedPreferences settings = act.getSharedPreferences(Constants.PREF_PROFILE_FILE, 0);
        String password = settings.getString(Constants.PREF_PASSWORD, null);
//        Log.v(LOGTAG, "pass: " + password);
        return password;
    }

    public static void logout(Activity act){
		
		makeHandle(act).unauth();		
		//handle = null;
		PrefUtility.put(USER_NAME, (String) null);
	}
	
	
	private static Locale locale;
	
	public static void presetLocale(Context context){
		
		try{
		
			String value = PrefUtility.get("locale", null);
		
			AQUtility.debug("user locale", value);
			
			if(value != null && value.length() > 0){
				locale = new Locale(value);
				resetLocale(context);
			}
			
			
		}catch(Exception e){
			AQUtility.report(e);
		}
	}
	
	public static void setLocale(String lang){
		
		if(lang.equals("")){
			locale = Locale.getDefault();
		}else{		
			locale = new Locale(lang);
		}
		
	}
	
	public static void resetLocale(Context context){
		
		
		Configuration config = new Configuration();
		config.locale = locale;
		context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
		
		AQUtility.debug("reset locale", locale);
	
		
	}
	
	public static void reportRemote(Throwable ex){
		
		try{
			
			AQUtility.debug("reporting");
			String trace = Log.getStackTraceString(ex);
			FlurryAgent.onError(ex.getMessage(), trace, ex.getClass().getName());
		}catch(Exception e){
			e.printStackTrace();
		}
		
		
	}
	
	public static String getUserName(){
    	return PrefUtility.get(AppUtility.USER_NAME, MainApplication.get(R.string.me));
    }
	
	public static String getUserName(String fallback){
    	return PrefUtility.get(AppUtility.USER_NAME, fallback);    
    }
	
	public static String getAppWorkingDir(Context ctx) {
		String state = Environment.getExternalStorageState();
		File cd = null;
		if (Environment.MEDIA_MOUNTED.equals(state)) 
		{
			Log.v(LOGTAG, "Media mounted");
			cd = Environment.getExternalStorageDirectory();
		}
		else {
			Log.w(LOGTAG, "Media not mounted");
			cd = ctx.getApplicationContext().getFilesDir();
		}
		return  cd + File.separator + AppUtility.APP_PACKAGE_NAME;
	}
	
	public static String getVideoCacheDir(Context ctx) {
		String state = Environment.getExternalStorageState();
		File cd = null;
		if (Environment.MEDIA_MOUNTED.equals(state)) 
		{
			Log.v(LOGTAG, "Media mounted");
		    // We can read and write the media
			cd = ctx.getApplicationContext().getExternalCacheDir();
		}
		else {
			Log.w(LOGTAG, "Media not mounted");
			cd = ctx.getApplicationContext().getFilesDir();
		}
		
		return cd.getPath();
	}

	public static void purgeProfile(Context context) {
    	SharedPreferences settings = context.getSharedPreferences(Constants.PREF_PROFILE_FILE, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.remove(Constants.PREF_EMAIL);
		editor.remove(Constants.PREF_USERNAME);
		editor.remove(Constants.PREF_AVATAR_URL);
		editor.remove(Constants.PREF_AUTH_METHOD);
		editor.remove(Constants.PREF_IS_LOGGED_IN);
		editor.remove(Constants.PREF_FIRST_RUN);
		editor.remove(Constants.PREF_GCM_REG_ID);
		editor.commit();
	}
	
    public static boolean isLoggedIn(){
		SharedPreferences settings = MainApplication.getContext().getSharedPreferences(Constants.PREF_PROFILE_FILE, 0);
		String uuid = settings.getString(Constants.PREF_EMAIL, null);
		Boolean loggedIn = settings.getBoolean(Constants.PREF_IS_LOGGED_IN, false);
		return (uuid!=null) && loggedIn;
    }
    
    public static boolean isFirstRun(){
    	SharedPreferences p = MainApplication.getContext().getSharedPreferences(Constants.PREF_PROFILE_FILE, 0);
    	boolean firstRun = p.getBoolean(Constants.PREF_FIRST_RUN, true);
    	p.edit().putBoolean(Constants.PREF_FIRST_RUN, false).commit();
    	return firstRun;
    }
    
    public static boolean isShakeServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("ShakeService".equals(service.service.getClassName())) {
            	Log.v(LOGTAG, "ShakeService already running");
                return true;
            }
        }
        Log.v(LOGTAG, "ShakeService not running");
        return false;
    }



//	public static FeedMode getDefaultMode(){
//    	return PrefUtility.getEnum(FeedMode.class, FeedMode.NEWS);
//    }
	
	
//    public static Entity getDefaultSource(){
//    	return getDefaultSource(getDefaultMode());
//    }
//    
//    public static Entity getDefaultSource(FeedMode mode){
//    	
//    	Entity entity = new Entity();
//    	entity.setId("me");    	
//    	entity.setName(PrefUtility.get(AppUtility.USER_NAME, MainApplication.get(R.string.me)));
//    	
//    	String type = "feed";
//		if(FeedMode.NEWS.equals(mode)){
//			type = "home";
//		}
//    	entity.setMode(type);
//    	
//    	return entity;
//    }
	
}
