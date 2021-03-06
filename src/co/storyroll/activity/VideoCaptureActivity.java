package co.storyroll.activity;

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
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.*;
import co.storyroll.R;
import co.storyroll.base.BaseActivity;
import co.storyroll.enums.AutofocusMode;
import co.storyroll.tasks.VideoDownloadTask;
import co.storyroll.tasks.VideoDownloadTask.OnVideoTaskCompleted;
import co.storyroll.util.*;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.bugsense.trace.BugSenseHandler;
import com.google.analytics.tracking.android.Fields;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;

public class VideoCaptureActivity extends BaseActivity implements
		SurfaceHolder.Callback, OnVideoTaskCompleted,  OnInfoListener {

	public static final String LOGTAG = "VIDEOCAPTURE";
    private static final String SCREEN_NAME = "VideoCapture";

	public static final String LAST_USER_UUID = "LAST_USER";
    public static final String LAST_USER_AVATAR = "LAST_USER_AVATAR";

    private static final String MODE_NEW = "MODE_NEW";
	public static final String CURRENT_CHANNEL = "CURRENT_CHANNEL";
	public static final String RESPOND_TO_CLIP = "RESPOND_TO_CLIP";
    public static final String RESPOND_TO_CLIP_URL = "RESPOND_TO_CLIP_URL";
    public static final String MOVIE = "MOVIE";
	private static final String CURRENT_CAMERA = "CURRENT_CAMERA";
	private static final String LAST_STATE = "LAST_STATE";
    private static final String LAST_FRAGMENT_PATH = "LAST_FRAGMENT_PATH" ;


	
	//fires once a half/second
	private static final int PROG_REFRESH = 500; // progress refresh rate
//	private static final int SAVE_REQ = 1000;
	
	private static final int VIDEO_BITRATE = 1300000;
	private static final int VIDEO_FRAMERATE = 30;
	private static final int NUM_PREVIEW_FRAGMENTS = 20;
    private static final int REQUEST_VIDEO_SENT = 1459;

    private static int DEFAULT_CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_BACK;

	private SurfaceView surfaceView;
	private VideoView videoView;

	SurfaceHolder previewHolder = null;
	Button rotateButton;
//	ImageButton btnClose, btnBack, btnOK, btnCamera;
//	View redButton, redButtonCircle;
	
	TextView startStoryMessage;
//	videocapReadyMessage;
	ImageView counterOverlay, sliderOverlay, avatar;//, controlClose, controlBack;
	ProgressBar progress, customRecProgress;

	MediaRecorder recorder;
	private Camera camera;

	JSONObject lastFragment = null;
	private String mLastFragmentPath = null;
	private boolean playsEarlierFragment;
	
	public static final int STATE_NO_STORY = -1;
	public static final int STATE_INITIAL = 0;
	public static final int STATE_PREV_CAM = 1;
	public static final int STATE_REC = 2;
	public static final int STATE_PREV_NEW = 3;
	public static final int STATE_UPLOAD = 4;
	public static final int STATE_UPLOAD_FAIL = 5;
	
	private static final int MAX_NEXT_FRAGMENT_ATTEMPTS =5;

	private int mLastState = STATE_NO_STORY;
	private int cameraOrientation;
	private Camera.Size bestPreviewSize;
//	private Camera.Size bestRecordSize;

	private Integer mCurrentCameraId = null;
	private boolean mStartNewMode = false;
	private final static long NULL_RESPONSE_CLIP=-1L;
	private final static long NULL_CHAN=-1L;

	private long mRespondToClipId = NULL_RESPONSE_CLIP;
    private String mRespondToClipUrl = null;
    private String mLastUserAvatar = null;

	private long mCurrentChanlId = NULL_CHAN;
	private long mMovieId = -1L;
	private String mLastUserUuid = null;
	private String recordingCompletedTsAsISO = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(LOGTAG, "onCreate");
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
//		videoView.setOnTouchListener(gestureListener);
		
		counterOverlay = (ImageView)findViewById(R.id.counterOverlay);
		sliderOverlay = (ImageView)findViewById(R.id.sliderOverlay);
		avatar = (ImageView)findViewById(R.id.avatar);
		progress = (ProgressBar) findViewById(R.id.progress);
		customRecProgress = (ProgressBar) findViewById(R.id.customProgressBar);
		
		CameraUtility.viewToSquare(counterOverlay, VideoCaptureActivity.this);
    	
		if (previewHolder == null) {
			previewHolder = surfaceView.getHolder();
		}

		previewHolder.addCallback(this);
		
		aq.id(R.id.btnReply).clicked(this, "workflowClickedButtonCb");
		aq.id(R.id.btnSend).clicked(this, "workflowClickedButtonCb");
		aq.id(R.id.btnCamera).clicked(this, "workflowClickedButtonCb");
		aq.id(R.id.videocapReadyMessage).clicked(this, "workflowClickedMessageCb");
		
//		videocapReadyMessage = (TextView)findViewById(R.id.videocapReadyMessage);
		startStoryMessage = (TextView)findViewById(R.id.startStoryMessage);

		aq.id(R.id.btnClose).clicked(this, "backOrCloseClickedCb");
		aq.id(R.id.btnBack).clicked(this, "backOrCloseClickedCb");
		
		rotateButton = aq.id(R.id.rotateButton).getButton();
		aq.id(R.id.rotateButton).clicked(this, "switchCameraClickedCb");
		
		if( savedInstanceState == null ) {
			// initialize fresh
			mMovieId = getIntent().getLongExtra(MOVIE, -1L);
			mLastUserUuid = getIntent().getStringExtra(LAST_USER_UUID);
            mLastUserAvatar = getIntent().getStringExtra(LAST_USER_AVATAR);
			mRespondToClipId = getIntent().getLongExtra(RESPOND_TO_CLIP, NULL_RESPONSE_CLIP);
            mRespondToClipUrl = getIntent().getStringExtra(RESPOND_TO_CLIP_URL);
            mCurrentChanlId  = getIntent().getLongExtra(CURRENT_CHANNEL, NULL_CHAN);
            mLastFragmentPath = getIntent().getStringExtra(LAST_FRAGMENT_PATH);
			
			if (mLastUserAvatar!=null) {
				aq.id(avatar).image(mLastUserAvatar, true, true, 0, R.drawable.ic_avatar_default);
			}
			
//			Log.v("LOGTAG", "onCreate - savedInstanceState: "+savedInstanceState);
	
			// implicit instruction to start new fragment?
			mStartNewMode = getIntent().getBooleanExtra(MODE_NEW, false);
			if (mStartNewMode) 
			{
				mLastState = STATE_NO_STORY;
				mLastState = processAndSwitchToState(STATE_PREV_CAM);
			}
			else if (mRespondToClipId!=NULL_RESPONSE_CLIP) 
			{
				mLastState = STATE_INITIAL;
                mLastState = processAndSwitchToState(STATE_PREV_CAM);
				startVideoPreloadTask(mRespondToClipUrl);
			}
			else {
				Log.e(LOGTAG, "trying to load fragment carousell");
			}
		}
		else {
			mMovieId = savedInstanceState.getLong(MOVIE);
			mLastUserUuid = savedInstanceState.getString(LAST_USER_UUID);
            mLastUserAvatar = savedInstanceState.getString(LAST_USER_AVATAR);
			mStartNewMode = savedInstanceState.getBoolean(MODE_NEW);
			mCurrentChanlId = savedInstanceState.getLong(CURRENT_CHANNEL);
			mRespondToClipId = savedInstanceState.getLong(RESPOND_TO_CLIP);
            mRespondToClipUrl = savedInstanceState.getString(RESPOND_TO_CLIP_URL);
			mCurrentCameraId = savedInstanceState.getInt(CURRENT_CAMERA);
			mLastState = savedInstanceState.getInt(LAST_STATE);
			
			// TODO hack trying to handle/reset state
			int stateToSet = mLastState;
			if (mLastState<0) mLastState=0;
			mLastState = processAndSwitchToState(stateToSet);
			
		}
		// refresh video length from server
		refreshVideoLengthSetting();
	}
	
	private void init() {

	}
	// - - - callbacks
	
	private int currentLastCarouselItemId = 0;
