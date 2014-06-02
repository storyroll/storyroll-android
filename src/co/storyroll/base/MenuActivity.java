package co.storyroll.base;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import co.storyroll.R;
import co.storyroll.activity.*;

public class MenuActivity extends GcmActivity {
	private static final String TAG = "MenuActivity";
	MenuItem settings, profile;

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
//	    if (item.getItemId() == R.id.action_join) {
////	    	onJoinPressed(null, null);
//	    	Log.e(LOGTAG, "can't join video from this activity!");
//			return true;
//		} else 
			if (item.getItemId() == R.id.action_help) {
			intent = new Intent (this, HelpActivity.class);
			startActivity(intent);
			return true;
		} else if (item.getItemId() == R.id.action_profile) {
			intent = new Intent (this, ProfileActivity.class);
			startActivity(intent);
			return true;
		} else if (item.getItemId() == R.id.action_settings) {
			intent = new Intent (this, SettingsActivity.class);
			startActivity(intent);
			return true;
		} else if (item.getItemId() == R.id.action_log) {
			intent = new Intent (this, LogReadActivity.class);
			startActivity(intent);
			return true;	
//		} else if (item.getItemId() == R.id.action_rolls_favs || item.getItemId() == R.id.action_rolls_my
//				|| item.getItemId() == R.id.action_rolls_new || item.getItemId() == R.id.action_rolls_top) {
//			intent = new Intent (this, RollFlipPlayActivity.class);
//			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//			startActivity(intent);
//			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
    }
	
    /*-- callbacks & helpers --*/

	protected void onJoinPressed(Long clipId, Long chanId){
		Intent intent;
		if (isTrial) {
			intent = new Intent(this, LoginActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		}
		else {
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
}
