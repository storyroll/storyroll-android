package co.storyroll.base;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import co.storyroll.R;
import co.storyroll.activity.VideoCaptureActivity;

public class LeftActionActivity extends MenuActivity {
	@Override
    protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		ActionBar ab = getActionBar();
		ab.setIcon(R.drawable.ic_action_join);
//		ab.setHomeAsUpEnabled(false);
		ab.setHomeButtonEnabled(true);
//		ab.setDisplayHomeAsUpEnabled(false);

	}
	
	// take control of the left action item
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//        case R.id.yourRightActionItem:
//                        //do something
//                        return true;
        case android.R.id.home: // this will be our left action item
			// go to Login?
			Intent intent = new Intent(this, VideoCaptureActivity.class);
			//					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
