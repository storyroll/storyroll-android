package com.storyroll.activity;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import ru.jecklandin.stickman.vp.ProcessingService;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnInfoListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.androidquery.callback.AjaxStatus;
import com.storyroll.R;
import com.storyroll.base.BaseActivity;
import com.storyroll.tasks.FfmpegTask;
import com.storyroll.tasks.FfmpegTask.OnFfmpegTaskCompleted;
import com.storyroll.tasks.VideoDownloadTask;
import com.storyroll.tasks.VideoDownloadTask.OnVideoTaskCompleted;
import com.storyroll.util.AppUtility;
import com.storyroll.util.CameraUtility;

public class VideoCaptureActivity extends BaseActivity implements
		SurfaceHolder.Callback, Button.OnClickListener, OnVideoTaskCompleted,  OnInfoListener {

	public static final String LOGTAG = "VIDEOCAPTURE";
	
	//fires once a half/second
	private static final int PROG_REFRESH = 500; // progress refresh rate
//	private static final int SAVE_REQ = 1000;
	private boolean ffmpegConvert = false; // ffmpeg convert?

	private SurfaceView surfaceView;
	
	private VideoView videoView;

	SurfaceHolder previewHolder = null;
	Button rotateButton;
	Button backButton;
	View redButton, redButtonCircle;
	TextView redButtonText, videocapReadyMessage, startStoryMessage;
	ImageView counterOverlay;
	ProgressBar progress, customProgress;

	MediaRecorder recorder;
	private Camera camera;

	JSONObject lastFragment = null;
	private String lastFragmentPath = null;
	private boolean playsEarlierFragment;
	
	private static final int STATE_NO_STORY = -1;
	private static final int STATE_INITIAL = 0;
	private static final int STATE_PREV_LAST = 1;
	private static final int STATE_PREV_CAM = 2;
	private static final int STATE_REC = 3;
	private static final int STATE_PREV_NEW = 4;
	private static final int STATE_UPLOAD = 5;
		
	private static final boolean BUTTON_RED=false;
	private static final boolean BUTTON_YELLOW=true;

	private int lastState = STATE_NO_STORY;
	private Integer storyId;
	private boolean isStartNew = false;
	private int cameraOrientation;
	private Camera.Size bestPreviewSize;
	private Camera.Size bestRecordSize;

	private Integer currentCameraId = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_videocapture);

//		getWindow().setFormat(PixelFormat.TRANSLUCENT);
//        getWindow().setFlags(
//				WindowManager.LayoutParams.FLAG_FULLSCREEN,
//				WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getActionBar().hide();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		surfaceView = (SurfaceView) findViewById(R.id.CameraView);

		videoView = (VideoView) findViewById(R.id.videoPlayerView);
		videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				Log.i(LOGTAG, "Duration = " + videoView.getDuration());
				mp.setLooping(true);
			}
		});
		counterOverlay = (ImageView)findViewById(R.id.counterOverlay);
		progress = (ProgressBar) findViewById(R.id.progress);
		customProgress = (ProgressBar) findViewById(R.id.customProgressBar);
		

