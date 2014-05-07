package com.storyroll.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.storyroll.PQuery;
import com.storyroll.R;
import com.storyroll.enums.AutostartMode;
import com.storyroll.model.Clip;
import com.storyroll.model.Story;
import com.storyroll.ui.ClipListItemView;
import com.storyroll.ui.ControlledClipView;
import com.storyroll.ui.RoundedImageView;
import com.storyroll.util.AppUtility;
import com.storyroll.util.ErrorUtility;
import com.storyroll.util.NetworkUtility;
import com.storyroll.util.PrefUtility;
import com.storyroll.util.ViewUtility;

public class ArrayClipsFragment extends ListFragment {
	private static final String LOGTAG = "ArrayListFragment";

	public static final int TAB_ONE = 0;
	public static final int TAB_TWO = 1;

	public static final String[] TAB_HEADINGS = new String[] { "One", "Two" };
	public static final String[] TAB_HEADINGS_TRIAL = new String[] { "One", null };
	
	private static final Integer LIMIT_ITEMS = 40;
	
	// how many percents of width should a squared video take
	private static final int PERCENT_SQUARE = 95;

	// the range of display which acts as an "autostart" area for videos
	// when the center of VideoView enters this range, it is autostarted
	// TODO: must be calculated as a function of display w, h and PERCENT_SQUARE
	private static final float AUTO_RANGE_TOP = 0.33f;
	private static final float AUTO_RANGE_BOTTOM = 0.75f;


	private int mNum;
	private String mUuid;
	private boolean isTrial;
	private PQuery aq;
	
	private ArrayList<Clip> clips = new ArrayList<Clip>();
	public static Set<String> unseenClips = null;
    

	/**
	 * Create a new instance of CountingFragment, providing "num" as an
	 * argument.
	 */
	static ArrayClipsFragment newInstance(int num, String uuid, boolean isTrial) {
		ArrayClipsFragment f = new ArrayClipsFragment();

		// Supply num input as an argument.
		Bundle args = new Bundle();
		args.putInt("num", num);
		args.putString("uuid", uuid);
		args.putBoolean("trial", isTrial);
		f.setArguments(args);

		return f;
	}

