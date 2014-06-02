package co.storyroll.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkUtility {
	
	public static boolean isWifiConnected(Context context) {
		  ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		  NetworkInfo ni = cm.getActiveNetworkInfo();
		  if (ni != null && ni.getType()==ConnectivityManager.TYPE_WIFI) {
		   // There is an active Wifi network.
		   return true;
		  } else
		   return false;
	}
}