//	private long currentLastFragmentId = 0;
	
	private long[] fragmentIds = null;


//	private void fragmentCarousel(int i)
//	{
//		mRespondToClipId = fragmentIds[i];
//		currentLastCarouselItemId = i;
//
//		// load fragment
//		if (mRespondToClipId!=NULL_RESPONSE_CLIP)
//		{
//   			// start a story fragment preload task
//	   		startVideoPreloadTask(fragmentIds[i]);
//    	}
//	}
	
//	private void startVideoPreloadTask(long fragmentId) {
//		String fragmentApiUrl = PrefUtility.getApiUrl(ServerUtility.API_CLIP_FILE, "fragment="+fragmentId+"&uuid="+getUuid());
//   		VideoDownloadTask task = new VideoDownloadTask(getApplicationContext(), this);
//   		task.execute(fragmentApiUrl);
//	}
    private void startVideoPreloadTask(String fragmentFileUrl) {
        VideoDownloadTask task = new VideoDownloadTask(getApplicationContext(), this);
        task.execute(fragmentFileUrl);
    }
	private int failedAttempts = 0;
	
	@Override
	public void onVideoTaskCompleted(String cachedFileName, boolean success, boolean wasCached, Exception e) {
		
		hide(progress);
		
		// start playing last fragment and enable control button
		mLastFragmentPath = AppUtility.getVideoCacheDir(getApplicationContext())+"/"+cachedFileName;
		Log.d(LOGTAG, "onVideoTaskCompleted: "+mLastFragmentPath);
		
		if (success) {
			// below will do all the necessart UI cleanup
//			mLastState = processAndSwitchToState(STATE_PREV_LAST);
			failedAttempts = 0;
		}
		else {
//			// swallow, clean up after itself, and try next;
//			failedAttempts++;
//			if (e!=null) {
//				BugSenseHandler.sendException(e);
//			}
//			if (failedAttempts < MAX_NEXT_FRAGMENT_ATTEMPTS)
//			{
//				if (isFragmentCarousel) {
//					playNextCarouselItem(); // try loading next
//				}
//			}
//			else {
				Toast.makeText(this, "Error loading video, please try again.", Toast.LENGTH_SHORT).show();;
				Log.e(LOGTAG, "Video load fail/retry exhausted.");
				failedAttempts = 0;
//			}
		}
	}
	
	public void addFragmentCb(String url, JSONObject json, AjaxStatus status)
	{
        
    	fireGAnalyticsEvent("fragment_workflow", "addFragmentEnd", json==null?"fail":"success", null);
    	
    	if (cancelUpload) {
    		// do nothing
    		cancelUpload = false;
    		Log.v(LOGTAG, "cancelUpload is true, upload response ignored (cleanup needed?)"); // TODO cleanup
    		return;
    	}
    	else if (isAjaxErrorThenReport(status)) {
    		Toast.makeText(this, "Error uploading fragment, please try again", Toast.LENGTH_SHORT).show();
    		mLastState = processAndSwitchToState(STATE_UPLOAD_FAIL);
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

			// go to "video sent, got it!" activity

//			Intent sendActivity = new Intent(this, VideoSendActivity.class);
//			sendActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//			sendActivity.putExtra(MODE_NEW, mStartNewMode);
//            sendActivity.putExtra(CURRENT_CHANNEL, mCurrentChanlId);
//			startActivityForResult(sendActivity, REQUEST_VIDEO_SENT);
            setResult(RESULT_OK);
            finish();

        }else
        {          
            //ajax error
        	apiError(LOGTAG, "Could not upload the fragment, try again.", status, true, Log.ERROR);

        	// restore state
        	mLastState = processAndSwitchToState(STATE_PREV_NEW);
//			lastState = STATE_PREV_NEW;
        }
	}

	// this only activates necessary elements for new state, without taking into account the last state
	public int processAndSwitchToState(int newState) {
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
			gone(avatar);
			hide(startStoryMessage);
			hide(videoView);
			showClose();
			showOk();
			break;
//		case STATE_PREV_LAST:
//			hide(progress);
//			showOk();
//			showClose();
//
//			if (isFragmentCarousel) {
//				ImageUtility.sliderAnimateRightToLeft(sliderOverlay);
//			} else {
//				show(avatar);
//			}
//
//			// start previewing last fragment
//			if (lastFragmentPath!=null) {
//				show(videoView);
//				videoView.setVideoPath(lastFragmentPath);
//				videoView.start();
//				return STATE_PREV_LAST;
//			}
//			else {
//				show(startStoryMessage);
//				return STATE_INITIAL;
//			}
		case STATE_PREV_CAM:
			// hide possibly previously shown elements
			hide(startStoryMessage);
			hide(avatar);
//			hide(videocapReadyMessage);
			hide(videoView);
			show(surfaceView);
			
			//if phone has only one camera, don't show "switch camera" button
			if(Camera.getNumberOfCameras() > 1){
				show(rotateButton);
			}
			
			showCamera();
			
			// only show back button if not starting a new story
//			if (lastFragmentPath!=null){
//				showBack();
//			}
//			else {
				showClose();
//			}
			break;
		case STATE_REC:
			showBack();
			aq.id(R.id.btnReply).gone();
			aq.id(R.id.btnSend).gone();
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
			if (mLastFragmentPath==null) {
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
				videoView.setVideoPath(mLastFragmentPath);
				playsEarlierFragment = true;
				videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer mp) {
						if (playsEarlierFragment && mLastState==STATE_PREV_NEW)
							videoView.setVideoPath(CameraUtility.getNewFragmentFilePath(VideoCaptureActivity.this));
						else
							videoView.setVideoPath(mLastFragmentPath);
						playsEarlierFragment = !playsEarlierFragment;
					    videoView.start();
					}
					});

			}
			
			show(videoView);
			videoView.start();
			showBack();
			showSend();
			
			break;
		case STATE_UPLOAD:
			// upload the video
			doUpload(CameraUtility.getNewFragmentFilePath(this), recordingCompletedTsAsISO);
			break;
		case STATE_UPLOAD_FAIL:
			hide(progress);
			isUploading = false;
			showBack();
			break;
		default:
			Log.w(LOGTAG, "processAndSwitchToState to state not implemented: "+newState);
			break;
		}
		return newState;
	}
	

	final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
	
	private class Counter implements Runnable{
		int c;
		Drawable d;
		public Counter(int c) {
            			int dId = 0;

//			int dId = R.drawable.rec_2;
//			if (c==1) dId = R.drawable.rec_1;
//			if (c==0) dId = R.drawable.rec_0;
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
        	
        	String cs = "CAMERA_"+mCurrentCameraId;
        	if (mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
        		cs = "CAMERA_FACING_FRONT";
        	}
        	else if (mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
        		cs = "CAMERA_FACING_BACK";
        	}
        	
        	fireGAnalyticsEvent("fragment_workflow", "recordingStart", cs, null);

        	
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
				Log.v(LOGTAG, "Tick " + millisUntilFinished);
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
//		counterOverlay.setImageDrawable( getResources().getDrawable(R.drawable.rec_3) );
        counterOverlay.setImageDrawable( getResources().getDrawable(0) );

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


//	public void doUpload1(String filePath, String timeStampAsISO){
//		isUploading = true;
//		cancelUpload = false;
//		showClose();
//		show(progress);
//
//		File file = new File(filePath);
//
//		Map<String, Object> params = new HashMap<String, Object>();
//		params.put("file", file);
//		params.put("uuid", getUuid());
//		Log.v(LOGTAG, "timestamp as ISO: "+timeStampAsISO);
//		params.put("t", timeStampAsISO);
//		if (mMovieId!=-1L)
//        {
//			params.put("l", mMovieId);
//		}
//		Log.v(LOGTAG, "channel: "+mCurrentChanlId);
//		if (mCurrentChanlId!=NULL_CHAN)
//        {
//			params.put("c", mCurrentChanlId+"");
//		}
//
//		fireGAnalyticsEvent("fragment_workflow", "addFragmentStart", "", null);
//
//		aq.ajax(PrefUtility.getApiUrl(ServerUtility.API_CLIP_ADD, null),
//				params, JSONObject.class, VideoCaptureActivity.this, "addFragmentCb").progress(R.id.progress);
//	}

    public void doUpload(String filePath, String timeStampAsISO)
    {
        isUploading = true;
        cancelUpload = false;
        showClose();
        show(progress);

        File file = new File(filePath);

//        Log.v(LOGTAG, "timestamp as ISO: "+timeStampAsISO+", fileName: "+file.getName());

        MultipartEntityBuilder mpeb = MultipartEntityBuilder.create()
                .addBinaryBody("file", file, APPLICATION_OCTET_STREAM, file.getName())
                .addTextBody("uuid", getUuid(), ContentType.TEXT_PLAIN)
                .addTextBody("t", timeStampAsISO, ContentType.TEXT_PLAIN);
        if (mMovieId!=-1L) {
            mpeb.addTextBody("l", mMovieId + "", ContentType.TEXT_PLAIN);
        }
        Log.d(LOGTAG, "uploading to channel with id: "+mCurrentChanlId);
		if (mCurrentChanlId!=NULL_CHAN)
        {
            mpeb.addTextBody("c", mCurrentChanlId + "", ContentType.TEXT_PLAIN);
		}
        HttpEntity reqEntity = mpeb.build();
        String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_CLIP_ADD);

        fireGAnalyticsEvent("fragment_workflow", "addFragmentStart", "", null);
        aq.auth(basicHandle).post(apiUrl, reqEntity, JSONObject.class, new AjaxCallback<JSONObject>() {

            @Override
            public void callback(String url, JSONObject json, AjaxStatus status) {
//                Log.v(LOGTAG, "callback: json="+(json==null?"null":json.toString()));
                addFragmentCb(url, json, status);
            }
        } );
    }
	private boolean isUploading = false;
	private boolean cancelUpload = false;
	
	// WORKFLOW CONTROL click handlers
	
	public void workflowClickedButtonCb()
	{
		fireGAnalyticsEvent("ui_action", "controll_button_from_state", DataUtility.stateStr(mLastState), null);
		workflowClicked() ;
	}

	public void workflowClickedMessageCb()
	{
		fireGAnalyticsEvent("ui_action", "controll_message_from_state", DataUtility.stateStr(mLastState), null);
		workflowClicked() ;
	}
	
	public void workflowClicked() 
	{
		
		switch (mLastState) {
		case STATE_NO_STORY:
			// not creating new stories any more
//			aq.ajax(PrefUtility.getApiUrl()+"startStory?uuid="+getUuid(), JSONObject.class, this, "startStoryCb").progress(progress);
			Log.e(LOGTAG, "workflowClickedCb STATE_NO_STORY");
			break;
//		case STATE_PREV_LAST:
//			// stop previewing last fragment
//			videoView.stopPlayback();
			
		case STATE_INITIAL:
			// switch camera preview on
			mLastState = processAndSwitchToState(STATE_PREV_CAM);
			break;
			
		case STATE_PREV_CAM:
			// START RECORDING
			mLastState = processAndSwitchToState(STATE_REC);
			break;
			
		case STATE_REC:
			// ignore push during recording. recorder stops automatically after 3 secs
			break;
			
		case STATE_PREV_NEW:
			// Stop previewing NEW fragment
			videoView.stopPlayback();
			videoView.setOnCompletionListener(null);
			playsEarlierFragment = false;
		case STATE_UPLOAD_FAIL:
			mLastState = processAndSwitchToState(STATE_UPLOAD);
			break;

		default:
			BugSenseHandler.sendException(new RuntimeException("Undefined state for Controll "+mLastState));
			Log.e(LOGTAG, "control switch in undefined state: "+mLastState);
			break;
			}
	}

	private void refreshVideoLengthSetting() {
		// query API for server config
		String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_SERVER_PROPERTIES, null);
		aq.progress(R.id.progress).ajax(apiUrl, JSONObject.class, this, "getServerPropertiesCb");
	}

	
	public void getServerPropertiesCb(String url, JSONObject json, AjaxStatus status)
	{
		ServerUtility.getServerPropertiesCb(url, json, status, this);
	}
	
	// "BACK" and "CLOSE" control
	public void backOrCloseClickedCb()
	{
		fireGAnalyticsEvent("ui_action", "back_close_control_from_state", DataUtility.stateStr(mLastState), null);
		backOrCloseClicked();
	}
	
	public void backOrCloseClicked() 
	{

		Intent intent;
		switch (mLastState) {
		case STATE_NO_STORY:
		case STATE_INITIAL:
        case STATE_PREV_CAM:
            // todo: obsolete
			// return to the last used playlist
//			intent = new Intent(VideoCaptureActivity.this, AppUtility.ACTIVITY_HOME);
//			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//			intent.putExtra(RESPOND_TO_CLIP, mRespondToClipId);
//            intent.putExtra(RESPOND_TO_CLIP_URL, mRespondToClipUrl);
//			intent.putExtra(CURRENT_CHANNEL, mCurrentChanlId);
//			intent.putExtra(MOVIE, mMovieId);
//			startActivity(intent);
			setResult(RESULT_CANCELED);
            finish();

			break;
//		case STATE_PREV_CAM:
//			// restore button color
//			if (lastFragmentPath!=null)
//			{
//				// stop camera preview and return to last fragment review
//				hide(surfaceView);
//				show(videoView);
//			}
//			hide(rotateButton);
//			if (mStartNewMode) {
//                setResult(RESULT_CANCELED);
//                finish();
//			}
//			else {
//				mLastState = processAndSwitchToState(STATE_PREV_LAST);
//			}
//
//			break;
		case STATE_REC:
			// stop recording
			recorder.stop();
			Log.v(LOGTAG, "Recording Stopped");

			releaseMediaRecorder();
			hide(surfaceView);
			
			mLastState = processAndSwitchToState(STATE_PREV_CAM);

			break;
		case STATE_PREV_NEW:
			// Stop previewing NEW fragment
			videoView.stopPlayback();
			hide(videoView);
			
			// return to camera preview
			mLastState = processAndSwitchToState(STATE_PREV_CAM);

			break;
		case STATE_UPLOAD:
			cancelUpload  = true;
		case STATE_UPLOAD_FAIL:
			
			mLastState = processAndSwitchToState(STATE_PREV_NEW);
			break;
		default:
			Log.e(LOGTAG, "back pressed while in undefined state "+mLastState);
			BugSenseHandler.sendException(new RuntimeException("Undefined state for Back "+mLastState));
			break;
		}
	}
	
	// "SWITCH CAMERA" button control
	public void switchCameraClickedCb() {

		fireGAnalyticsEvent("ui_action", "touch", "switchCamera", null);

		if (mLastState==STATE_PREV_CAM) {
			// todo: rotate
			if (mLastState == STATE_PREV_CAM) {
		        camera.stopPreview();
		    }
			//NB: if you don't release the current camera before switching, you app will crash
		    camera.release();
		    //swap the id of the camera to be used
		    mCurrentCameraId++;
		    if (mCurrentCameraId >= Camera.getNumberOfCameras())
		    	mCurrentCameraId = 0;
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
//		holder.removeCallback(this);
		camera.release();
		camera = null;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		camera = getCameraInstance();
		if (camera==null) {
			Log.e(LOGTAG, "camera is null");
			return;
		}
		
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
			if (mCurrentCameraId==null) {
				// get default camera, if it exists, otherwise use first available
				int cameras = Camera.getNumberOfCameras();
				Log.v(LOGTAG, "Cameras: " + cameras);
				mCurrentCameraId = 0;
				for (int i=0;i<cameras;i++) {
					if (i==DEFAULT_CAMERA_ID) {
						mCurrentCameraId = i;
					}
				}
			}
			
			c = Camera.open(mCurrentCameraId); // attempt to get a Camera instance
			
			// not much difference when is this being set
			cameraOrientation = CameraUtility.getCameraDisplayOrientation(this, mCurrentCameraId, true);
			c.setDisplayOrientation(cameraOrientation);
			
			Log.d(LOGTAG, "cameraOrientation "+cameraOrientation);
			
			Camera.Parameters cp = c.getParameters();
            // This should be called before starting preview for the best result
            cp.setRecordingHint(true);

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
			
			// This should be called before starting preview for the best result
//			cp.setRecordingHint(true);
			
			List<String> focusModes = cp.getSupportedFocusModes();
            // Autofocus modes supported
            AutofocusMode afm = PrefUtility.getAutofocusMode();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) && AutofocusMode.FAST.equals(afm))
            {
                cp.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                fireGAnalyticsEvent("camera", "autoFocusMode", "FOCUS_MODE_CONTINUOUS_PICTURE", null);
                Log.d(LOGTAG, "FOCUS_MODE_CONTINUOUS_PICTURE");

            }
            else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) && !AutofocusMode.AUTO.equals(afm))
            {
                cp.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                fireGAnalyticsEvent("camera", "autoFocusMode", "FOCUS_MODE_CONTINUOUS_VIDEO", null);
                Log.d(LOGTAG, "FOCUS_MODE_CONTINUOUS_VIDEO");

            }
            else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
            {
			  	cp.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                fireGAnalyticsEvent("camera", "autoFocusMode", "FOCUS_MODE_AUTO", null);
                Log.d(LOGTAG, "FOCUS_MODE_AUTO");

            }
            else {
                fireGAnalyticsEvent("camera", "autoFocusMode", "NONE", null);
                Log.d(LOGTAG, "FOCUS_MODE:NONE");
            }


			c.setParameters(cp);
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
			Log.e(LOGTAG, "Camera is not available (in use or does not exist), camera id "+mCurrentCameraId);
			BugSenseHandler.sendException(new Exception("Camera not available "+mCurrentCameraId));
		}
		return c; // returns null if camera is unavailable
	}


	private void prepareRecorder() {
        camera.stopPreview(); // this fixes https://github.com/storyroll/storyroll-android/issues/190
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
		int orientationHint = CameraUtility.getCameraDisplayOrientation(this, mCurrentCameraId, false);
        recorder.setOrientationHint(orientationHint);
        
		recorder.setPreviewDisplay(previewHolder.getSurface());

		try {
			recorder.prepare();
			Log.v(LOGTAG, "Recorder prepared");

		} catch (IllegalStateException e) {
			Log.e(LOGTAG, "State Error preparing MediaRecorder", e);
			BugSenseHandler.sendException(e);
			finish(); // todo
		} catch (IOException e) {
			Log.e(LOGTAG, "I/O Error preparing MediaRecorder", e);
			BugSenseHandler.sendException(e);
			finish(); // todo
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
		    recordingCompletedTsAsISO = TimeISO8601.now();
		    
		    
			hide(customRecProgress);
			
			Log.v(LOGTAG, "OnInfoListener: Maximum Duration Reached");
			mr.stop();
			Log.v(LOGTAG, "Recording Stopped");

			// here comes "hidden" stop state processing step
			releaseMediaRecorder();
			hide(surfaceView);
			mLastState = processAndSwitchToState(STATE_PREV_NEW);
		}
	}

