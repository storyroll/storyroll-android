package com.storyroll.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.androidquery.callback.AjaxStatus;
import com.bugsense.trace.BugSenseHandler;
import com.storyroll.exception.APIException;

public class ErrorUtility {
	
	public static boolean isAjaxErrorThenReport(String logtag, AjaxStatus status, Context c) 
	{
		if (status.getCode() != 200 && status.getCode()!=AjaxStatus.TRANSFORM_ERROR) 
		{
			String s = "Connection error, try again later";
			if (status.getCode()==AjaxStatus.NETWORK_ERROR) {
				s = "Network error, check your connection";
			}
			apiError(logtag, s, status.getCode(), c, true);
			return true;
		}
		return false;
	}
	
	public static void apiError(String logtag, String s, Integer errorCode, Context c, boolean showToast) {
		Log.e(logtag, "API Error: " + s+", code "+errorCode);
		BugSenseHandler.sendException(new APIException(errorCode+""));
		if (showToast) {
			Toast.makeText(c, s, Toast.LENGTH_SHORT).show();
		}
	}
}