//		CameraUtility.viewToSquare(videoView, VideoCaptureActivity.this);
//		CameraUtility.viewToSquare(surfaceView, VideoCaptureActivity.this);
		CameraUtility.viewToSquare(counterOverlay, VideoCaptureActivity.this);

    	
		if (previewHolder == null) {
			previewHolder = surfaceView.getHolder();
		}

		previewHolder.addCallback(this);

		redButton = findViewById(R.id.redButton);
		redButton.setOnClickListener(this);
		redButtonText = (TextView)findViewById(R.id.redButtonText);
		redButtonCircle = findViewById(R.id.redButtonCircle);
		videocapReadyMessage = (TextView)findViewById(R.id.videocapReadyMessage);
		startStoryMessage = (TextView)findViewById(R.id.startStoryMessage);
		
		backButton = aq.id(R.id.backButton).getButton();
		backButton.setOnClickListener(new BackClickListener());
		
		rotateButton = (Button)findViewById(R.id.rotateButton);

		if(Camera.getNumberOfCameras() > 1){
			rotateButton.setOnClickListener(new SwitchCameraClickListener());
		}

		// get story to join
		aq.ajax(AppUtility.API_URL+"getStoryToJoin?uuid="+getUuid(), JSONObject.class, this, "getStoryToJoinCb");
		
	}
	
	private String getNewFragmentTempFilePath() {
		return AppUtility.getAppWorkingDir() + File.separator+"cropped_fragment.mpg";
	}
	private String getNewFragmentCroppedFilePath() {
		return AppUtility.getAppWorkingDir() + File.separator+"cropped_fragment.mp4";
	}
	// - - - callbacks
	
	public void getStoryToJoinCb(String url, JSONObject json, AjaxStatus status){
        
        if(json != null){               
            //successful ajax call
        	Log.i(LOGTAG, "getStoryToJoinCb success: "+json.toString());
        	try {
				JSONObject story = (JSONObject)json.get("story");
				getStoryDataAndJoin(story, json);
								
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }else{          
            //ajax error - means no story to join, offer to start new one
        	Log.w(LOGTAG, "getStoryToJoinCb: null Json");
        	lastState = processAndSetNewState(STATE_NO_STORY);
        }
        
	}
	
	private void getStoryDataAndJoin(JSONObject story, JSONObject parentJson){
		try {
			storyId = story.getInt("id");
			if ( parentJson!=null && parentJson.has("lastFragment") && !parentJson.isNull("lastFragment") ) 
			{
				lastFragment = parentJson.getJSONObject("lastFragment");
				Log.i(LOGTAG, "getStoryToJoinCb: storyId="+storyId+", lastFragment="+lastFragment.toString());
			}
			else {
				lastFragment = null;
				Log.v(LOGTAG, "getStoryToJoinCb: lastFragment is null");
			}
			
			// join (lock) the story
			aq.ajax(AppUtility.API_URL+"joinStory?uuid="+getUuid()+"&story="+storyId, JSONObject.class, this, "joinStoryCb");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void onVideoTaskCompleted(String cachedFileName) {
		// start playing last fragment and enable control button
		lastFragmentPath = AppUtility.getVideoCacheDir(getApplicationContext())+"/"+cachedFileName;
		Log.d(LOGTAG, "onVideoTaskCompleted: "+lastFragmentPath);
		
		// below will do all of the above
		lastState = processAndSetNewState(STATE_PREV_LAST);
		
	}
	
	public void joinStoryCb(String url, JSONObject json, AjaxStatus status){
        
        if(json != null){               
            //successful ajax call
        	Log.i(LOGTAG, "joinStoryCb success: "+json.toString());
        	// load last fragment
        	if (lastFragment!=null) {
		   		try {
		   			// start a story fragment preload task
					String fragmentApiUrl = AppUtility.API_URL+"fragmentFile?fragment="+lastFragment.getInt("id");
			   		VideoDownloadTask task = new VideoDownloadTask(getApplicationContext(), this);
			   		task.execute(fragmentApiUrl);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        	else {
        		// no last fragment preview
        		if (isStartNew) {
        			// this is after start new? start camera preview immediately
        			lastState = processAndSetNewState(STATE_PREV_CAM);
        		}
        		else {
        			// this is regular join, show start message first
	        		lastState = processAndSetNewState(STATE_INITIAL);
        		}
        	}
        }else{          
            //ajax error
        	Log.e(LOGTAG, "joinStoryCb: null Json, story was not joined");
        	// TODO: invalidate story join
        	// needs decision: a) retry joining the same story b) auto choose next story 
        }
        
	}
	
	public void startStoryCb(String url, JSONObject json, AjaxStatus status) {
        
        if(json != null){               
            //successful ajax call
        	Log.i(LOGTAG, "startStoryCb success: "+json.toString());
			// now join that story that was just created (possible race condition)
        	isStartNew = true;
			getStoryDataAndJoin(json, null);
        }
        else{          
            //ajax error
        	Log.e(LOGTAG, "startStoryCb: null Json, story was not started");
			Toast.makeText(aq.getContext(), "Could not start story", Toast.LENGTH_SHORT).show();

        }
       
	}
	
	public void videoUploadCb(String url, JSONObject json, AjaxStatus status){
        isUploading = false;
    	progress.setVisibility(View.INVISIBLE);
    	
    	String fileName = getNewFragmentCroppedFilePath();
    	if (!ffmpegConvert)
    		fileName = CameraUtility.getNewFragmentFilePath();

        if(json != null){               
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
        }else{          
            //ajax error
        	Log.w(LOGTAG, "Json null");
			Toast.makeText(aq.getContext(), "Could not upload the fragment, try again.", Toast.LENGTH_SHORT).show();
			// restore state
			lastState = STATE_PREV_NEW;
        }
	}

	// this only activates necessary elements for new state, without taking into account the last state
	public int processAndSetNewState(int newState) {
		Log.i(LOGTAG, "processAndSetNewState: "+newState);
		switch (newState) {
		case STATE_NO_STORY:
			// this will allow user to start new story
			startStoryMessage.setVisibility(View.VISIBLE);
			videoView.setVisibility(View.INVISIBLE);
			break;
			
		case STATE_INITIAL:
			// we suppose a user already has a story to join, even if it's a new one and there is no preview
			startStoryMessage.setVisibility(View.VISIBLE);
			videoView.setVisibility(View.INVISIBLE);
    		redButton.setEnabled(true);
			break;
		case STATE_PREV_LAST:
			redButton.setEnabled(true);
//			surfaceView.setVisibility(View.VISIBLE);
			videocapReadyMessage.setVisibility(View.VISIBLE);
			redButtonText.setText(R.string.ready);

			// start previewing last fragment
			if (lastFragmentPath!=null) {
				videoView.setVisibility(View.VISIBLE);
				videoView.setVideoPath(lastFragmentPath);
				videoView.start();
				return STATE_PREV_LAST;
			}
			else {
				startStoryMessage.setVisibility(View.VISIBLE);
				return STATE_INITIAL;
			}			
		case STATE_PREV_CAM:
			// hide possibly previously shown elements
			startStoryMessage.setVisibility(View.INVISIBLE);
			videocapReadyMessage.setVisibility(View.INVISIBLE);
			videoView.setVisibility(View.INVISIBLE);
			
			surfaceView.setVisibility(View.VISIBLE);
			
			//if phone has only one camera, don't show "switch camera" button
			if(Camera.getNumberOfCameras() > 1){
				rotateButton.setVisibility(View.VISIBLE);
			}
			
			setRedButtonState(BUTTON_RED);
			redButtonText.setText(R.string.record);
			
			// only show back button if not starting a new story
			if (lastFragmentPath!=null){
				backButton.setVisibility(View.VISIBLE);
			}
			else {
				backButton.setVisibility(View.INVISIBLE);
			}
			break;
		case STATE_REC:
			backButton.setVisibility(View.INVISIBLE);
			redButton.setVisibility(View.INVISIBLE);
			rotateButton.setVisibility(View.INVISIBLE);
			customProgress.setProgress(0);
			customProgress.setVisibility(View.VISIBLE);
			
			// countdown
			countdown();
			
//			prepareRecorder();
//			recorder.start();
//			Log.v(LOGTAG, "Recording Started");
//			redButtonText.setText(R.string.go);
//			backButton.setVisibility(View.INVISIBLE);
			break;
		case STATE_PREV_NEW:
			if (lastFragmentPath==null) {
				// switch new fragment preview on
				videoView.setVideoPath(CameraUtility.getNewFragmentFilePath());
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
							videoView.setVideoPath(CameraUtility.getNewFragmentFilePath());
						else
							videoView.setVideoPath(lastFragmentPath);
						playsEarlierFragment = !playsEarlierFragment;
					    videoView.start();
					}
					});

			}
			
			videoView.setVisibility(View.VISIBLE);
			videoView.start();
			backButton.setVisibility(View.VISIBLE);
			redButton.setVisibility(View.VISIBLE);
			redButtonText.setText("UPLOAD");
			
			break;
		case STATE_UPLOAD:
			// (convert and then) upload the video
			if (!ffmpegConvert){
				doUpload(CameraUtility.getNewFragmentFilePath());
			}
			else if (!isCroppingConverting && !isUploading) {
				isCroppingConverting = true;
				// first, convert
				MediaMetadataRetriever retriever = new  MediaMetadataRetriever();
				Bitmap bmp = null;      
				int h, w;
				try
				{
				    retriever.setDataSource(CameraUtility.getNewFragmentFilePath());
				    bmp = retriever.getFrameAtTime();
				    h=bmp.getHeight();
				    w=bmp.getWidth();
				    
				    int m = w>h?h:w;
				    int dw = (w-m)/2;
				    int dh = (h-m)/    2;
				    Log.i(LOGTAG, "video dimensions: "+w+" x "+h+", delta: "+dw+", "+dh);
				    String s=null;// = "-y -i /storage/emulated/0/com.storyroll/new_fragment.mp4 -cropleft 260 /storage/emulated/0/com.storyroll/cropped_video.mpg";
				    if (dh!=0) {
				    	// crop top and bottom
				    	s= "-y -i "+CameraUtility.getNewFragmentFilePath()+" -cropright "+ dh + " -cropleft "+dh+ " "+getNewFragmentTempFilePath();
				    }
				    if (dw!=0) {
				    	s= "-y -i "+CameraUtility.getNewFragmentFilePath()+" -cropbottom "+ dw + " -croptop "+dw+ " "+getNewFragmentTempFilePath();
				    }
				    Log.i(LOGTAG, "comm: "+s);
				    progress.setVisibility(View.VISIBLE);
				    if (s!=null) {
				        Intent intent = new Intent(ProcessingService.START_ACTION);
				        intent.setClass(getApplicationContext(), ProcessingService.class);
						String[] commands = new String[2];
						commands[0]=s;
						commands[1]=("-y -i "+getNewFragmentTempFilePath()+" "+getNewFragmentCroppedFilePath());
						intent.putExtra("commands", commands);
					    startService(intent);
					    Log.v(LOGTAG, "Service started");
				    }
				    
				}
				catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}

			}
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
        		counterOverlay.setVisibility(View.INVISIBLE);
        	}
        ;}
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void setRedButtonState(boolean isDefault) {
		if (isDefault) {
			redButtonCircle.setBackground(getResources().getDrawable(R.drawable.circle_flash));
    		redButtonText.setTextColor(getResources().getColor(R.color.sr_text_rec_button_flash));
		}
		else {
			redButtonCircle.setBackground(getResources().getDrawable(R.drawable.circle));
    		redButtonText.setTextColor(getResources().getColor(R.color.sr_vidcap_ctrl_text));
		}
	}
	
	private class RecordStarter implements Runnable{
        @Override
        public void run() {
        	counterOverlay.setVisibility(View.INVISIBLE);
        	
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

	private void startRecProgressTimer() {
		customProgress.setMax(CameraUtility.VIDEO_LENGTH + PROG_REFRESH);
		customProgress.setProgress(0);

		CountDownTimer mCountDownTimer = new CountDownTimer(CameraUtility.VIDEO_LENGTH
				+ PROG_REFRESH, PROG_REFRESH) {

			@Override
			public void onTick(long millisUntilFinished) {
				Log.v("Log_tag", "Tick " + millisUntilFinished);
				customProgress.setProgress(customProgress.getProgress()
						+ PROG_REFRESH);
			}

			@Override
			public void onFinish() {
				customProgress.setProgress(customProgress.getProgress()
						+ PROG_REFRESH);
			}
		};
		mCountDownTimer.start();
	}
	
	public void countdown() {
		counterOverlay.setImageDrawable( getResources().getDrawable(R.drawable.rec_3) );
		tg.startTone(ToneGenerator.TONE_PROP_BEEP);
		
		counterOverlay.setVisibility(View.VISIBLE);
		Handler handler = new Handler();
		for (int count = 2; count >= 0; count--){
	        handler.postDelayed(new Counter(count), 1000 * (3-count));
	    }
		handler.postDelayed(new RecordStarter(), 3200); 
		
	}
	
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
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();
			if (bundle != null) {
				Integer value = bundle.getInt("value", 0);
				Log.v(LOGTAG, "BroadcastReceiver: " + value);

				if (value == 0) {
					Log.v(LOGTAG, "FFMPEG commands completed");
					isCroppingConverting = false;

					doUpload(getNewFragmentCroppedFilePath());

				}
			}
		}

	};
	  
	public void doUpload(String filePath){
		isUploading = true;
		backButton.setVisibility(View.INVISIBLE);
		progress.setVisibility(View.VISIBLE);
		
		File file = new File(filePath);
		
		Map params = new HashMap();
		params.put("file", file);
		params.put("uuid", getUuid());
		params.put("story", storyId);
		
		aq.ajax(AppUtility.API_URL+"addFragment", params, JSONObject.class, VideoCaptureActivity.this, "videoUploadCb").progress(R.id.progress);
	}
	  
	boolean isCroppingConverting = false;
	boolean isUploading = false;

	
	// RED-YELLOW CONTROL button click handler
	
	@Override
	public void onClick(View v) {
		switch (lastState) {
		case STATE_NO_STORY:
			aq.ajax(AppUtility.API_URL+"startStory?uuid="+getUuid(), JSONObject.class, this, "startStoryCb").progress(progress);
			break;
		case STATE_PREV_LAST:
			// stop previewing last fragment
			videoView.stopPlayback();
			
		case STATE_INITIAL:
			// switch camera preview on
//			startStoryMessage.setVisibility(View.INVISIBLE);
//			videocapReadyMessage.setVisibility(View.INVISIBLE);
//			videoView.setVisibility(View.INVISIBLE);
			
			lastState = processAndSetNewState(STATE_PREV_CAM);
			break;
			
		case STATE_PREV_CAM:
			// restore button to initial
			setRedButtonState(BUTTON_YELLOW);
			// START RECORDING
			lastState = processAndSetNewState(STATE_REC);
			break;
			
		case STATE_REC:
			// ignore push during recording. recorder stops automatically after 3 secs
			break;
			
		case STATE_PREV_NEW:
			// Stop previewing NEW fragment
			videoView.stopPlayback();
			
			lastState = processAndSetNewState(STATE_UPLOAD);
			
			break;

		default:
			Log.e(LOGTAG, "control switch in undefined state "+lastState);
			break;
		}
	}

	// "BACK" button control
	class BackClickListener implements Button.OnClickListener {
		
		@Override
		public void onClick(View v) {
			Intent intent;
			switch (lastState) {
			case STATE_PREV_LAST:
			case STATE_INITIAL:
				// return to the last used playlist
				intent = new Intent(VideoCaptureActivity.this, AppUtility.ACTIVITY_HOME);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				
				break;
			case STATE_PREV_CAM:
				// restore button color
				setRedButtonState(BUTTON_YELLOW);
				if (lastFragmentPath!=null)
				{
					// stop camera preview and return to last fragment review
					surfaceView.setVisibility(View.INVISIBLE);
					videoView.setVisibility(View.VISIBLE);
				}
				rotateButton.setVisibility(View.INVISIBLE);
				lastState = processAndSetNewState(STATE_PREV_LAST);
				
				break;
			case STATE_REC:
				// stop recording
				recorder.stop();
				Log.v(LOGTAG, "Recording Stopped");

				releaseMediaRecorder();
				surfaceView.setVisibility(View.INVISIBLE);
				
				lastState = processAndSetNewState(STATE_PREV_CAM);

				break;
			case STATE_PREV_NEW:
				// Stop previewing NEW fragment
				videoView.stopPlayback();
				videoView.setVisibility(View.INVISIBLE);

				// return to camera preview
				lastState = processAndSetNewState(STATE_PREV_CAM);

				break;

			default:
				Log.e(LOGTAG, "back pressed while in undefined state "+lastState);
				break;
			}
		}
	}
	
	// "SWITCH CAMERA" button control
	class SwitchCameraClickListener implements Button.OnClickListener {
		
		@Override
		public void onClick(View v) {
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
			        //this step is critical or preview on new camera will no know where to render to
			        camera.setPreviewDisplay(previewHolder);
			    } catch (IOException e) {
			        e.printStackTrace();
			        camera.release();
					camera = null;
			    }
			    camera.startPreview();
			}
				
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
				// get default camera id
				int cameras = Camera.getNumberOfCameras();
				Log.v(LOGTAG, "Cameras: " + cameras);
				currentCameraId = 0;
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
					
			Log.i(LOGTAG, "Best preview sizes: " + bestPreviewSize.width + ", "
					+ bestPreviewSize.height);
			
			cp.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);

			cp.setColorEffect(Camera.Parameters.EFFECT_MONO);
			// TOOD:
