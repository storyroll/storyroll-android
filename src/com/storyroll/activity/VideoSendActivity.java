package com.storyroll.activity;

import java.io.File;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import com.google.analytics.tracking.android.Fields;
import com.storyroll.R;
import com.storyroll.base.MenuActivity;
import com.storyroll.util.AppUtility;

public class VideoSendActivity extends MenuActivity {

	protected static final String LOGTAG = "VIDEOSENT";
	private static final String SCREEN_NAME = "VideoSent";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_videocapture);
		
		// Fields set on a tracker persist for all hits, until they are
	    // overridden or cleared by assignment to null.
	    getGTracker().set(Fields.SCREEN_NAME, SCREEN_NAME);

//		requestWindowFeature(Window.FEATURE_NO_TITLE);		
//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		getActionBar().hide();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		
		aq.id(R.id.videoPlayerView).visibility(View.INVISIBLE);
		aq.id(R.id.videocapReadyMessage).visibility(View.INVISIBLE);
		aq.id(R.id.backButton).visibility(View.INVISIBLE);
		
		TextView upMsg = aq.id(R.id.videoUploadedMessage).getTextView();
		upMsg.setVisibility(View.VISIBLE);
		
		LayoutParams lp = upMsg.getLayoutParams();
    	int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
    	lp.width = screenWidth;
    	lp.height = screenWidth;
    	upMsg.setLayoutParams(lp);

		aq.id(R.id.redButtonText).text(R.string.again);
		aq.id(R.id.redButton).clicked(this, "againClickedCb");
		
		// extract frame? using ffmpeg
//		Bitmap bm = MediaUtils.getVideoFrame(getNewFragmentFilePath(), 1000);
//		aq.id(R.id.preview).image(bm);
   	
	}
	
	public void againClickedCb(View view)
	{
		fireGAnalyticsEvent("ui_action", "touch", "againButton", null);
		Intent intent = new Intent(getApplicationContext(), VideoCaptureActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	
	private String getNewFragmentFilePath() {
		return AppUtility.getAppWorkingDir(this) + File.separator+"new_fragment.mp4";

	}
	
	   @Override
	    public boolean onKeyDown(int keyCode, KeyEvent event) {
	        if (keyCode == KeyEvent.KEYCODE_BACK
	                && event.getRepeatCount() == 0) {
	            event.startTracking();
	            return true;
	        }
	        return super.onKeyDown(keyCode, event);
	    }

	    @Override
	    public boolean onKeyUp(int keyCode, KeyEvent event) {
	        if (keyCode == KeyEvent.KEYCODE_BACK && event.isTracking()
	                && !event.isCanceled()) {
	            // Back button press complete, handle
	        	
	    		fireGAnalyticsEvent("ui_action", "click", "SystemBack", null);
	    		fireGAnalyticsEvent("fragment_workflow", "videoUpload", "SystemBack", null);
	        	
        		Intent intent = new Intent(getApplicationContext(), AppUtility.ACTIVITY_HOME);
        		intent.addCategory(Intent.CATEGORY_HOME);
//        		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        		startActivity(intent);
	        	
	        	
	            return true;
	        }
	        return super.onKeyUp(keyCode, event);
	    }
}