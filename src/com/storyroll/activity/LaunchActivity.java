package com.storyroll.activity;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.androidquery.auth.FacebookHandle;
import com.androidquery.callback.AjaxStatus;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
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
public class LaunchActivity extends BaseActivity {
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

//	protected static final long SPLASH_SCREEN_WAIT = 3000;
	protected static final long SPLASH_SCREEN_WAIT = 500;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;
	
	private final String LOGTAG = "LAUNCH";
	private final static String SCREEN_NAME = "Launch";

	
	private FacebookHandle handle;
	private Thread timerThread;
	private boolean loginChecked = false;
	private Button bt;
	private static boolean activityClosed = false; 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(LOGTAG, "onCreate");
		super.onCreate(savedInstanceState);
		
		// Fields set on a tracker persist for all hits, until they are
	    // overridden or cleared by assignment to null.
	    getGTracker().set(Fields.SCREEN_NAME, SCREEN_NAME);
	    
		// an exit?
		if (getIntent().getBooleanExtra("EXIT", false)) {
			 finish();
			 return;
		}
		
		// update loggedIn flag - in case user was deleted
		if (isLoggedIn()) {
			String apiUrl = AppUtility.API_URL + "hasUser?uuid="+getUuid();
			aq.progress(R.id.progressMarker).ajax(apiUrl, JSONObject.class, this, "hasUserCb");
		}
		else {
			nextAction();
		}

		setContentView(R.layout.activity_launcher);
		
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
				fireGAnalyticsEvent("ui_action", "touch", "launch_screen", null);
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
				fireGAnalyticsEvent("ui_action", "touch", "proceed_button", null);
				nextAction();        
			}
		});

		
//		Button bt3 = (Button) findViewById(R.id.camera_button);
//		bt3.setOnTouchListener(mDelayHideTouchListener);
//
//		bt3.setOnClickListener(new Button.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				isGone = true;
//	        	startActivity(new Intent(getApplicationContext(), CameraPreview.class));		        
//			}
//		});
		
		Button bt4 = (Button) findViewById(R.id.gcm_button);
		bt4.setOnTouchListener(mDelayHideTouchListener);

		bt4.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
	        	startActivity(new Intent(LaunchActivity.this, GcmActivity.class));
			}
		});
		
//		if (SPLASH_SCREEN_WAIT>0) {
//			Handler handler = new Handler();
//			handler.postDelayed(new Runnable() {
//				
//				@Override
//				public void run() {
//					if (!isGone)
//		            	nextAction();   
//				}
//			}, SPLASH_SCREEN_WAIT);
//		}
//		else {
//			nextAction();
//		}
		
	}

	  
	// - - - callbacks
	
	public void hasUserCb(String url, JSONObject json, AjaxStatus status) throws JSONException{
		Log.v(LOGTAG, "hasUserCb");
		if (isAjaxErrorThenReport(status)) return;
		
		if(json!=null){
			// TODO: got proper response
			boolean userExists = json.getBoolean("result");
			Log.i(LOGTAG, "hasUserCb user exists: "+ userExists);
			if (!userExists) {
				fireGAnalyticsEvent("launch", "hasUserCb", userExists+"", null);
				AppUtility.purgeProfile(this);
			}
			nextAction();
		} else {
			// user not found, purge profile
			apiError(LOGTAG, "hasUserCb bad (json null) response", status, false, Log.ERROR);			
        }
		aq.id(R.id.progressMarker).visibility(View.INVISIBLE);
		bt.setEnabled(true);
		loginChecked = true;
	}
	
//	public void checkUserExistsCb(String url, JSONObject json, AjaxStatus status){
//		Log.v(LOGTAG, "checkUserExistsCb");
//		if(json != null){
//			// TODO: user exists, only update his username
//			// ...
//			Log.i(LOGTAG, "user exists");
//		} else {
//			// user not found, purge profile
//			Log.i(LOGTAG, "user doesn't exist");
//			AppUtility.purgeProfile(this);
//        }
//		bt.setEnabled(true);
//		loginChecked = true;
//	}
	
	private static boolean isGone = false;
	private void nextAction() {
		Log.d(LOGTAG, "proceed to next action");
		isGone = true;
        if (isLoggedIn()) {
//        	startActivity(new Intent(getApplicationContext(), RollFlipPlayActivity.class));
        	startActivity(new Intent(getApplicationContext(), TabbedPlaylistActivity.class));

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
	
}
