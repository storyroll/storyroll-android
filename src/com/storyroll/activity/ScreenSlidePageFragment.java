/*
 * Copyright 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.storyroll.activity;

import java.io.File;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.androidquery.callback.AjaxStatus;
import com.storyroll.PQuery;
import com.storyroll.R;
import com.storyroll.tasks.VideoDownloadTask;
import com.storyroll.tasks.VideoDownloadTask.OnVideoTaskCompleted;
import com.storyroll.util.AppUtility;

import android.app.Fragment;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

/**
 * A fragment representing a single step in a wizard. The fragment shows a dummy title indicating
 * the page number, along with some dummy text.
 *
 * <p>This class is used by the {@link CardFlipActivity} and {@link
 * ScreenSlideActivity} samples.</p>
 */
public class ScreenSlidePageFragment extends Fragment implements OnVideoTaskCompleted {
	private final static String LOGTAG = "PLAYROLL";
    /**
     * The argument key for the page number this fragment represents.
     */
    public static final String ARG_PAGE = "page";
    public static final String ARG_STORY = "story";
    public static final String ARG_UUID = "uuid";

    /**
     * The fragment's page number, which is set to the argument value for {@link #ARG_PAGE}.
     */
    private int mPageNumber;

    private ViewGroup rootView;
    private VideoView videoView = null;
    private ImageView mVideoSnapshot;
    private ProgressBar progressBar;
    private ImageView mLikeImage;
	private Integer mStoryId = null;
	private String mUuid;
	private boolean isLoading = false;


//	boolean playing = false;
	boolean loaded = false;
	boolean playQueued = false;
	
	public void queueStartVideo() {
		Log.v(LOGTAG, "queued video start");
		if (videoView!=null && !videoView.isPlaying()) {
			if (loaded) {
				startVideo();
			}
			else {
				playQueued = true;
			}
		}
		else {
			playQueued = true;
		}
	}

	public void startVideo() {
		Log.v(LOGTAG, "starting video");
		videoView.start();
		playQueued = false;
	}
	
	public void stopVideo() {
		Log.v(LOGTAG, "stopping video");
		if (videoView!=null)
			videoView.pause();
	}
	
	/**
     * Factory method for this fragment class. Constructs a new fragment for the given page number.
     */
    public static ScreenSlidePageFragment create(int pageNumber, int storyId, String uuid) {
    	Log.v(LOGTAG, "create fragment no "+pageNumber);
        ScreenSlidePageFragment fragment = new ScreenSlidePageFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, pageNumber);
        args.putInt(ARG_STORY, storyId);
        args.putString(ARG_UUID, uuid);
        fragment.setArguments(args);
        return fragment;
    }

    public ScreenSlidePageFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageNumber = getArguments().getInt(ARG_PAGE);
        mStoryId = getArguments().getInt(ARG_STORY);
        mUuid =getArguments().getString(ARG_UUID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout containing a title and body text.
        rootView = (ViewGroup) inflater
                .inflate(R.layout.fragment_roll_slide, container, false);
        
        // set both snapshot and video preview to square
        final int screenWidth = getActivity().getWindowManager().getDefaultDisplay().getWidth();
        
        
        mVideoSnapshot = (ImageView) rootView.findViewById(R.id.videoSnapshot);
        // TODO: load video snapshot
//        mVideoSnapshot.setImageResource(R.drawable.video_screen);
    	
    	Uri imgUri = Uri.parse(AppUtility.API_URL+"storyThumb?story="+mStoryId);
    	mVideoSnapshot.setImageURI(imgUri);
    	setViewSquare(mVideoSnapshot, screenWidth);

        progressBar = (ProgressBar) rootView.findViewById(R.id.progress);
        mVideoSnapshot.setOnClickListener(new ImageView.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!isLoading) {
			   		// start a video preload task
			        progressBar.setVisibility(View.VISIBLE);
			        isLoading = true;
//			        String url = "https://archive.org/download/Pbtestfilemp4videotestmp4/video_test_512kb.mp4";
			        String url = AppUtility.API_URL+"storyFile?story="+mStoryId;
			        		        
			   		VideoDownloadTask task = new VideoDownloadTask(getActivity().getApplicationContext(), ScreenSlidePageFragment.this);
			        task.execute(url);
				}
			}
		});
        
        
        videoView = (VideoView) rootView.findViewById(R.id.videoPlayerView);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener()  {
            @Override
            public void onPrepared(MediaPlayer mp) {                         
            	Log.i(LOGTAG, "Duration = " + videoView.getDuration());
            	
            	// Adjust the size of the video
                // to fit width of the screen
//                int videoWidth = mp.getVideoWidth();
//                int videoHeight = mp.getVideoHeight();
//                float videoProportion = (float) videoWidth / (float) videoHeight;       
//                int screenWidth = getActivity().getWindowManager().getDefaultDisplay().getWidth();
//                int screenHeight = getActivity().getWindowManager().getDefaultDisplay().getHeight();
//                android.view.ViewGroup.LayoutParams lp = videoView.getLayoutParams();
//                lp.width = screenWidth;
//                lp.height = (int) ((float) screenWidth / videoProportion);
//                videoView.setLayoutParams(lp);
            	
            	// set preview window to square
            	setViewSquare(videoView, screenWidth);
            	
            	// finally make visible
            	videoView.setVisibility(View.VISIBLE);
            	
            	mp.setLooping(true);
        		if (playQueued) {
        			startVideo();
        		}
            }
        });
        videoView.setOnTouchListener(new VideoView.OnTouchListener()
        {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
                Log.i(LOGTAG, "View.OnTouchListener: Up");
                if (videoView.isPlaying()) {
					stopVideo();
				}
				else {
					startVideo();
				}
			    return false;
			}
        });

        
		mLikeImage = (ImageView) rootView.findViewById(R.id.likeImage);
		// TODO: set user-story like state, and overall likes number
		// ...
		mLikeImage.setOnClickListener(new ImageView.OnClickListener() {
			@Override
			public void onClick(View v) {
				String url = AppUtility.API_URL + "like?uuid="+ mUuid +"&story="+mStoryId;             
		        aq.ajax(url, JSONObject.class, ScreenSlidePageFragment.this, "likeCallback");	
			}
		});
        
   		// load story cast from API
   		// TODO: real id
        mStoryId = 10;
        aq = ((RollFlipPlayActivity)getActivity()).getPQuery();
        
   		aq.ajax(AppUtility.API_URL+"getStoryCast?story="+mStoryId, JSONArray.class, this, "getStoryCastCb");

        // Set the title view to show the page number.