//	@Override
//	protected void leftSwipe() {
//		if (mLastState!=STATE_PREV_LAST) return;
//		fireGAnalyticsEvent("fragment_workflow", "swipe", "left", 0L);
//		// next fragment
//		playNextCarouselItem();
//	}
//
//	private void playNextCarouselItem(){
//		fragmentCarousel(++currentLastCarouselItemId<fragmentIds.length?currentLastCarouselItemId:0);
//	}
//
//	@Override
//	protected void rightSwipe() {
//		if (mLastState!=STATE_PREV_LAST) return;
//		fireGAnalyticsEvent("fragment_workflow", "swipe", "right", 0L);
//		show(progress);
//		// previous fragment
//		fragmentCarousel(--currentLastCarouselItemId>=0?currentLastCarouselItemId:fragmentIds.length-1);
//	}
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
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
    		fireGAnalyticsEvent("ui_action", "system_back_from_state", DataUtility.stateStr(mLastState), null);
    		backOrCloseClicked();
            return true;
        }
        return super.onKeyUp(keyCode, event);
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
		aq.id(R.id.btnCamera).gone();
		aq.id(R.id.btnSend).gone();
		aq.id(R.id.btnReply).visible();		
	}
	private void showSend() {
		aq.id(R.id.btnCamera).gone();
		aq.id(R.id.btnSend).visible();
		aq.id(R.id.btnReply).gone();		
	}
	private void showCamera() {
		aq.id(R.id.btnCamera).visible(); 
		aq.id(R.id.btnReply).gone();
		aq.id(R.id.btnSend).gone();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) 
	{
		Log.v(LOGTAG, "onSaveInstanceState");
		super.onSaveInstanceState(outState);
		outState.putBoolean(MODE_NEW, mStartNewMode);
		outState.putLong(CURRENT_CHANNEL, mCurrentChanlId);
		outState.putLong(RESPOND_TO_CLIP, mRespondToClipId);
        outState.putString(RESPOND_TO_CLIP_URL, mRespondToClipUrl);
		outState.putLong(MOVIE, mMovieId);
		
		outState.putInt(CURRENT_CAMERA, mCurrentCameraId);
		outState.putInt(LAST_STATE, mLastState);
		outState.putString(LAST_USER_UUID, mLastUserUuid);
        outState.putString(LAST_USER_AVATAR, mLastUserAvatar);
        outState.putString(LAST_FRAGMENT_PATH, mLastFragmentPath);
		
	}
	
	@Override
	public void onStop() {

// see http://stackoverflow.com/questions/11022031/managing-camera-preview-surfaceview-throughout-the-activity-lifecycle
//				  if (mCamera != null) {
//				        mCamera.stopPreview();
//				        mCamera.release();        // release the camera for other applications
//				        mCamera = null;
//				    }
//				    if (mCameraPreview != null) {
//				        mLayoutRoot.removeView(mCameraPreview);
//				        mCameraPreview = null;
//				    }
//				    super.onPause();
//				    Log.d(TAG, "onPause OUT mCamera, mCameraPreview: " + mCamera + ", " + mCameraPreview);	  
		
		// but we stop camera in onSurfaceDestroy
		
		Log.v(LOGTAG, "onStop");
		super.onStop();
	}
	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		Log.v(LOGTAG, "onStart");
		super.onStop();
	}

}
