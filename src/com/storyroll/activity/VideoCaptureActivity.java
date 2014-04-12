package com.storyroll.activity;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnInfoListener;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.androidquery.callback.AjaxStatus;
import com.bugsense.trace.BugSenseHandler;
import com.google.analytics.tracking.android.Fields;
import com.storyroll.R;
import com.storyroll.base.SwipeVideoActivity;
import com.storyroll.tasks.VideoDownloadTask;
import com.storyroll.tasks.VideoDownloadTask.OnVideoTaskCompleted;
import com.storyroll.util.AppUtility;
import com.storyroll.util.CameraUtility;
import com.storyroll.util.DataUtility;
import com.storyroll.util.ErrorUtility;
import com.storyroll.util.ImageUtility;
import com.storyroll.util.PrefUtility;

public class VideoCaptureActivity extends SwipeVideoActivity implements
		SurfaceHolder.Callback, OnVideoTaskCompleted,  OnInfoListener {

	public static final String LOGTAG = "VIDEOCAPTURE";
	private static final String SCREEN_NAME = "VideoCapture";

	
	//fires once a half/second
	private static final int PROG_REFRESH = 500; // progress refresh rate
//	private static final int SAVE_REQ = 1000;
	
	private static final int VIDEO_BITRATE = 1300000;
	private static final int VIDEO_FRAMERATE = 30;
	
	private static int DEFAULT_CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_FRONT;

	private SurfaceView surfaceView;
	
	private VideoView videoView;

	SurfaceHolder previewHolder = null;
	Button rotateButton;
//	ImageButton btnClose, btnBack, btnOK, btnCamera;
//	View redButton, redButtonCircle;
	
	TextView videocapReadyMessage, startStoryMessage;
	ImageView counterOverlay, sliderOverlay, controlClose, controlBack;
	ProgressBar progress, customRecProgress;

	MediaRecorder recorder;
	private Camera camera;

	JSONObject lastFragment = null;
	private String lastFragmentPath = null;
	private boolean playsEarlierFragment;
	
	public static final int STATE_NO_STORY = -1;
	public static final int STATE_INITIAL = 0;
	public static final int STATE_PREV_LAST = 1;
	public static final int STATE_PREV_CAM = 2;
	public static final int STATE_REC = 3;
	public static final int STATE_PREV_NEW = 4;
	public static final int STATE_UPLOAD = 5;
	public static final int STATE_UPLOAD_FAIL = 6;

	private int lastState = STATE_NO_STORY;
	private int cameraOrientation;
	private Camera.Size bestPreviewSize;
//	private Camera.Size bestRecordSize;

	private Integer currentCameraId = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_videocapture);
		
		// Fields set on a tracker persist for all hits, until they are
	    // overridden or cleared by assignment to null.
	    getGTracker().set(Fields.SCREEN_NAME, SCREEN_NAME);

        getActionBar().hide();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		surfaceView = (SurfaceView) findViewById(R.id.cameraView);

		videoView = (VideoView) findViewById(R.id.videoPlayerView);
		videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) 
			{
				Log.i(LOGTAG, "Duration = " + videoView.getDuration());
				mp.setLooping(true);
			}
		});
//		videoView.setOnClickListener(SwipeVideoActivity.this); 
		videoView.setOnTouchListener(gestureListener);
		
		counterOverlay = (ImageView)findViewById(R.id.counterOverlay);
		sliderOverlay = (ImageView)findViewById(R.id.sliderOverlay);

		progress = (ProgressBar) findViewById(R.id.progress);
		customRecProgress = (ProgressBar) findViewById(R.id.customProgressBar);
		

