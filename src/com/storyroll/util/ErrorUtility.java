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
			apiError(logtag, s, status, c, true);
			return true;
		}
		return false;
	}
	
	public static void apiError(String logtag, String s, AjaxStatus status, Context c, boolean showToast) {
		String errstr = "";
		switch (status.getCode()) {
		case -101:
			errstr = "NETWORK_ERROR";
			break;
		case -102:
			errstr = "AUTH_ERROR";
			break;
		case -103:
			errstr = "TRANSFORM_ERROR";
			break;
		default:
			errstr = "";
			break;
		}
		Log.e(logtag, "API Error: " + errstr + " ("+status.getCode()+")");

		BugSenseHandler.sendException(new APIException(errstr));
		if (showToast) {
			Toast.makeText(c, s, Toast.LENGTH_SHORT).show();
		}
	}
}
