package com.storyroll.base;

import java.util.Date;

import com.androidquery.callback.AjaxStatus;
import com.bugsense.trace.BugSense;
import com.bugsense.trace.BugSenseHandler;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.storyroll.R;
import com.storyroll.activity.HelpActivity;
import com.storyroll.activity.LoginActivity;
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
    protected static boolean isTrial=false;

	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        isTrial = getIntent().getBooleanExtra("TRIAL", false);
        if (!isTrial) {
        	isTrial = getUuid()==null;
        }
        
        // Setup search by username on Android
		BugSenseHandler.setUserIdentifier(getUuid());
	}
	
    @Override
    public void onStart() {
      super.onStart();
      // The rest of your onStart() code.
      getGTracker().activityStart(this);  // Add this method.
      
      // Send a screen view when the Activity is displayed to the user.
      getGTracker().send(MapBuilder.createAppView().build());
    }
    
    @Override
    public void onStop() {
      super.onStop();
      // The rest of your onStop() code.
      getGTracker().activityStop(this);  // Add this method.
    }
    
    // ------- protected methods
    
    protected void fireGAnalyticsEvent(String category, String action, String label, Long value) {
    	getGTracker().send(MapBuilder
			    .createEvent(category, action, label, value)
			    .build()
			);
    }
    
    // ------- menus
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        if (isTrial) {
        	inflater.inflate(R.menu.trial_activity_menu, menu);
        }
        else {
        	inflater.inflate(R.menu.home_activity_menu, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		Intent intent;
		if (item.getItemId() == android.R.id.home) // this will be our left action item
		{
			fireGAnalyticsEvent("ui_action", "touch", "home", null);
			return super.onOptionsItemSelected(item);
			
		} else if (item.getItemId() == R.id.action_join) 
		{
			 onJoinPressed(null, null);
			 return true;
			 
		} else if (item.getItemId() == R.id.action_new) 
		{
			 onNewPressed(null);
			 return true;
			 
		} else if (item.getItemId() == R.id.action_help) {
			intent = new Intent (this, HelpActivity.class);
			startActivity(intent);
			return true;
			
		} else if (item.getItemId() == R.id.action_profile) {
			intent = new Intent(this, ProfileActivity.class);
			startActivity(intent);
			return true;
			
		} else if (item.getItemId() == R.id.action_settings) {
			intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
			
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
	
    /*-- callbacks & helpers --*/
	
	protected void onNewPressed(Long chanId) {
		Intent intent;
		if (isTrial) {
			fireGAnalyticsEvent("ui_action", "touch", "joinRoll_trial", null);
			intent = new Intent(this, LoginActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		}
		else {
			fireGAnalyticsEvent("ui_action", "touch", "joinRoll_regged", null);
			intent = new Intent(this, VideoCaptureActivity.class);
			intent.putExtra("MODE_NEW", true);
			if (chanId!=null && chanId!=-1L) {
				intent.putExtra("CURRENT_CHANNEL", chanId);
			}
		}
		
		startActivity(intent);
	}

	
	protected void onJoinPressed(Long clipId, Long chanId){
		Intent intent;
//		if (isTrial) {
//			fireGAnalyticsEvent("ui_action", "touch", "joinRoll_trial", null);
//			intent = new Intent(this, LoginActivity.class);
//			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//		}
//		else 
		{
			fireGAnalyticsEvent("ui_action", "touch", "joinRoll_regged", null);
			intent = new Intent(this, VideoCaptureActivity.class);
		}
		if (clipId!=null && clipId!=-1L) {
			intent.putExtra("RESPOND_TO_CLIP", clipId);
		}
		if (chanId!=null && chanId!=-1L) {
			intent.putExtra("CURRENT_CHANNEL", chanId);
		}
		
		startActivity(intent);
	}
    
	protected EasyTracker getGTracker() {
    	return EasyTracker.getInstance(this);
    }
    
	protected String getUuid() {
		SharedPreferences settings = getSharedPreferences(Constants.PREF_PROFILE_FILE, 0);
		String uuid = settings.getString(Constants.PREF_EMAIL, null);
		String username = settings.getString(Constants.PREF_USERNAME, null);
		Log.v(LOGTAG, "uuid: " + uuid + ", username: " + username);
		return uuid;
	}
	
}
