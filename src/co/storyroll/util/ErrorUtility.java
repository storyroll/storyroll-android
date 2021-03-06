package co.storyroll.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import co.storyroll.exception.APIException;
import com.androidquery.callback.AjaxStatus;
import com.bugsense.trace.BugSenseHandler;

public class ErrorUtility {
	
	public static boolean isAjaxErrorThenReport(String logtag, AjaxStatus status, Context c) 
	{
		if (status.getCode() != 200 && status.getCode()!=AjaxStatus.TRANSFORM_ERROR) 
		{
			String s = "Connection error, try again later";
			if (status.getCode()==AjaxStatus.NETWORK_ERROR) {
				s = "Network error, check your connection";
			}
			if (status.getCode()==500) {
				s = "Internal server error, try again later";
			}
			apiError(logtag, s, status, c, true, Log.ERROR);
			return true;
		}
		return false;
	}
	
	public static void apiError(String logtag, String s, AjaxStatus status, Context c, boolean showToast, int logLevel) {
		String errstr = "";
        Integer code = null;
		if (status!=null) {
            code = status.getCode();
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
				errstr = status.getMessage();
				break;
			}
		}

		String logStr = "API Error: " + errstr + (code==null?"":(" ("+code+")"));
		
		// only send WARN and above to BugSense
		if (logLevel>=Log.WARN) {
			BugSenseHandler.sendException(new APIException(errstr));
		}
		
		if (logLevel>=Log.ERROR)
		{
			Log.e(logtag, logStr+". HTTP response: "+errstr+(code==null?"":(", ("+code+")")));
		} else if (logLevel==Log.WARN) 
		{
			Log.w(logtag, logStr);			
		} else if (logLevel==Log.INFO) 
		{
			Log.i(logtag, logStr);
		} else 
		{
			Log.d(logtag, logStr);
		}
		
		if (showToast) {
			Toast.makeText(c, s, Toast.LENGTH_SHORT).show();
		}
	}
}