	/**
	 * When creating, retrieve this instance's number from its arguments.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mNum = getArguments() != null ? getArguments().getInt("num") : 1;
		mUuid = getArguments() != null ? getArguments().getString("uuid") : "";
		isTrial = getArguments() != null ? getArguments().getBoolean("trial") : false;
		
		aq = ((ClipPlaylistActivity) getActivity()).getPQuery();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_tab_playlist, container, false);
		// View tv = v.findViewById(R.id.text);
		// ((TextView)tv).setText("Fragment #" + mNum);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.v(LOGTAG, "onActivityCreated");
		super.onActivityCreated(savedInstanceState);

		String apiUrl = PrefUtility.getApiUrl();
		switch (mNum) {
		case TAB_ONE:
			apiUrl += "available?uuid=" + mUuid + "&limit=" + LIMIT_ITEMS;
			aq.progress(R.id.progress).ajax(apiUrl, JSONArray.class, this,
					"getClipListCb");

			break;
		case TAB_TWO:
			apiUrl += "available?uuid=" + mUuid + "&limit=" + LIMIT_ITEMS;
			aq.progress(R.id.progress).ajax(apiUrl, JSONArray.class, this,
					"getClipListCb");

			break;
		
		default:
			Log.e(LOGTAG, "Unrecognized tabnum: " + mNum);
			break;
		}

		// using application context to avoid the leak
		// see
		// http://stackoverflow.com/questions/18896880/passing-context-to-arrayadapter-inside-fragment-with-setretaininstancetrue-wil
		ClipListAdapter pla = new ClipListAdapter(getActivity()
				.getApplicationContext(), clips, aq, mUuid, isTrial);
		
		setListAdapter(pla);
		getListView().setOnScrollListener(pla);
		

	}

	// @Override
	// public void onListItemClick(ListView l, View v, int position, long id) {
	// Log.i("FragmentList", "Item clicked: " + id);
	// }


	public void getClipListCb(String url, JSONArray jarr, AjaxStatus status) 
	{
		getClipListSorted(url, jarr, status, true);
	}
	

	
	public void getClipListSorted(String url, JSONArray jarr, AjaxStatus status, boolean sorted) 
	{
		if (isAjaxErrorThenReport(status)) {
			aq.id(R.id.emptyMessage).gone();
			return;
		}
			
		Log.v(LOGTAG, "getClipListSorted " + mNum);
		if (jarr != null) {
			// successful ajax call
			Log.i(LOGTAG, "getClipListSorted success: " + jarr.toString());
			// do something with the jsonarray
			try {
				clips.clear();
//				for (int test = 0; test<3; test++) {
				for (int i = 0; i < jarr.length(); i++) {
					JSONObject clipObj = jarr.getJSONObject(i);
					Clip clip = new Clip(clipObj);
//					if (blink.isPublished()) {
//						// manually set userLikes flag
////						story.setUserLikes(userLikes.contains(story.getId()+""));
//						blink.setUnseen(unseenBlinks.contains(story.getId()+""));
						clips.add(clip);
//					}
				}
//				}
				if (sorted) {
					Collections.sort(clips);
				}
				Log.v(LOGTAG, "clips:" + clips.size());

				// refresh the adapter now
				((BaseAdapter) getListAdapter()).notifyDataSetChanged();
			} 
			catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			// ajax error
			apiError(LOGTAG,
//					"getClipListSorted: null Json, could not get blink list for uuid " + mUuid, status, false, Log.ERROR);
			"Error getting clips", status, true, Log.ERROR);
		}
		if (clips.size()<1) {
			aq.id(R.id.emptyMessage).gone();
		}
	}

	private static ControlledClipView currentlyPlayed = null;

	
	// stop currently played video and start new one, setting it as currently played
	// needed to avoid playing two or more videos at once
	// using this as a central point for play event counter
	public void switchCurrentlyPlayedClip(ControlledClipView v)
	{
		if (currentlyPlayed!=null && currentlyPlayed.isPlaying()) {
			currentlyPlayed.stopPlayback();
			currentlyPlayed.markPlayable(true);
		}
		currentlyPlayed = v;
		
		// hide "newness" indicator
		v.markSeen();
		unseenClips.remove(v.getClipId()+"");
		
		// fire an event about new video start
		// TODO do we track in trial?
//		String apiUrl = PrefUtility.getApiUrl()+"addView?story="+ v.getBlinkId() +"&uuid=" + v.getUuid();
//		aq.ajax(apiUrl, JSONObject.class, this, "addViewCb");
	}
	
	public void addViewCb(String url, JSONObject json, AjaxStatus status){
		// ignore
	}

	boolean isTabVisible = false;
	
	// this method is called when item is made visible
	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) 
	{
	    super.setUserVisibleHint(isVisibleToUser);

	    // Make sure that we are currently visible
	    if (this.isVisible()) {
	        // If we are becoming invisible, then...
	        if (!isVisibleToUser) {
	        	isTabVisible = false;
	            // TODO stop playback
	            if (currentlyPlayed!=null) {
	            	currentlyPlayed.queueStopVideo();
	            	currentlyPlayed = null;
	            }
//	            lastTracked = -1;
	            ((ClipListAdapter)getListAdapter()).resetAutoplayTracker();
	        }
	        else {
	        	isTabVisible = true;
	        }
	    }
	    isTabVisible = isVisibleToUser;
	    Log.d(LOGTAG, mNum +" set to visible "+isTabVisible);
	}
	
	public class ClipListAdapter extends ArrayAdapter<Clip> implements OnScrollListener {

		private final Context context;
		private final ArrayList<Clip> clips;
		private final PQuery aq;
		private final String uuid;
		private int screenWidth, screenHeight, calculcatedVideoWidth;

		private int autoRangeTop;
		private int autoRangeBottom;
		private boolean isTrial;
		
		

		public ClipListAdapter(Context context, ArrayList<Clip> clips,
				PQuery aq, String uuid, boolean trial) {

			super(context, R.layout.tab_cliplist_item, clips);

			this.context = context;
			this.clips = clips;
			this.aq = aq;
			this.uuid = uuid;
			this.isTrial = trial;
			
//			screenWidth = getActivity().getWindowManager().getDefaultDisplay()
//					.getWidth();
			DisplayMetrics metrics = getActivity().getResources().getDisplayMetrics();
			screenWidth = metrics.widthPixels;
			screenHeight = metrics.heightPixels;
			calculcatedVideoWidth = (screenWidth*PERCENT_SQUARE)/100;
			
			// calculate active range
			autoRangeTop = Math.round(AUTO_RANGE_TOP*screenHeight);
			autoRangeBottom = Math.round(AUTO_RANGE_BOTTOM*screenHeight);
			
			Log.d(LOGTAG, "display metrix: "+screenWidth+" x "+screenHeight+", autoRange: "+autoRangeTop+" - "+autoRangeBottom);

		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
//			Log.v(LOGTAG, "getView "+position);

			// 1. Create inflater
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			// 2. Get rowView from inflater
			ClipListItemView rowView = (ClipListItemView)inflater.inflate(R.layout.tab_cliplist_item, parent, false);

			// 3. Get the views from the rowView
			ImageView clipThumb = (ImageView) rowView.findViewById(R.id.videoThumb);
			ImageView playControl = (ImageView) rowView.findViewById(R.id.playControl);
			ControlledClipView videoView = (ControlledClipView) rowView.findViewById(R.id.videoPlayerView);
			ProgressBar progressBar = (ProgressBar) rowView.findViewById(R.id.progress);
			View unseenIndicator = rowView.findViewById(R.id.unseenIndicator);
			ImageButton replyButton = (ImageButton)rowView.findViewById(R.id.replyButton);

			// 4. set data & callbacks
			Clip clip = clips.get(position);
			rowView.initAndLoadCast(clip, aq, ArrayClipsFragment.this);

			// TODO:
			aq.id(clipThumb).image(PrefUtility.getApiUrl() + "clipThumb?clip=" + clip.getId());
			
			ViewUtility.setViewSquare(clipThumb, calculcatedVideoWidth);
			ViewUtility.setViewSquare(playControl, calculcatedVideoWidth);
			
			videoView.init(ArrayClipsFragment.this, clipThumb, calculcatedVideoWidth, position, clip.getId(), mUuid, progressBar, unseenIndicator, playControl);
			clipThumb.setOnClickListener(new ThumbClickListener(videoView, clip.getId()));
			replyButton.setOnClickListener(new ReplyClickListener(clip.getId(), context));
			
			// set current user-story like state
//			story.setUserLikes( userLikes.contains(story.getId()+"") );
//			if (story.isUserLikes()) {
//				likeControl.setImageResource(R.drawable.ic_star_on);
//			}
//			else {
//				likeControl.setImageResource(R.drawable.ic_star_off);
//			}
			
//			// disable liking in trial
////			if (!isTrial) {
//				likeControl.setOnClickListener(new LikeClickListener(likeControl,
//						likesNum, uuid, story, isTrial));
////			}
				
			// 5. return rowView
			return rowView;
		}

		// play/stop click listener
		class ThumbClickListener implements ImageView.OnClickListener {
			ControlledClipView pv;
			long clipId;
			public ThumbClickListener(ControlledClipView v, long clipId){
				this.pv = v;
				this.clipId = clipId;
			}

			@Override
			public void onClick(View v) {
				fireGAnalyticsEvent("ui_action", "touch", "clipThumb", null);
				pv.startVideoPreloading(true);
			}
		}
		
		// reply listener
		class ReplyClickListener implements ImageButton.OnClickListener {
			long clipId;
			Context ctx;
			
			public ReplyClickListener(long clipId, Context ctx){
				this.clipId = clipId;
				this.ctx = ctx;
			}
			
			@Override
			public void onClick(View v) {
				fireGAnalyticsEvent("ui_action", "touch", "replyButton", null);
				Intent intent = new Intent(ctx, VideoCaptureActivity.class);
				intent.putExtra("RESPOND_TO_CLIP", clipId);
				startActivity(intent);
			}
		}
		
		// --------- HELPERS & CALLBACKS
		
		public void onCastClickedCb()
		{
			fireGAnalyticsEvent("ui_action", "touch", "cast", null);
		}


		
		// TODO: improve to show like 1.5m
		private String shortLikesString(Integer num) {
			if (num == null || num < 1)
				return "0";
			if (num >= 1000000)
				return num / 1000000 + "m";
			if (num >= 1000)
				return num / 1000 + "k";
			return num + "";
		}
		
		private int lastTrackedPos=-1;
		
		public void resetAutoplayTracker() {
			lastTrackedPos=-1;
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			
			boolean stopCurrent = false;
			if (currentlyPlayed!=null) {
				// stop previously played roll when it is scrolled out of active range
				int[] location = new int[2];
				currentlyPlayed.getLocationOnScreen(location);
				int viewCenterY = location[1] + calculcatedVideoWidth/2;
				if (viewCenterY<=autoRangeTop || viewCenterY>=autoRangeBottom) 
				{
//					Log.v(LOGTAG, "onScroll: current video exits active range");
					// gotcha! stop it
					currentlyPlayed.queueStopVideo();
					currentlyPlayed = null;
				}
				
			}
					
			// only autostart if the tab was freshly switched, or shown for the first time
			
			if (lastTrackedPos==-1 && visibleItemCount>0) {
				// TODO: if it's currently selected fragment, autoplay, if not, schedule
				
				ClipListItemView pv = (ClipListItemView) ((ViewGroup)view).getChildAt(0);
				Log.v(LOGTAG, "onScroll: pv!=null, visible " + (pv!=null) + " " + isTabVisible);
				if (pv!=null && isTabVisible) 
				{
					Log.v(LOGTAG, "onScroll: starting freshly shown tab's first video");
					
					lastTrackedPos = firstVisibleItem;
					if (currentlyPlayed!=null) {
						currentlyPlayed.queueStopVideo();
						currentlyPlayed = null;
					}
					ControlledClipView videoView = (ControlledClipView) pv.findViewById(R.id.videoPlayerView);
					handleAutostart(pv, videoView);
				}
			}
		}


		
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// autostart here - when the scoll event is over (state idle)
			if (OnScrollListener.SCROLL_STATE_IDLE == scrollState)
			{
				int first = view.getFirstVisiblePosition();
				int last = view.getLastVisiblePosition();
				Log.v(LOGTAG, "onScrollStateChanged: first " +first+" last "+last + " lastTrackedPos "+lastTrackedPos);

				// find the first VideoView that falls in the "active" range
				boolean found = false;
				for (int current=first, i=0; current<=last && !found; current++, i++) 
				{
					ClipListItemView pv = (ClipListItemView) ((ViewGroup)view).getChildAt(i);
					ControlledClipView videoView = (ControlledClipView) pv.findViewById(R.id.videoPlayerView);
					int[] location = new int[2];
					videoView.getLocationOnScreen(location);
					int viewCenterY = location[1] + calculcatedVideoWidth/2;
					Log.d(LOGTAG, "onScrollStateChanged: "+current+" videoView's center y location: "+viewCenterY);
					
					if (viewCenterY>autoRangeTop && viewCenterY<autoRangeBottom) {
						// that's the one
						found = true;
						if (lastTrackedPos!=current) {
							Log.d(LOGTAG, "onScrollStateChanged: new roll in active range: "+current);
							lastTrackedPos = current;
							handleAutostart(pv, videoView);
						}
					}
	
					
				}
			}
		}
		
		// autostart first visible video, video/network settings permitting
		private void handleAutostart(ClipListItemView pv, ControlledClipView videoView) {


			Log.v(LOGTAG, "handleAutostart: first visible item's position in list: "+videoView.getItemPosition());
			AutostartMode am = PrefUtility.getAutostartMode();
			Log.v(LOGTAG, "autostartMode = "+am.toString());
			
			boolean autoStart = false;
			switch (am) {
			case ALWAYS:
				autoStart = true;
				break;
			case WIFI:
				autoStart = NetworkUtility.isWifiConnected(getActivity().getApplicationContext());
				Log.v(LOGTAG, "wificonnected: "+autoStart);
				break;
			default:
				break;
			}
			if (autoStart) {
				videoView.startVideoPreloading(true);
			}
			
			fireGAnalyticsEvent("handle_autostart", "mode", am.toString(), null);
			fireGAnalyticsEvent("handle_autostart", "autoStart", autoStart+"", null);

		}
		

	
	}
	
	// error reporter wrappers
	protected boolean isAjaxErrorThenReport(AjaxStatus status) {
		return ErrorUtility.isAjaxErrorThenReport(LOGTAG, status, getActivity());
	}
	
	protected void apiError(String logtag, String s, AjaxStatus status, boolean toast, int logLevel) {
		ErrorUtility.apiError(logtag, s, status, getActivity(), toast, logLevel);
	}
	
    protected void fireGAnalyticsEvent(String category, String action, String label, Long value) {
    	EasyTracker.getInstance(getActivity()).send(MapBuilder
			    .createEvent(category, action, label, value)
			    .build()
			);
    }
    
    // unseen stories
    public static void resetUnseenClipSet(int[] stories) {
    	if (unseenClips == null) {
    		unseenClips = new HashSet<String>();
    	}
    	else {
    		unseenClips.clear();
    	}
    	if (stories!=null) {
    		for (int i : stories) unseenClips.add(i+"");
    	}
    }
    
    // TODO deduplicate code (TabbedPlaylistActivity)
    private void updateUnseenStoriesFromServer() {
    	Log.v(LOGTAG, "updateUnseenStoriesFromServer");
    	aq.ajax(PrefUtility.getApiUrl()+"unseenStories?uuid=" + mUuid, JSONArray.class, this, "unseenStoriesCb");
	}

	public void unseenStoriesCb(String url, JSONArray jarr, AjaxStatus status) 
	{
		Log.v(LOGTAG, "unseenStoriesCb");
		if (jarr != null) {
			// successful ajax call
			try {
				resetUnseenClipSet(null);
				for (int i = 0; i < jarr.length(); i++) {
					unseenClips.add( jarr.getInt(i)+"" );
				}
				Log.v(LOGTAG, "unseen clips:" + jarr.length()+", set: "+unseenClips.size());
//				refreshUnseenBadge(unseenStories);
			} catch (JSONException e) {
				Log.e(LOGTAG, "jsonexception", e);
			}

		} else {
			// ajax error
			apiError(LOGTAG,
					"userLikesCb: null Json, could not get unseenStories for uuid " + mUuid, status, false, Log.ERROR);
		}
	}



}