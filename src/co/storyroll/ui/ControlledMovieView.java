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
import co.storyroll.adapter.MovieAdapter;
import co.storyroll.model.Clip;
import co.storyroll.model.Movie;
import co.storyroll.tasks.VideoDownloadTask;
import co.storyroll.tasks.VideoDownloadTask.OnVideoTaskCompleted;
import co.storyroll.util.AppUtility;

import java.io.File;

public class ControlledMovieView extends VideoView implements OnVideoTaskCompleted {
	private static final String LOGTAG = "ControlledMovieView";

	protected static final boolean LOOPING = true;
	
	public boolean isLoading = false;
	public boolean isLoadedSeq = false;
    public boolean isLoadedQuad = false;
	boolean playQueued = true;
	private View controlView, unseenIndicator;
	private ImageView playControl;
//	private ArrayMoviesFragment parentFragment = null;
    private MovieAdapter parentAdapter = null;
	int screenWidth;
	private int itemPosition;
	private long mMovieId;
	private String mUuid;
	private long mUpdateTag;
	private String movieFileUrl;
    private String movieQuadFileUrl;
    private boolean isQuadPlay = false;

	private ProgressBar progressBar;

	private Clip mVideo;


	public ControlledMovieView(Context context) {
		super(context);
	}
	public ControlledMovieView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
    public ControlledMovieView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
       
    }
	
	public void queueStartVideo(boolean toggle) {
		Log.v(LOGTAG, "queued video start");
        boolean isLoaded = isQuadPlay?isLoadedQuad:isLoadedSeq;
		if (!isPlaying() && isLoaded && !toggle)
		{
			playQueued = false;
			startVideo();
		}
		else {
			playQueued = true;
		}	
	}
	
	public void startVideoPreloading(boolean autoStart, boolean toggle)
	{
		Log.v(LOGTAG, "startVideoPreloading, autostart: "+autoStart);
        indicatePlayable(false);
		if (autoStart) queueStartVideo(toggle);
		if (!isLoading) {
	   		// start a video preload task
			if(progressBar!=null) {
				progressBar.setVisibility(View.VISIBLE);
			}
			isLoading = true;
//		        String url = "https://archive.org/download/Pbtestfilemp4videotestmp4/video_test_512kb.mp4";
//	        String url = PrefUtility.getApiUrl(ServerUtility.API_MOVIE_FILE, "story="+mMovieId+"&uuid="+mUuid+"&updateTag="+mUpdateTag);
	        		        
	   		VideoDownloadTask task = new VideoDownloadTask(getContext().getApplicationContext(), this);
            if (isQuadPlay) {
                task.execute(movieQuadFileUrl);
            }
            else {
                task.execute(movieFileUrl + "#" + mUpdateTag);
            }
		}
	}
	
	public void queueStopVideo() {
		Log.v(LOGTAG, "queued video stop");
		if (playQueued) {
			playQueued = false;
		}
		else if (isPlaying()) {
//			stopPlayback();
            pauseVideo();
            indicatePlayable(true);
		}
	}

	public void startVideo() {
		Log.v(LOGTAG, "will start video playback");

        if (parentAdapter!=null) {
            parentAdapter.switchCurrentlyPlayedMovie(this);
        }
//        else if (parentFragment!=null) {
//            parentFragment.switchCurrentlyPlayedMovie(this);
//        }
        indicatePlayable(false);
		Log.v(LOGTAG, "starting video playback");
		start();
		playQueued = false;
	}
	
	public void pauseVideo() {
		Log.v(LOGTAG, "pausing video");
        indicatePlayable(true);
		pause();
	}

    public void adapterInit(MovieAdapter parentAdapter, View controlView, int screenWidth, int itemPosition, Movie movie, String uuid,
                     ProgressBar progressBar, View unseenIndicator, ImageView playControl) {
        init(controlView, screenWidth, itemPosition, movie, uuid, progressBar, unseenIndicator, playControl);
        this.parentAdapter = parentAdapter;

    }
    public void adapterInit(MovieAdapter parentAdapter, View controlView, int screenWidth, int itemPosition, Clip movie, String uuid,
                            ProgressBar progressBar, View unseenIndicator, ImageView playControl) {
        init(controlView, screenWidth, itemPosition, movie, uuid, progressBar, unseenIndicator, playControl);
        this.parentAdapter = parentAdapter;

    }
    public void init(View controlView, int screenWidth, int itemPosition, Movie video, String uuid,
                     ProgressBar progressBar, View unseenIndicator, ImageView playControl) {
        init(controlView, screenWidth, itemPosition, (Clip)video, uuid, progressBar, unseenIndicator, playControl);
        this.movieQuadFileUrl = video.getQuadFileUrl();
        Log.v(LOGTAG, "movieQuadFileUrl: "+movieQuadFileUrl);

    }
	
	public void init(View controlView, int screenWidth, int itemPosition, Clip video, String uuid,
			ProgressBar progressBar, View unseenIndicator, ImageView playControl) {
		this.controlView = controlView;
		this.screenWidth = screenWidth;
		this.itemPosition = itemPosition;
		this.mMovieId = video.getId();
		this.mVideo = video;
		this.mUuid = uuid;
		this.progressBar = progressBar;
//		this.unseenIndicator = unseenIndicator;
		this.playControl = playControl;
		this.mUpdateTag = video.getPublishedOn();
        this.movieFileUrl = video.getFileUrl();
        Log.v(LOGTAG, "init: "+video.getFileUrl());

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
					Log.v(LOGTAG, "onTouch==MotionEvent.ACTION_UP");
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
	
	
	
	// video download complete
	@Override
	public void onVideoTaskCompleted(String fileName, boolean success, boolean wasCached, Exception e) 
	{
        String videoFilePath = AppUtility.getVideoCacheDir(getContext().getApplicationContext())+File.separator+fileName;
        Log.d(LOGTAG, "onVideoTaskCompleted: "+videoFilePath+", success: "+success);
        if (progressBar!=null) {
            progressBar.setVisibility(View.GONE);
        }
		isLoading = false;

		if (success) {
            controlView.setVisibility(View.GONE);
            setVisibility(View.VISIBLE);
            setViewSquare();
            setVideoPath(videoFilePath);
            if (isQuadPlay())
			    isLoadedQuad = true;
            else
                isLoadedSeq = true;
			
			if (playQueued) {
                if (mVideo instanceof Movie)
                {
                    ((Movie) mVideo).setSeen(true);
                }
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
	
	
	public long getMovieId() {
		return mMovieId;
	}
	public String getUuid() {
		return mUuid;
	}
	public void indicateSeenPlayable() {
        if (unseenIndicator!=null) {
            unseenIndicator.setVisibility(View.INVISIBLE);
        }
        if (playControl!=null) {
            playControl.setImageResource(R.drawable.ic_play_roll);
            playControl.setVisibility(View.VISIBLE);
        }
	}
	public void indicatePlayable(boolean playable) {
        if (playControl!=null) {
            Log.v(LOGTAG, "indicatePlayable: " + playable);
            playControl.setVisibility(playable ? View.VISIBLE : View.INVISIBLE);
        }
	}

    public Clip getVideo() {
        return mVideo;
    }

    public String getVideoType() {
        return (mVideo instanceof Movie)?"movie":"clip";
    }

    public boolean isQuadPlay() {
        return isQuadPlay;
    }

    public void toggleQuadPlay() {
        isQuadPlay = !isQuadPlay;
        Log.v(LOGTAG, "isQuadPlay: "+isQuadPlay);
        if (isPlaying()) {
            stopPlayback();
            // play new movie
            startVideoPreloading(true, true);
        }
    }
}
