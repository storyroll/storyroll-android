package com.storyroll.util;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.androidquery.callback.AjaxStatus;
import com.storyroll.base.Constants;

public class ServerUtility {
	private static final String LOGTAG = "ServerUtility";

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