//		CameraUtility.viewToSquare(videoView, VideoCaptureActivity.this);
//		CameraUtility.viewToSquare(surfaceView, VideoCaptureActivity.this);
		CameraUtility.viewToSquare(counterOverlay, VideoCaptureActivity.this);

    	
		if (previewHolder == null) {
			previewHolder = surfaceView.getHolder();
		}

		previewHolder.addCallback(this);
		
		aq.id(R.id.btnOK).clicked(this, "workflowClickedCb");
		aq.id(R.id.btnCamera).clicked(this, "workflowClickedCb");
		
		videocapReadyMessage = (TextView)findViewById(R.id.videocapReadyMessage);
		startStoryMessage = (TextView)findViewById(R.id.startStoryMessage);

		aq.id(R.id.btnClose).clicked(this, "backAndCloseClickedCb");
		
		rotateButton = aq.id(R.id.rotateButton).getButton();
		aq.id(R.id.rotateButton).clicked(this, "switchCameraClickedCb");

		// get list of available fragments
		show(progress);
		aq.ajax(PrefUtility.getApiUrl()+"available?uuid="+getUuid()+"&c="+10, JSONArray.class, this, "availableCb");
		
	}

	// - - - callbacks
	
	private int currentLastCarouselItemId = 0;
	private long currentLastFragmentId = 0;
	private long[] fragmentIds = null;
	
	public void availableCb(String url, JSONArray jarr, AjaxStatus status)
	{
//    	fireGAnalyticsEvent("story_workflow", "available", json==null?"got no story":"got stories", null);
    	
    	if (status.getCode()==AjaxStatus.NETWORK_ERROR) {
    		hide(progress);
    		ErrorUtility.apiError(LOGTAG, "Network error, check your connection", status, this, true, Log.ERROR);
		}
    	// TODO: is 500 resp ok when there is simply no story to join?
    	if (isAjaxErrorThenReport(status)) return;
    	
        if(jarr != null){               
            //successful ajax call
        	Log.i(LOGTAG, "availableCb success: got "+jarr.length()+" items");
        	fragmentIds = new long[jarr.length()];
        	for (int i = 0; i < jarr.length(); i++) {
				JSONObject fragObj;
				try {
					fragObj = jarr.getJSONObject(i);
					fragmentIds[i] = fragObj.getLong("id");
				} 
				catch (JSONException e) 
				{
					fragmentIds[i] = 0;
					e.printStackTrace();
				}
			}
			fragmentCarousel(0);
        }
        else{          
            //ajax error - means no story to join, offer to start new one
        	Log.w(LOGTAG, "availableCb: null Json");
        	lastState = processAndSetState(STATE_NO_STORY);
        }
        
	}

	private void fragmentCarousel(int i) 
	{
		currentLastFragmentId = fragmentIds[i];
		currentLastCarouselItemId = i;
		
		// load fragment
		if (currentLastFragmentId!=0) 
		{
   			// start a story fragment preload task
			String fragmentApiUrl = PrefUtility.getApiUrl()+"fragmentFile?fragment="+fragmentIds[i]+"&uuid="+getUuid();
	   		VideoDownloadTask task = new VideoDownloadTask(getApplicationContext(), this);
	   		task.execute(fragmentApiUrl);
    	}		
	}

	@Override
	public void onVideoTaskCompleted(String cachedFileName, boolean success, boolean wasCached) {
		// start playing last fragment and enable control button
		lastFragmentPath = AppUtility.getVideoCacheDir(getApplicationContext())+"/"+cachedFileName;
		Log.d(LOGTAG, "onVideoTaskCompleted: "+lastFragmentPath);
		
		if (success) {
			// below will do all of the above
			lastState = processAndSetState(STATE_PREV_LAST);
		}
		else {
			// clean up after itself, and take next action. maybe retry?
			hide(progress);
			Toast.makeText(this, "Error downloading fragment, please try later", Toast.LENGTH_SHORT).show();
		}
	}
	
	public void addFragmentCb(String url, JSONObject json, AjaxStatus status)
	{
        
    	fireGAnalyticsEvent("fragment_workflow", "addFragment", json==null?"fail":"success", null);
    	
    	if (cancelUpload) {
    		// do nothing
    		cancelUpload = false;
    		Log.v(LOGTAG, "cancelUpload is true, upload response ignored (cleanup needed?)"); // TODO cleanup
    		return;
    	}
    	else if (isAjaxErrorThenReport(status)) {
    		Toast.makeText(this, "Error uploading fragment, please try again", Toast.LENGTH_SHORT).show();
    		processAndSetState(STATE_UPLOAD_FAIL);
    		return;
    	}

		isUploading = false;
    	hide(progress);
    	
    	String fileName = CameraUtility.getNewFragmentFilePath(this);

        if(json != null)
        {               
            //successful ajax call
        	Log.i(LOGTAG, "videoUploadCb success: "+json.toString());
        	
        	// rename file
        	File newFile = new File(fileName+".last");
        	if (newFile.exists()) {
        		newFile.delete();
        	}
        	File f = new File(fileName);
        	f.renameTo(newFile);
			// go to "video sent" activity
			Intent sendActivity = new Intent(this, VideoSendActivity.class);
			sendActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(sendActivity);
        }else
        {          
            //ajax error
        	apiError(LOGTAG, "Could not upload the fragment, try again.", status, true, Log.ERROR);

        	// restore state
        	processAndSetState(STATE_PREV_NEW);
//			lastState = STATE_PREV_NEW;
        }
	}

	// this only activates necessary elements for new state, without taking into account the last state
	public int processAndSetState(int newState) {
		Log.i(LOGTAG, "processAndSetNewState: "+newState);
		switch (newState) {
		case STATE_NO_STORY:
			// this will allow user to start new story
			hide(progress);
			show(startStoryMessage);
			hide(videoView);
			showClose();
			showOk();
			break;
			
		case STATE_INITIAL:
			// we suppose a user already has a story to join, even if it's a new one and there is no preview
			hide(progress);
			show(startStoryMessage);
			hide(videoView);
			showClose();
			showOk();
			break;
		case STATE_PREV_LAST:
			hide(progress);
			showOk();
			showClose();
//			surfaceView.setVisibility(View.VISIBLE);
			show(videocapReadyMessage);
			
			ImageUtility.sliderAnimateRightToLeft(sliderOverlay);
			
			// start previewing last fragment
			if (lastFragmentPath!=null) {
				show(videoView);
				videoView.setVideoPath(lastFragmentPath);
				videoView.start();
				return STATE_PREV_LAST;
			}
			else {
				show(startStoryMessage);
				return STATE_INITIAL;
			}			
		case STATE_PREV_CAM:
			// hide possibly previously shown elements
			hide(startStoryMessage);
			hide(videocapReadyMessage);
			hide(videoView);
			show(surfaceView);
			
			//if phone has only one camera, don't show "switch camera" button
			if(Camera.getNumberOfCameras() > 1){
				show(rotateButton);
			}
			
			showCamera();
			
			// only show back button if not starting a new story
			if (lastFragmentPath!=null){
				showBack();
			}
			else {
				showClose();
			}
			break;
		case STATE_REC:
			showBack();
			aq.id(R.id.btnOK).gone();
			aq.id(R.id.btnCamera).gone();
			
			hide(rotateButton);
			customRecProgress.setProgress(0);
			show(customRecProgress);
			
//			// countdown
//			countdown();
			startImmediately();
			
//			prepareRecorder();
//			recorder.start();
			break;
		case STATE_PREV_NEW:
			gone(progress);
			if (lastFragmentPath==null) {
				// switch new fragment preview on
				videoView.setVideoPath(CameraUtility.getNewFragmentFilePath(this));
			}
			else {
				Log.v(LOGTAG, "setting up for two sequential videos to be played");
				// play last (loaded) fragment, and then a new one on completion
				videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
					@Override
					public void onPrepared(MediaPlayer mp) {
		            	// Adjust the size of the video
		                // to fit width of the screen
//		                int videoWidth = mp.getVideoWidth();
//		                int videoHeight = mp.getVideoHeight();
//		                float videoProportion = (float) videoWidth / (float) videoHeight;       
//		                int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
//		                int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
//		                android.view.ViewGroup.LayoutParams lp = videoView.getLayoutParams();
//		                lp.width = screenWidth;
//		                lp.height = (int) ((float) screenWidth / videoProportion);
//		                videoView.setLayoutParams(lp);
						
						CameraUtility.viewToSquare(videoView, VideoCaptureActivity.this);
						CameraUtility.viewToSquare(surfaceView, VideoCaptureActivity.this);
		            	
						mp.setLooping(false);
					}
				});
				videoView.setVideoPath(lastFragmentPath);
				playsEarlierFragment = true;
				videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer mp) {
						if (playsEarlierFragment)
							videoView.setVideoPath(CameraUtility.getNewFragmentFilePath(VideoCaptureActivity.this));
						else
							videoView.setVideoPath(lastFragmentPath);
						playsEarlierFragment = !playsEarlierFragment;
					    videoView.start();
					}
					});

			}
			
			show(videoView);
			videoView.start();
			showBack();
			showOk();
			
			break;
		case STATE_UPLOAD:
			// upload the video
			doUpload(CameraUtility.getNewFragmentFilePath(this));
			break;
		case STATE_UPLOAD_FAIL:
			hide(progress);
			isUploading = false;
			showBack();
			break;
		default:
			Log.w(LOGTAG, "change state not implemented: "+newState);
			break;
		}
		return newState;
	}
	

	final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
	
	private class Counter implements Runnable{
		int c;
		Drawable d;
		public Counter(int c) {
			int dId = R.drawable.rec_2;
			if (c==1) dId = R.drawable.rec_1;
			if (c==0) dId = R.drawable.rec_0;
			this.c = c;
			d = getResources().getDrawable(dId);
		}
        @Override
        public void run() {
        	if (c>=0) {
        		counterOverlay.setImageDrawable(d);
				// beep
				try {
			        tg.startTone(ToneGenerator.TONE_PROP_BEEP);
			    } catch (Exception e) {}
        	}
        	else {
        		hide(counterOverlay);
        	}
        ;}
	}
	
	private class RecordStarter implements Runnable{
        @Override
        public void run() {
        	hide(counterOverlay);
        	
//			try {
				prepareRecorder();
				recorder.start();
				Log.v(LOGTAG, "Recording Started");
//			} catch (IllegalStateException e) {
//				e.printStackTrace();
//				finish();
//			} catch (IOException e) {
//				e.printStackTrace();
//				finish();
//			}
			
        	startRecProgressTimer();

        ;}
	}

