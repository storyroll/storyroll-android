package com.storyroll.base;

import java.util.Date;

import com.androidquery.callback.AjaxStatus;
import com.bugsense.trace.BugSense;
import com.bugsense.trace.BugSenseHandler;
import com.storyroll.R;
import com.storyroll.activity.ProfileActivity;
import com.storyroll.activity.SettingsActivity;
import com.storyroll.activity.VideoCaptureActivity;
import com.storyroll.util.AppUtility;
import com.storyroll.util.ErrorUtility;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class MenuFragmentActivity extends FragmentActivity {
	
	private static final String LOGTAG = "MenuFragment";

	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup search by username on Android
		BugSenseHandler.setUserIdentifier(getUuid());
	}
    // ------- menus
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
	  // Handle item selection
	  Intent intent;
	  if (item.getItemId() == android.R.id.home) // this will be our left action item 
	  {
		// TODO: go to Login?
			intent = new Intent(this, VideoCaptureActivity.class);
			//					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
          return true;
	  } 
	  else
//	    if (item.getItemId() == R.id.action_join) {
//			// go to Login
//			intent = new Intent(this, VideoCaptureActivity.class);
//			//					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//			startActivity(intent);
//			return true;
//		} else 
				if (item.getItemId() == R.id.action_profile) {
			intent = new Intent (this, ProfileActivity.class);
			startActivity(intent);
			return true;
		} else if (item.getItemId() == R.id.action_settings) {
			intent = new Intent (this, SettingsActivity.class);
			startActivity(intent);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
    }
	
    /*-- callbacks & helpers --*/
    
	protected String getUuid() {
		SharedPreferences settings = getSharedPreferences(Constants.PREF_PROFILE_FILE, 0);
		String uuid = settings.getString(Constants.PREF_EMAIL, null);
		String username = settings.getString(Constants.PREF_USERNAME, null);
		Log.v(LOGTAG, "uuid: " + uuid + ", username: " + username);
		return uuid;
	}
	
}
