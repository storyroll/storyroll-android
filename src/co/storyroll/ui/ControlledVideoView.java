package co.storyroll.ui;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.VideoView;
import co.storyroll.R;
import co.storyroll.activity.ArrayListFragment;
import co.storyroll.tasks.VideoDownloadTask;
import co.storyroll.tasks.VideoDownloadTask.OnVideoTaskCompleted;
import co.storyroll.util.AppUtility;
import co.storyroll.util.PrefUtility;
import co.storyroll.util.ServerUtility;

import java.io.File;

public class ControlledVideoView extends VideoView implements OnVideoTaskCompleted {
	private static final String LOGTAG = "ControlledVideoView";

	protected static final boolean LOOPING = true;
	
	public boolean isLoading = false;
	public boolean isLoaded = false;
	boolean playQueued = true;
	private View controlView, unseenIndicator;
	private ImageView playControl;
	private ArrayListFragment parent;
	int screenWidth;
	private int itemPosition;
	private long storyId;
	private String uuid;

	private ProgressBar progressBar;

	public ControlledVideoView(Context context) {
		super(context);
	}
	public ControlledVideoView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
    public ControlledVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
       
    }
	
	public void queueStartVideo() {
		Log.v(LOGTAG, "queued video start");
		if (!isPlaying() && isLoaded) 
		{
			playQueued = false;
			startVideo();
		}
		else {
			playQueued = true;
		}	
	}
	
	public void startVideoPreloading(boolean autoStart) {
		Log.v(LOGTAG, "startVideoPreloading, autostart: "+autoStart);
		markPlayable(false);
		if (autoStart) queueStartVideo();
		if (!isLoading) {
	   		// start a video preload task
//			if(progressBar!=null) {
				progressBar.setVisibility(View.VISIBLE);
//			}
			isLoading = true;
//		        String url = "https://archive.org/download/Pbtestfilemp4videotestmp4/video_test_512kb.mp4";
	        String url = PrefUtility.getApiUrl(ServerUtility.API_STORY_FILE, "story=" + storyId + "&uuid=" + uuid);
	        		        
	   		VideoDownloadTask task = new VideoDownloadTask(getContext().getApplicationContext(), this);
	        task.execute(url);
		}
	}
	
	public void queueStopVideo() {
		Log.v(LOGTAG, "queued video stop");
		if (playQueued) {
			playQueued = false;
		}
		else if (isPlaying()) {
			stopPlayback();
			markPlayable(true);
		}
	}

	public void startVideo() {
		parent.switchCurrentlyPlayed(this);
		markPlayable(false);
		Log.v(LOGTAG, "starting video playback");
		start();
		playQueued = false;
	}
	
	public void pauseVideo() {
		Log.v(LOGTAG, "stopping video");
		markPlayable(true);
		pause();
	}
	
	public void init(ArrayListFragment parent, View controlView, int screenWidth, int itemPosition, long storyId, String uuid, 
			ProgressBar progressBar, View unseenIndicator, ImageView playControl) {
		this.controlView = controlView;
		this.screenWidth = screenWidth;
		this.itemPosition = itemPosition;
		this.parent = parent;
		this.storyId = storyId;
		this.uuid = uuid;
		this.progressBar = progressBar;
		this.unseenIndicator = unseenIndicator;
		this.playControl = playControl;
		
		setOnPreparedListener(new MediaPlayer.OnPreparedListener()  {
            @Override
            public void onPrepared(MediaPlayer mp) {                         
            	Log.i(LOGTAG, "Duration = " + getDuration());

            	
            	// set preview window to square
            	setViewSquare();
            	
            	// finally make visible
            	setVisibility(View.VISIBLE);
            	
            	mp.setLooping(LOOPING);
        		if (playQueued) {
        			startVideo();
        		}
            }
        });
		
		setOnTouchListener(new VideoView.OnTouchListener()
        {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (MotionEvent.ACTION_UP == event.getAction() ) 
				{
	                if (isPlaying()) {
						pauseVideo();
					}
					else {
						startVideo();
					}
	                return true;
				}
			    return true;
			}
        });
	}
	
	
	
	// once video download complete, play it
	@Override
	public void onVideoTaskCompleted(String fileName, boolean success, boolean wasCached, Exception e) 
	{
		progressBar.setVisibility(View.GONE);
		controlView.setVisibility(View.GONE);
		setVisibility(View.VISIBLE);
		setViewSquare();
		isLoading = false;
		
		String videoFilePath = AppUtility.getVideoCacheDir(getContext().getApplicationContext())+File.separator+fileName;
		Log.d(LOGTAG, "onVideoTaskCompleted: "+videoFilePath+", success: "+success);

		if (success) {
			setVideoPath(videoFilePath);
			isLoaded = true;
			
			if (playQueued) {
				startVideo();
			}
		}
		else {
			Log.e(LOGTAG, "Eror loading video", e);
		}
	}

	private void setViewSquare() {
		// set preview window to square
		android.view.ViewGroup.LayoutParams lp = getLayoutParams();
		lp.width = screenWidth;
		lp.height = screenWidth;
		setLayoutParams(lp);
	}
	
	public long getItemPosition() {
		return itemPosition;
	}
	
	
	public long getStoryId() {
		return storyId;
	}
	public String getUuid() {
		return uuid;
	}
	public void markSeen() {
		unseenIndicator.setVisibility(View.INVISIBLE);
		playControl.setImageResource(R.drawable.ic_play_roll);
		playControl.setVisibility(View.VISIBLE);
	}
	public void markPlayable(boolean playable) {
		Log.v(LOGTAG, "markPlayable: "+playable);
		playControl.setVisibility(playable?View.VISIBLE:View.INVISIBLE);
	}
}
