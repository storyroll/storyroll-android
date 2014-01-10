package com.storyroll.activity;

import java.io.File;

import org.ffmpeg.android.ShellUtils.ShellCallback;
import org.json.JSONException;
import org.json.JSONObject;

import ru.jecklandin.stickman.vp.ProcessingService;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.androidquery.auth.FacebookHandle;
import com.androidquery.callback.AjaxStatus;
import com.storyroll.R;
import com.storyroll.base.BaseActivity;
import com.storyroll.util.AppUtility;
import com.storyroll.util.SystemUiHider;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class LaunchActivity extends BaseActivity implements ShellCallback {
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	protected static final long SPLASH_SCREEN_WAIT = 3000;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;
	
	private final String LOGTAG = "LAUNCH";
	
	private FacebookHandle handle;
	private Thread timerThread;
	private boolean loginChecked = false;
	private Button bt;
	private static boolean activityClosed = false; 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_launcher);
		
		// update loggedIn flag - in case user was deleted
		if (isLoggedIn()) {
			String apiUrl = AppUtility.API_URL + "hasUser?uuid="+getUuid();
//			String apiUrl = AppUtility.API_URL + "getProfile?uuid="+getUuid();

//			aq.ajax(apiUrl, JSONObject.class, this, "checkUserExistsCb");
			aq.progress(R.id.progressMarker).ajax(apiUrl, JSONObject.class, this, "checkUserExistsBooleanCb");
		}

		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.fullscreen_content);

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, contentView,
				HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider
				.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
					// Cached values.
					int mControlsHeight;
					int mShortAnimTime;

					@Override
					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
					public void onVisibilityChange(boolean visible) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
							// If the ViewPropertyAnimator API is available
							// (Honeycomb MR2 and later), use it to animate the
							// in-layout UI controls at the bottom of the
							// screen.
							if (mControlsHeight == 0) {
								mControlsHeight = controlsView.getHeight();
							}
							if (mShortAnimTime == 0) {
								mShortAnimTime = getResources().getInteger(
										android.R.integer.config_shortAnimTime);
							}
							controlsView
									.animate()
									.translationY(visible ? 0 : mControlsHeight)
									.setDuration(mShortAnimTime);
						} else {
							// If the ViewPropertyAnimator APIs aren't
							// available, simply show or hide the in-layout UI
							// controls.
							controlsView.setVisibility(visible ? View.VISIBLE
									: View.GONE);
						}

						if (visible && AUTO_HIDE) {
							// Schedule a hide().
							delayedHide(AUTO_HIDE_DELAY_MILLIS);
						}
					}
				});
		
		getActionBar().hide();
		
		// Set up the user interaction to manually show or hide the system UI.
		contentView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		bt = (Button) findViewById(R.id.proceed_button);
		bt.setOnTouchListener(mDelayHideTouchListener);

		bt.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				nextAction();        
			}
		});
		
		Button bt2 = (Button) findViewById(R.id.test_button);
		bt2.setOnTouchListener(mDelayHideTouchListener);

		bt2.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				ffmpegTester();
		        
			}
		});
		Button bt3 = (Button) findViewById(R.id.camera_button);
		bt3.setOnTouchListener(mDelayHideTouchListener);

		bt3.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				isGone = true;
	        	startActivity(new Intent(getApplicationContext(), CameraPreview.class));
				Log.v(LOGTAG, "check");
		        
			}
		});
		
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				if (!isGone)
	            	nextAction();   
			}
		}, SPLASH_SCREEN_WAIT);
		
	}
	
	public void checkUserExistsBooleanCb(String url, JSONObject json, AjaxStatus status) throws JSONException{
		Log.v(LOGTAG, "checkUserExistsBooleanCb");
		if(json!=null){
			// TODO: got proper response
			boolean userExists = json.getBoolean("result");
			Log.i(LOGTAG, "checkUserExistsBooleanCb user exists: "+ userExists);
			if (!userExists) {
				AppUtility.purgeProfile(this);
			}
		} else {
			// user not found, purge profile
			Log.i(LOGTAG, "checkUserExistsBooleanCb bad response");
			
        }
		aq.id(R.id.progressMarker).visibility(View.INVISIBLE);
		bt.setEnabled(true);
		loginChecked = true;
	}
	
	public void checkUserExistsCb(String url, JSONObject json, AjaxStatus status){
		Log.v(LOGTAG, "checkUserExistsCb");
		if(json != null){
			// TODO: user exists, only update his username
			// ...
			Log.i(LOGTAG, "user exists");
		} else {
			// user not found, purge profile
			Log.i(LOGTAG, "user doesn't exist");
			AppUtility.purgeProfile(this);
        }
		bt.setEnabled(true);
		loginChecked = true;
	}
	
	private static boolean isGone = false;
	private void nextAction() {
		Log.d(LOGTAG, "proceed to next action");
		isGone = true;
        if (isLoggedIn()) {
        	startActivity(new Intent(getApplicationContext(), RollFlipPlayActivity.class));
        }
        else {
        	// TODO: in fact, will have to be a roll-flip but different
        	startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        }
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			mSystemUiHider.hide();
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}

	// - - - Test
	private BroadcastReceiver receiver = new BroadcastReceiver() {

	    @Override
	    public void onReceive(Context context, Intent intent) {
	      Bundle bundle = intent.getExtras();
	        Integer value = bundle.getInt("value", 0);

	      if (bundle != null) {
	    	  Log.v(LOGTAG, "BroadcastReceiver received: "+value);
	      }
	    }
	  };
	  @Override
	  protected void onResume() {
	    super.onResume();
	    registerReceiver(receiver, new IntentFilter(ProcessingService.NOTIFICATION));
	  }
	  @Override
	  protected void onPause() {
	    super.onPause();
	    unregisterReceiver(receiver);
	  }

	  // test video
	  private void ffmpegTester() {
	    // add infos for the service which file to download and where to store
//	    intent.putExtra(ProcessingService.FILENAME, "index.html");
//	    intent.putExtra(ProcessingService.URL,
//	        "http://www.vogella.com/index.html");
        Intent intent = new Intent(ProcessingService.START_ACTION);
        intent.setClass(getApplicationContext(), ProcessingService.class);
//		intent.putExtra("num", 0);
		intent.putExtra("name", "wot.txt");
		String[] commands = new String[1];
//		commands[0]="-y -i /storage/emulated/0/com.storyroll/new_fragment.mp4 -cropleft 200 -croptop 200 /storage/emulated/0/com.storyroll/cropped_video.mpg";
//		commands[0]="-y -i /storage/emulated/0/com.storyroll/new_fragment.mp4 -r 30000/1001 -b 200k -acodec copy -sameq /storage/emulated/0/com.storyroll/trans_video.mpg";
//		commands[1]=("-y -i /storage/emulated/0/com.storyroll/cropped_video.mpg /storage/emulated/0/com.storyroll/cropped_video.mp4");
		commands[0]="-version";
		intent.putExtra("commands", commands);
	    startService(intent);
	    Log.v(LOGTAG, "Service started");
	    
//		ProcessingService ps = new ProcessingService();
////		ps.go("-y -i /storage/emulated/0/com.storyroll/new_fragment.mp4 -s 320x320 /storage/emulated/0/com.storyroll/cropped_video.mp4");
////		ps.go("-y -i /storage/emulated/0/com.storyroll/new_fragment.mp4 -filter:v \"crop=320,320,0,0\" /storage/emulated/0/com.storyroll/cropped_video.mp4");
////		ps.go("-y -i /storage/emulated/0/com.storyroll/new_fragment.mp4 -filter:v \"crop=320:320:0:0\" /storage/emulated/0/com.storyroll/cropped_video.mp4");
//		if (0==step++) {
//			ps.go("-y -i /storage/emulated/0/com.storyroll/new_fragment.mp4 -cropleft 200 -croptop 200 /storage/emulated/0/com.storyroll/cropped_video.mpg");
//		}
//		else {
//			ps.go("-y -i /storage/emulated/0/com.storyroll/cropped_video.mpg /storage/emulated/0/com.storyroll/cropped_video.mp4");
//		}
//		ps.go("-y -i /storage/emulated/0/com.storyroll/new_fragment.mp4 -vfilters \"rotate=90\" /storage/emulated/0/com.storyroll/cropped_video.mpg");
		
//		MediaDesc min = new MediaDesc();
//		min.path = getNewFragmentFilePath();
//		min.mimeType = "video/mp4";
//		
//		MediaDesc mout = new MediaDesc();
//		mout.path = AppUtility.getAppWorkingDir()+"/cropped_video.mp4";
//		mout.width = 320;
//		mout.height = 320;
//		mout.mimeType = "video/mp4";
//		
//		android.os.Handler handler = getWindow().getDecorView().getHandler(); 
//		MediaCropper mr = new MediaCropper(this, handler, min, mout, new File(AppUtility.getAppWorkingDir()), this);
//		mr.run();
	}
	
	private String getNewFragmentFilePath() {
		return AppUtility.getAppWorkingDir() + File.separator+"new_fragment.mp4";

	}
	@Override
	public void processComplete(int arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void shellOut(String arg0) {
		// TODO Auto-generated method stub
		Log.v(LOGTAG, "shellOut: "+arg0);
	}
	
}