//			cp.setRecordingHint(true);
			
			List<String> focusModes = cp.getSupportedFocusModes();
			if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
			  // Autofocus mode is supported
				cp.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
			}

			c.setParameters(cp);
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
			Log.e(LOGTAG, "Camera is not available (in use or does not exist), camera id "+currentCameraId);
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
		recorder.setVideoFrameRate(30);
	
		recorder.setOutputFile(CameraUtility.getNewFragmentFilePath());
		recorder.setMaxDuration(CameraUtility.VIDEO_LENGTH);
		recorder.setOnInfoListener(this);

        // Tags the video with an appropriate (90¡) angle in order to tell the phone how to display it
		// the compensation parameter here is off, in order to work with both front and back facing cameras
		int orientationHint = CameraUtility.getCameraDisplayOrientation(this, currentCameraId, false);
        recorder.setOrientationHint(orientationHint);
        
		recorder.setPreviewDisplay(previewHolder.getSurface());

		try {
			recorder.prepare();
			Log.v(LOGTAG, "Recorder prepared");

		} catch (IllegalStateException e) {
			e.printStackTrace();
			finish();
		} catch (IOException e) {
			e.printStackTrace();
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
	public void onInfo(MediaRecorder mr, int what, int extra) {

		if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
			customProgress.setVisibility(View.INVISIBLE);
			
			Log.v(LOGTAG, "OnInfoListener: Maximum Duration Reached");
			mr.stop();
			Log.v(LOGTAG, "Recording Stopped");

			// here comes "hidden" stop state processing step
			releaseMediaRecorder();
			surfaceView.setVisibility(View.INVISIBLE);
			lastState = processAndSetNewState(STATE_PREV_NEW);
		}
	}
	
}
