package com.storyroll.base;

import com.storyroll.R;
import com.storyroll.activity.ProfileActivity;
import com.storyroll.activity.SettingsActivity;
import com.storyroll.activity.VideoCaptureActivity;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class MenuFragmentActivity extends FragmentActivity {
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
}