//        ((TextView) rootView.findViewById(android.R.id.text1)).setText(
//                getString(R.string.title_template_step, mPageNumber + 1));

        return rootView;
    }
    
    PQuery aq;
	private final static int[] castIds = {R.id.cast1, R.id.cast2, R.id.cast3, R.id.cast4, R.id.cast5};

	public void getStoryCastCb(String url, JSONArray jarr, AjaxStatus status){
        progressBar.setVisibility(View.GONE);

        if(jarr != null){               
            //successful ajax call
        	Log.i(LOGTAG, "getStoryCastCb success: "+jarr.toString());
        	// make cast avatars visible
    		try {
	        	for (int i = 0; i < jarr.length(); i++) {
						JSONObject userObj = jarr.getJSONObject(i);
						ImageView castImage = (ImageView) rootView.findViewById(castIds[i]);
						aq.id(castImage).image(AppUtility.API_URL+"avatar?uuid="+userObj.getString("uuid"), false, false, 0, R.drawable.ic_avatar_default);
	        	}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		
        }else{          
            //ajax error
        	Log.e(LOGTAG, "getStoryCastCb: null Json, cast not received for storyId="+mStoryId);
        }
       
	}	
    
    @Override
    public void onHiddenChanged(boolean hidden) {
        Log.v(LOGTAG, "onHiddenChanged: "+hidden);
    	if (videoView!=null && hidden) {
    		stopVideo();
    	}
    	if (videoView!=null && !hidden) {
    		startVideo();
    	}
    };

	// once video download complete, play it
	@Override
	public void onVideoTaskCompleted(String fileName) {
		progressBar.setVisibility(View.GONE);
		mVideoSnapshot.setVisibility(View.GONE);
		videoView.setVisibility(View.VISIBLE);
		isLoading = false;
		
		String videoFilePath = AppUtility.getVideoCacheDir(getActivity().getApplicationContext())+File.separator+fileName;
		Log.d(LOGTAG, "onVideoTaskCompleted: "+videoFilePath);

		videoView.setVideoPath(videoFilePath);
		loaded = true;
		
		if (playQueued) {
			startVideo();
		}
	}
	
    /**
     * Returns the page number represented by this fragment object.
     */
    public int getPageNumber() {
        return mPageNumber;
    }
    
	public void likeCb(String url, JSONObject json, AjaxStatus status){
        
        if(json != null){               
            //successful ajax call
        	Log.i(LOGTAG, "likeCb success: "+json.toString());
        	// update like icon and increase the count
        	mLikeImage.setImageResource(R.drawable.ic_star_on);
			
        }else{          
            //ajax error
        	Log.w(LOGTAG, "likeCb: Json null");
        }
        
	}
	
	private void setViewSquare(View v, int screenWidth) {
		// set preview window to square
		android.view.ViewGroup.LayoutParams lp = v.getLayoutParams();
		lp.width = screenWidth;
		lp.height = screenWidth;
		v.setLayoutParams(lp);
	}

}
