package co.storyroll.activity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import co.storyroll.R;
import co.storyroll.base.MenuActivity;
import co.storyroll.util.AppUtility;
import com.google.analytics.tracking.android.Fields;

import java.io.File;

public class VideoSendActivity extends MenuActivity {

	protected static final String LOGTAG = "VIDEOSENT";
	private static final String SCREEN_NAME = "VideoSent";
	
	private boolean startNewMode = false;
	private long mChanId = -1L;
	private long mLastClipId = -1L;
	private long mMovieId = -1L;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
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
		aq.id(R.id.btnBack).visibility(View.INVISIBLE);
		aq.id(R.id.btnClose).visibility(View.VISIBLE);
		
		TextView upMsg = aq.id(R.id.videoUploadedMessage).getTextView();
		upMsg.setVisibility(View.VISIBLE);
		
		LayoutParams lp = upMsg.getLayoutParams();
    	int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
    	lp.width = screenWidth;
    	lp.height = screenWidth;
    	upMsg.setLayoutParams(lp);

    	aq.id(R.id.btnOK).visibility(View.VISIBLE);
		aq.id(R.id.btnOK).clicked(this, "againClickedCb");
		aq.id(R.id.btnClose).clicked(this, "closeClickedCb");
		
		if( bundle != null ) {
			Log.v(LOGTAG, "restoring activity");
			startNewMode = 	bundle.getBoolean("MODE_NEW", false);
			mChanId = 	bundle.getLong("CURRENT_CHANNEL", -1L);
			mLastClipId = bundle.getLong("RESPOND_TO_CLIP", -1L);
			mMovieId = bundle.getLong("MOVIE", -1L);
		}
		else {
			startNewMode = getIntent().getBooleanExtra("MODE_NEW", false);
			mChanId = getIntent().getLongExtra("CURRENT_CHANNEL", -1L);
			mLastClipId = getIntent().getLongExtra("RESPOND_TO_CLIP", -1L);
			mMovieId = getIntent().getLongExtra("MOVIE", -1L);
		}
	}
	
	public void againClickedCb(View view)
	{
		fireGAnalyticsEvent("ui_action", "touch", "againButton", null);
//		
//		if (startNewMode) {
//			Intent intent = new Intent(getApplicationContext(), VideoCaptureActivity.class);
//			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//			intent.putExtra("MODE_NEW", true);
//			if (mLastClipId!=-1L) {
//				intent.putExtra("RESPOND_TO_CLIP", mLastClipId);
//			}
//			if (mChanId!=-1L) {
//				intent.putExtra("CURRENT_CHANNEL", mChanId);
//			}
//			if (mMovieId  != -1L) {
//				intent.putExtra("MOVIE", mMovieId);
//			}
//			startActivity(intent);
//		}
//		else {
//			returnHomeActivity();
//		}
        nextStep();
	}

	private void closeClickedCb(View view)
	{
		fireGAnalyticsEvent("ui_action", "touch", "closeButton", null);
		// return to the last used playlist
        nextStep();
	}

	
	private String getNewFragmentFilePath() {
		return AppUtility.getAppWorkingDir(this) + File.separator+"new_fragment.mp4";

	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("MODE_NEW", startNewMode);
		outState.putLong("CURRENT_CHANNEL", mChanId);
		outState.putLong("RESPOND_TO_CLIP", mLastClipId);
		outState.putLong("MOVIE", mMovieId);
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

                nextStep();
	        	
	            return true;
	        }
	        return super.onKeyUp(keyCode, event);
	    }

    private void nextStep() {
        Intent data = new Intent();
        data.putExtra("CURRENT_CHANNEL", mChanId);
        setResult(RESULT_OK, data);
        finish();
    }
}