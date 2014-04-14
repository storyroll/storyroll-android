package com.storyroll.base;

public interface Constants {

	public final static String APP_ID = "556141531146952";
	public static final String BUGSENSE_API_KEY = "b3f4e407";
	public static final String FLURRY_API_KEY = "D54R7MJVB3KQ2MWN3YJ5";

	public final static String API_URL_STAGING="http://storyroll.vibe.lt/storyroll/api/";
	public final static String API_URL_AWS="http://api.storyroll.co/storyroll/api/";
	public final static String API_URL_DEV="http://dev.storyroll.co/storyroll/api/";

	/**
     * StoryRoll sender ID here. This is the project number 
     * from the API Console.
     */
	public static final String GCM_SENDER_ID = "737051971497";

	public int  ACTIVITY_SSO = 10000;
	public int ACTIVITY_GALLERY = 10001;
	public int ACTIVITY_CAMERA = 10002;
	public int ACTIVITY_CHECKIN = 10003;
	public int ACTIVITY_FRIENDS = 10004;
	public int ACTIVITY_POST = 10005;
	
	public static final String PREF_PROFILE_FILE = "com.storyroll.preferences";
	public static final String PREF_EMAIL = "sr.pref.email";
	public static final String PREF_USERNAME = "sr.pref.username";
	public static final String PREF_IS_LOGGED_IN = "sr.pref.loggedin";
	public static final String PREF_AUTH_METHOD = "sr.pref.auth.method";
	public static final String PREF_AVATAR = "sr.pref.avatar";
	public static final String PREF_LOCATION = "sr.pref.location";
	public static final String PREF_GCM_REG_ID = "sr.pref.gcm";
	
	public static final String PREF_SSO = "sf.pref.sso";
	public static final String PREF_NID = "aq.fb.nid";
	public static final String PREF_ACTION_ITEM = "aq.fb.item";
	public static final String PREF_LAST_NOTI = "aq.fb.noti";
	

	public static final boolean IS_NEW_STORY_INDICATED = false;

	public static final String SERV_PREF_VIDEO_DURATION = "storyroll.app.video.duration";
}
