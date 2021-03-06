package co.storyroll.shake;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
	
    private static final String LOGTAG = "BootReceiver";

	public void onReceive(Context context, Intent intent) {
    	Log.i(LOGTAG, "BootReceiver.onReceive: starting storyroll.shake.ShakeService");
        Intent i = new Intent(context, ShakeService.class);
        context.startService(i);
    }
}