//	private void prepareRecorder() throws IllegalStateException, IOException {
//		recorder = CameraUtility.prepareRecorder(camera, currentCameraId, previewHolder, this);
//	}

	private void startRecProgressTimer() 
	{
		customRecProgress.setMax(CameraUtility.VIDEO_LENGTH + PROG_REFRESH);
		customRecProgress.setProgress(0);

		CountDownTimer mCountDownTimer = new CountDownTimer(CameraUtility.VIDEO_LENGTH
				+ PROG_REFRESH, PROG_REFRESH) {

			@Override
			public void onTick(long millisUntilFinished) {
				Log.v("Log_tag", "Tick " + millisUntilFinished);
				customRecProgress.setProgress(customRecProgress.getProgress()
						+ PROG_REFRESH);
			}

			@Override
			public void onFinish() {
				customRecProgress.setProgress(customRecProgress.getProgress()
						+ PROG_REFRESH);
			}
		};
		mCountDownTimer.start();
	}
	
	public void countdown() 
	{
		counterOverlay.setImageDrawable( getResources().getDrawable(R.drawable.rec_3) );
		tg.startTone(ToneGenerator.TONE_PROP_BEEP);
		
		show(counterOverlay);
		Handler handler = new Handler();
		for (int count = 2; count >= 0; count--){
	        handler.postDelayed(new Counter(count), 1000 * (3-count));
	    }
		handler.postDelayed(new RecordStarter(), 3100); 
	}
	
	public void startImmediately() {
//		counterOverlay.setImageDrawable( getResources().getDrawable(R.drawable.rec_0) );
		tg.startTone(ToneGenerator.TONE_PROP_BEEP);
		
		show(counterOverlay);
		Handler handler = new Handler();
		handler.postDelayed(new RecordStarter(), 0); 
	}
	
	  @Override
	  protected void onResume() {
	    super.onResume();
	  }
	  @Override
	  protected void onPause() {
	    super.onPause();
	  }
	
	  
	public void doUpload(String filePath){
		isUploading = true;
		cancelUpload = false;
		showClose();
		show(progress);
		
		File file = new File(filePath);
		
		Map params = new HashMap();
		params.put("file", file);
		params.put("uuid", getUuid());
		params.put("l", currentLastFragmentId);
		
		aq.ajax(PrefUtility.getApiUrl()+"addFragment", params, JSONObject.class, VideoCaptureActivity.this, "addFragmentCb").progress(R.id.progress);
	}
	  
	private boolean isUploading = false;
	private boolean cancelUpload = false;

	
	// WORKFLOW CONTROL button click handler
	public void workflowClickedCb() 
	{
		fireGAnalyticsEvent("ui_action", "controll_button_from_state", DataUtility.stateStr(lastState), null);
		
		switch (lastState) {
		case STATE_NO_STORY:
			// not creating new stories any more
//			aq.ajax(PrefUtility.getApiUrl()+"startStory?uuid="+getUuid(), JSONObject.class, this, "startStoryCb").progress(progress);
			Log.e(LOGTAG, "workflowClickedCb "+STATE_NO_STORY);
			break;
		case STATE_PREV_LAST:
			// stop previewing last fragment
			videoView.stopPlayback();
			
		case STATE_INITIAL:
			// switch camera preview on
			lastState = processAndSetState(STATE_PREV_CAM);
			break;
			
		case STATE_PREV_CAM:
			// START RECORDING
			lastState = processAndSetState(STATE_REC);
			break;
			
		case STATE_REC:
			// ignore push during recording. recorder stops automatically after 3 secs
			break;
			
		case STATE_PREV_NEW:
			// Stop previewing NEW fragment
			videoView.stopPlayback();
			lastState = processAndSetState(STATE_UPLOAD);
			break;

		default:
			BugSenseHandler.sendException(new RuntimeException("Undefined state for Controll "+lastState));
			Log.e(LOGTAG, "control switch in undefined state "+lastState);
			break;
			}
	}

	// "BACK" button control
	public void backAndCloseClickedCb() 
	{
		fireGAnalyticsEvent("ui_action", "back_button_from_state", DataUtility.stateStr(lastState), null);

		Intent intent;
		switch (lastState) {
		case STATE_NO_STORY:
		case STATE_PREV_LAST:
		case STATE_INITIAL:
			// return to the last used playlist
			intent = new Intent(VideoCaptureActivity.this, AppUtility.ACTIVITY_HOME);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			
			break;
		case STATE_PREV_CAM:
			// restore button color
			if (lastFragmentPath!=null)
			{
				// stop camera preview and return to last fragment review
				hide(surfaceView);
				show(videoView);
			}
			hide(rotateButton);
			lastState = processAndSetState(STATE_PREV_LAST);
			
			break;
		case STATE_REC:
			// stop recording
			recorder.stop();
			Log.v(LOGTAG, "Recording Stopped");

			releaseMediaRecorder();
			hide(surfaceView);
			
			lastState = processAndSetState(STATE_PREV_CAM);

			break;
		case STATE_PREV_NEW:
			// Stop previewing NEW fragment
			videoView.stopPlayback();
			hide(videoView);
			
			// return to camera preview
			lastState = processAndSetState(STATE_PREV_CAM);

			break;
		case STATE_UPLOAD:
			cancelUpload  = true;
			
			lastState = processAndSetState(STATE_PREV_NEW);
			break;
		default:
			Log.e(LOGTAG, "back pressed while in undefined state "+lastState);
			BugSenseHandler.sendException(new RuntimeException("Undefined state for Back "+lastState));
			break;
		}
	}
	
	// "SWITCH CAMERA" button control
	public void switchCameraClickedCb() {

		fireGAnalyticsEvent("ui_action", "touch", "switchCamera", null);

		if (lastState==STATE_PREV_CAM) {
			Log.d(LOGTAG, "change camera");
			// todo: rotate
			if (lastState == STATE_PREV_CAM) {
		        camera.stopPreview();
		    }
			//NB: if you don't release the current camera before switching, you app will crash
		    camera.release();
		    //swap the id of the camera to be used
		    currentCameraId++;
		    if (currentCameraId >= Camera.getNumberOfCameras())
		    	currentCameraId = 0;
//			    if(currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK){
//			        currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
//			    }
//			    else {
//			        currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
//			    }
		    camera = getCameraInstance();
		    try {
		        //this step is critical or preview on new camera will not know where to render to
		        camera.setPreviewDisplay(previewHolder);
		    } catch (IOException e) {
		        e.printStackTrace();
		        camera.release();
				camera = null;
		    }
		    camera.startPreview();
		}
			
	}
	
	// SurfaceHolder.Callback

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		camera.stopPreview();
		camera.release();
		camera = null;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		camera = getCameraInstance();

		try {
			camera.setPreviewDisplay(holder);
		} catch (IOException e) {
			e.printStackTrace();
			camera.release();
			camera = null;
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		camera.startPreview();
	}

	// Camera, Recorder

	private Camera getCameraInstance() {
		Log.i(LOGTAG, "getCameraInstance");
		Camera c = null;
		try {
			if (currentCameraId==null) {
				// get default camera, if it exists, otherwise use first available
				int cameras = Camera.getNumberOfCameras();
				Log.v(LOGTAG, "Cameras: " + cameras);
				currentCameraId = 0;
				for (int i=0;i<cameras;i++) {
					if (i==DEFAULT_CAMERA_ID) {
						currentCameraId = i;
					}
				}
			}
			
			c = Camera.open(currentCameraId); // attempt to get a Camera instance
			
			// not much difference when is this being set
			cameraOrientation = CameraUtility.getCameraDisplayOrientation(this, currentCameraId, true);
			c.setDisplayOrientation(cameraOrientation);
			
			Log.d(LOGTAG, "cameraOrientation "+cameraOrientation);
			
			Camera.Parameters cp = c.getParameters();

			cp.setRotation(cameraOrientation);
			cp.set("orientation", "portrait");
//			cp.set("rotation", "90");
			cp.set("rotation", cameraOrientation+"");


//			bestPreviewSize = CameraUtility.getOptimalPreviewSize(640, 640, c);
			bestPreviewSize = CameraUtility.getOptimalRecordingSize(480, 480, c.getParameters().getSupportedPreviewSizes());
					
			Log.i(LOGTAG, "Best preview sizes: " + bestPreviewSize.width + " x "
					+ bestPreviewSize.height);
			fireGAnalyticsEvent("camera", "bestPreviewSize", bestPreviewSize.width + " x "+ bestPreviewSize.height, null);

			
			cp.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);

			List<String> colorEffects = cp.getSupportedColorEffects();
//			if (colorEffects.contains(Camera.Parameters.EFFECT_MONO)) {
//				// Mono effect is supported
//				cp.setColorEffect(Camera.Parameters.EFFECT_MONO);
//			}
			// TODO: get stats about mono effect support?
			
			// TODO)
			cp.setRecordingHint(true);
			
			List<String> focusModes = cp.getSupportedFocusModes();
			if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
			  // Autofocus mode is supported
				cp.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
			}
			fireGAnalyticsEvent("camera", "autoFocusMode", focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)?"supported":"not supported", null);


			c.setParameters(cp);
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
			Log.e(LOGTAG, "Camera is not available (in use or does not exist), camera id "+currentCameraId);
			BugSenseHandler.sendException(new Exception("Camera not available "+currentCameraId));
		}
		return c; // returns null if camera is unavailable
	}


	private void prepareRecorder() {
		camera.unlock();

		recorder = new MediaRecorder();
		recorder.setCamera(camera);

		recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

		// recorder.setProfile(camcorderProfile);
//		recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
		
		// best is to setvideosize after setoutput format but before
		// setvideoencoder
		recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		// AAC_ELD - not supported by Samsung? Was hanging the recorder

//		recorder.setVideoSize(bestSize.width, bestSize.height);
//		recorder.setVideoSize(1280, 720);
		recorder.setVideoSize(bestPreviewSize.width, bestPreviewSize.height);
		Log.d(LOGTAG, "recorder video size set to "+bestPreviewSize.width+" x "+bestPreviewSize.height);

//		recorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
//		recorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP); // not working for samsung?
		recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264); 

		// Sets the frame rate of the video to be captured. Must be called after 
		// setVideoSource(). Call this after setOutFormat() but before prepare().
		recorder.setVideoFrameRate(VIDEO_FRAMERATE);
		recorder.setVideoEncodingBitRate(VIDEO_BITRATE);
	
		recorder.setOutputFile(CameraUtility.getNewFragmentFilePath(this));
		recorder.setMaxDuration(CameraUtility.VIDEO_LENGTH);
		recorder.setOnInfoListener(this);

        // Tags the video with an appropriate (90) angle in order to tell the phone how to display it
		// the compensation parameter here is off, in order to work with both front and back facing cameras
		int orientationHint = CameraUtility.getCameraDisplayOrientation(this, currentCameraId, false);
        recorder.setOrientationHint(orientationHint);
        
		recorder.setPreviewDisplay(previewHolder.getSurface());

		try {
			recorder.prepare();
			Log.v(LOGTAG, "Recorder prepared");

		} catch (IllegalStateException e) {
			Log.e(LOGTAG, "State Error preparing MediaRecorder", e);
			BugSenseHandler.sendException(e);
			finish();
		} catch (IOException e) {
			Log.e(LOGTAG, "I/O Error preparing MediaRecorder", e);
			BugSenseHandler.sendException(e);
			finish();
		}
	}

	private void releaseMediaRecorder() {
		if (recorder != null) {
			recorder.reset(); // clear recorder configuration
			recorder.release(); // release the recorder object
			recorder = null;
		}
	}
	
	@Override
	public void onInfo(MediaRecorder mr, int what, int extra) 
	{
		if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) 
		{
			hide(customRecProgress);
			
			Log.v(LOGTAG, "OnInfoListener: Maximum Duration Reached");
			mr.stop();
			Log.v(LOGTAG, "Recording Stopped");

			// here comes "hidden" stop state processing step
			releaseMediaRecorder();
			hide(surfaceView);
			lastState = processAndSetState(STATE_PREV_NEW);
		}
	}

	@Override
	protected void leftSwipe() {
		if (lastState!=STATE_PREV_LAST) return;
		// next fragment
		fragmentCarousel(++currentLastCarouselItemId<fragmentIds.length?currentLastCarouselItemId:0);
	}

	@Override
	protected void rightSwipe() {
		if (lastState!=STATE_PREV_LAST) return;
		show(progress);
		// previous fragment
		fragmentCarousel(--currentLastCarouselItemId>0?currentLastCarouselItemId:fragmentIds.length-1);
	}
	
	private void hide(View v) {
		v.setVisibility(View.GONE);
	}
	private void show(View v) {
		v.setVisibility(View.VISIBLE);
	}
	private void gone(View v){
		v.setVisibility(View.GONE);
	}
	private void showBack() {
		aq.id(R.id.btnBack).visible(); aq.id(R.id.btnClose).gone();
	}
	private void showClose() {
		aq.id(R.id.btnClose).visible(); aq.id(R.id.btnBack).gone();
	}
	private void showOk() {
		aq.id(R.id.btnOK).visible(); aq.id(R.id.btnCamera).gone();		
	}
	private void showCamera() {
		aq.id(R.id.btnCamera).visible(); aq.id(R.id.btnOK).gone();	
	}
}