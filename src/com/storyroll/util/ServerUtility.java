package com.storyroll.util;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.androidquery.callback.AjaxStatus;
import com.storyroll.base.Constants;

public class ServerUtility {
	private static final String LOGTAG = "ServerUtility";
	
	public static final String API_CLIP_THUMB = "clipThumb";

	public static final String API_CHAN_CLIPS = "available";

	public static final String API_UNSEEN_CLIPS = "unseenStories";

	public static final String API_UNSEEN_STORIES = "unseenStories";

	public static final String API_AVATAR = "avatar";

	public static final String API_STORY_THUMB = "storyThumb";

	public static final String API_USER_LIKES = "userLikes";

	public static final String API_ADD_VIEW = "addView";

	public static final String API_LIKE = "like";

	public static final String API_DISLIKE = "dislike";

	public static final String API_HAS_USER = "hasUser";

	public static final String API_PROFILE = "getProfile";

	public static final String API_LOGIN_VALID = "loginValid";

	public static final String API_UPDATE_PROFILE = "updateProfile";

	public static final String API_SET_AVATAR = "setAvatar";

	public static final String API_ADD_PROFILE = "addProfile";

	public static final String API_AVAILABLE_CLIPS = "available";

	public static final String API_CLIP_FILE = "fragmentFile";

	public static final String API_ADD_CLIP = "addFragment";

	public static final String API_SERVER_PROPERTIES = "getServerProperties";

	public static final String API_STORY_CAST = "getStoryCast";

	public static final String API_STORY_FILE = "storyFile";

	public static void getServerPropertiesCb(String url, JSONObject json, AjaxStatus status, Context ctx)
	{
		if(json != null)
        {               
            //successful ajax call
        	Log.i(LOGTAG, "getServerPropertiesCb success: "+json.toString());
        	// update property
        	String ls="";
        	try {
				ls = json.getString(Constants.SERV_PREF_VIDEO_DURATION);
				int l = Integer.valueOf(ls);
				if (l<100) { l=l*1000; };
				CameraUtility.VIDEO_LENGTH = l;
				Log.v(LOGTAG,  "server property "+Constants.SERV_PREF_VIDEO_DURATION+": "+CameraUtility.VIDEO_LENGTH);
			} catch (JSONException e) {
				ErrorUtility.apiError(LOGTAG, "server property "+Constants.SERV_PREF_VIDEO_DURATION+" JSONException, "+e.getMessage(), status, ctx, false, Log.WARN);
			} catch (NumberFormatException e) {
				ErrorUtility.apiError(LOGTAG, "server property "+Constants.SERV_PREF_VIDEO_DURATION+" incorrect: "+ls, status, ctx, false, Log.WARN);
			}
        }
		else
        {          
            //ajax error
			ErrorUtility.apiError(LOGTAG, "API call getServerProperties failed.", status, ctx, false, Log.WARN);
        }
	}
}
