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
import com.storyroll.model.Story;
import com.storyroll.ui.ControlledVideoView;
import com.storyroll.ui.PlaylistItemView;
import com.storyroll.ui.RoundedImageView;
import com.storyroll.util.AppUtility;
import com.storyroll.util.ErrorUtility;
import com.storyroll.util.NetworkUtility;
import com.storyroll.util.PrefUtility;
import com.storyroll.util.ViewUtility;

public class ArrayListFragment extends ListFragment {
	private static final String LOGTAG = "ArrayListFragment";

//	private static final int TAB_BEST = 0;
//	private static final int TAB_NEW = 1;
//	public static final int TAB_MINE = 2;
//	private static final int TAB_FAVORITE = 3;
//	public static final String[] TAB_HEADINGS = new String[] { "Best", "New", "Mine", "Likes" };
//	public static final String[] TAB_HEADINGS_TRIAL = new String[] { "Best", "New", null, null };

	private static final int TAB_BEST = 0;
	public static final int TAB_MINE = 1;
	private static final int TAB_NEW = 2;
	private static final int TAB_FAVORITE = 3;
	public static final String[] TAB_HEADINGS = new String[] { "Best Rolls", "My Rolls", null, null };
	public static final String[] TAB_HEADINGS_TRIAL = new String[] { "Best Rolls", null, null, null };
	
	private static final Integer LIMIT_ITEMS = 25;
	
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
	
	private ArrayList<Story> stories = new ArrayList<Story>();
//	private Map<Long, Story> idToStory = new HashMap<Long, Story>();
	public static Set<String> userLikes = null;
	public static Set<String> unseenStories = null;
    

	/**
	 * Create a new instance of CountingFragment, providing "num" as an
	 * argument.
	 */
	static ArrayListFragment newInstance(int num, String uuid, boolean isTrial) {
		ArrayListFragment f = new ArrayListFragment();

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
		
		aq = ((TabbedPlaylistActivity) getActivity()).getPQuery();
		
		// get user likes only once
		if (isTrial) {
			userLikes = new HashSet<String>();
		}
		else if (userLikes==null || userLikes.isEmpty()) 
		{
			if (userLikes == null) {
				userLikes = new HashSet<String>();
			}
			String apiUrl = PrefUtility.getApiUrl()+"userLikes?uuid=" + mUuid + "&limit=" + LIMIT_ITEMS;
			aq.progress(R.id.progress).ajax(apiUrl, JSONArray.class, this, "userLikesIdsCb");
		}
		// get unseen only once
		if (isTrial) {
			unseenStories = new HashSet<String>();
		}
		else if (unseenStories==null || unseenStories.isEmpty()) 
		{
			if (unseenStories == null) {
				unseenStories = new HashSet<String>();
			}
			updateUnseenStoriesFromServer();
		}

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
		case TAB_BEST:
			apiUrl += "getTopLikedStories?limit=" + LIMIT_ITEMS;
			aq.progress(R.id.progress).ajax(apiUrl, JSONArray.class, this,
					"getStoryListCb");

			break;
		case TAB_FAVORITE:
			apiUrl += "userLikes?uuid=" + mUuid + "&limit=" + LIMIT_ITEMS;
			aq.progress(R.id.progress).ajax(apiUrl, JSONArray.class, this,
					"userLikesCb");

			break;
		case TAB_MINE:
			apiUrl += "getUserPublishedStories?uuid=" + mUuid + "&limit="
					+ LIMIT_ITEMS;
			aq.progress(R.id.progress).ajax(apiUrl, JSONArray.class, this,
					"getStoryListCb");

			break;
		case TAB_NEW:
			apiUrl += "getLatestPublishedStories?limit=" + LIMIT_ITEMS;
			aq.progress(R.id.progress).ajax(apiUrl, JSONArray.class, this,
					"getStoryListCb");

			break;
		default:
			Log.e(LOGTAG, "Unrecognized tabnum: " + mNum);
			break;
		}

		// using application context to avoid the leak
		// see
		// http://stackoverflow.com/questions/18896880/passing-context-to-arrayadapter-inside-fragment-with-setretaininstancetrue-wil
		PlayListAdapter pla = new PlayListAdapter(getActivity()
				.getApplicationContext(), stories, aq, mUuid, isTrial);
		
		setListAdapter(pla);
		getListView().setOnScrollListener(pla);
		

	}

	// @Override
	// public void onListItemClick(ListView l, View v, int position, long id) {
	// Log.i("FragmentList", "Item clicked: " + id);
	// }

    
	// inner callbacks
    // get initial, centralized (static) user "liked" story ids
	public void userLikesIdsCb(String url, JSONArray jarr, AjaxStatus status) 
	{
		Log.v(LOGTAG, "userLikesIdsCb");
		if (isAjaxErrorThenReport(status)) return;
		
		if (jarr != null) {
			// successful ajax call
			try {
				
				userLikes.clear();
				for (int i = 0; i < jarr.length(); i++) {
					JSONObject likeObj = jarr.getJSONObject(i);
					JSONObject storyObj = (JSONObject) likeObj.get("story");
					userLikes.add(storyObj.getLong("id")+"");
				}
				Log.v(LOGTAG, "user liked stories:" + userLikes.size()+" "+userLikes.toString());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			// ajax error
			apiError(LOGTAG,
					"userLikesCb: null Json, could not get likes for uuid " + mUuid, status, false, Log.ERROR);
		}

	}
	
	public void userLikesCb(String url, JSONArray jarr, AjaxStatus status) {
		Log.v(LOGTAG, "userLikesCb ");
		if (jarr != null) {
			// successful ajax call
			// Log.i(LOGTAG, "userLikesCb success: "+jarr.length());
			// do something with the jsonarray
			try {
				stories.clear();
				userLikes.clear();
				for (int i = 0; i < jarr.length(); i++) {
					JSONObject likeObj = jarr.getJSONObject(i);
					JSONObject storyObj = (JSONObject) likeObj.get("story");
					Story story = new Story(storyObj);
					// all stories in this list are likes
					userLikes.add(story.getId()+"");
					story.setUserLikes(true);
					stories.add(story);
				}
				Collections.sort(stories);
				Log.v(LOGTAG, "stories:" + stories.size());

				// refresh the adapter now
				((BaseAdapter) getListAdapter()).notifyDataSetChanged();

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			// ajax error
			apiError(LOGTAG,
					"Error getting rolls", status, true, Log.ERROR);
		}

	}

	public void getStoryListCb(String url, JSONArray jarr, AjaxStatus status) 
	{
		if (isAjaxErrorThenReport(status)) return;
			
		Log.v(LOGTAG, "getStoryListCb " + mNum);
		if (jarr != null) {
			// successful ajax call
			Log.i(LOGTAG, "getStoryListCb success: " + jarr.toString());
			// do something with the jsonarray
			try {
				stories.clear();
//				for (int test = 0; test<3; test++) {
				for (int i = 0; i < jarr.length(); i++) {
					JSONObject storyObj = jarr.getJSONObject(i);
					Story story = new Story(storyObj);
					if (story.isPublished()) {
						// manually set userLikes flag
						story.setUserLikes(userLikes.contains(story.getId()+""));
						story.setUnseen(unseenStories.contains(story.getId()+""));
						stories.add(story);
					}
				}
//				}
				Collections.sort(stories);
				Log.v(LOGTAG, "stories:" + stories.size());

				// refresh the adapter now
				((BaseAdapter) getListAdapter()).notifyDataSetChanged();

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			// ajax error
			apiError(LOGTAG,
//					"userLikesCb: null Json, could not get story list for uuid " + mUuid, status, false, Log.ERROR);
			"Error getting rolls", status, true, Log.ERROR);
		}

	}

	private static ControlledVideoView currentlyPlayed = null;
	
	// stop currently played video and start new one, setting it as currently played
	// needed to avoid playing two or more videos at once
	// using this as a central point for play event counter
	public void switchCurrentlyPlayed(ControlledVideoView v)
	{
		if (currentlyPlayed!=null && currentlyPlayed.isPlaying()) {
			currentlyPlayed.stopPlayback();
			currentlyPlayed.markPlayable(true);
		}
		currentlyPlayed = v;
		
		// hide "newness" indicator
		v.markSeen();
		unseenStories.remove(v.getStoryId()+"");
		
		// fire an event about new video start
		// TODO do we track in trial?
		String apiUrl = PrefUtility.getApiUrl()+"addView?story="+ v.getStoryId() +"&uuid=" + v.getUuid();
		aq.ajax(apiUrl, JSONObject.class, this, "addViewCb");
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
	            ((PlayListAdapter)getListAdapter()).resetAutoplayTracker();
	        }
	        else {
	        	isTabVisible = true;
	        }
	    }
	    isTabVisible = isVisibleToUser;
	    Log.d(LOGTAG, mNum +" set to visible "+isTabVisible);
	}
	
	public class PlayListAdapter extends ArrayAdapter<Story> implements OnScrollListener {

		private final Context context;
		private final ArrayList<Story> stories;
		private final PQuery aq;
		private final String uuid;
		private int screenWidth, screenHeight, calculcatedVideoWidth;

		private int autoRangeTop;
		private int autoRangeBottom;
		private boolean isTrial;
		
		

		public PlayListAdapter(Context context, ArrayList<Story> stories,
				PQuery aq, String uuid, boolean trial) {

			super(context, R.layout.tab_playlist_item, stories);

			this.context = context;
			this.stories = stories;
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
			PlaylistItemView rowView = (PlaylistItemView)inflater.inflate(R.layout.tab_playlist_item, parent, false);

			// 3. Get the views from the rowView
			ImageView storyThumb = (ImageView) rowView.findViewById(R.id.storyThumb);
			ImageView playControl = (ImageView) rowView.findViewById(R.id.playControl);
			TextView likesNum = (TextView) rowView.findViewById(R.id.numLikes);
			ImageView likeControl = (ImageView) rowView.findViewById(R.id.likeImage);
			ControlledVideoView videoView = (ControlledVideoView) rowView.findViewById(R.id.videoPlayerView);
			ProgressBar progressBar = (ProgressBar) rowView.findViewById(R.id.progress);
			View unseenIndicator = rowView.findViewById(R.id.unseenIndicator);

			// 4. set data & callbacks
			Story story = stories.get(position);
			rowView.initAndLoadCast(story, aq, ArrayListFragment.this);

			aq.id(storyThumb).image(PrefUtility.getApiUrl() + "storyThumb?story=" + story.getId());
			
			ViewUtility.setViewSquare(storyThumb, calculcatedVideoWidth);
			ViewUtility.setViewSquare(playControl, calculcatedVideoWidth);
			
			videoView.init(ArrayListFragment.this, storyThumb, calculcatedVideoWidth, position, story.getId(), mUuid, progressBar, unseenIndicator, playControl);
			storyThumb.setOnClickListener(new ThumbClickListener(videoView, story.getId()));

			likesNum.setText(shortLikesString(story.getLikes()));

			if (story.getCast()!=null) {
				for (int i=0; i<story.getCast().length; i++) {
					RoundedImageView castImage = (RoundedImageView) rowView.findViewById(PlaylistItemView.castIds[i]);
					aq.id(castImage).image(PrefUtility.getApiUrl()+"avatar?uuid="+story.getCast()[i], true, false, 0, R.drawable.ic_avatar_default);
					aq.id(castImage).clicked(this, "onCastClickedCb");
				}
			}
			
			// set current user-story like state
			story.setUserLikes( userLikes.contains(story.getId()+"") );
			if (story.isUserLikes()) {
				likeControl.setImageResource(R.drawable.ic_star_on);
			}
			else {
				likeControl.setImageResource(R.drawable.ic_star_off);
			}
			
			// disable liking in trial
			if (!isTrial) {
				likeControl.setOnClickListener(new LikeClickListener(likeControl,
						likesNum, uuid, story));
			}
				
			// 5. return rowView
			return rowView;
		}

		// play/stop click listener
		class ThumbClickListener implements ImageView.OnClickListener {
			ControlledVideoView pv;
			long storyId;
			public ThumbClickListener(ControlledVideoView v, long storyId){
				this.pv = v;
				this.storyId = storyId;
			}

			@Override
			public void onClick(View v) {
				fireGAnalyticsEvent("ui_action", "touch", "storyThumb", null);
				pv.startVideoPreloading(true);
			}
			
		}
		
		// like click listener
		class LikeClickListener implements ImageView.OnClickListener {
			String uuid;
			Story story;
			ImageView view;
			TextView likesNum;

			public LikeClickListener(ImageView view, TextView likesNum,
					String uuid, Story story) {
				this.uuid = uuid;
				this.story = story;
				this.view = view;
				this.likesNum = likesNum;
			}

			@Override
			public void onClick(View v) {
				fireGAnalyticsEvent("ui_action", "touch", "likeButton", null);

				String url = PrefUtility.getApiUrl() + "like";
				if (story.isUserLikes()) {
					url = PrefUtility.getApiUrl() + "dislike";
				}
				url += "?uuid=" + this.uuid + "&story=" + story.getId();

				aq.ajax(url, JSONObject.class, new AjaxCallback<JSONObject>() {
					@Override
					public void callback(String url, JSONObject json,
							AjaxStatus status) {
						if (json != null) {
							// successful ajax call
							Log.v(LOGTAG, "likeCb success: " + json.toString());
							// update like icon, and increase the count.
							// TODO: collateral?
							story.setUserLikes(!story.isUserLikes());
							
							if (story.isUserLikes()) {
								story.setLikes(story.getLikes() + 1);
								view.setImageResource(R.drawable.ic_star_on);
								likesNum.setText(shortLikesString(story
										.getLikes()));
								userLikes.add(story.getId()+"");
							} else {
								story.setLikes(story.getLikes() - 1);
								view.setImageResource(R.drawable.ic_star_off);
								likesNum.setText(shortLikesString(story
										.getLikes()));
								userLikes.remove(story.getId()+"");

							}
							
						} else {
							// ajax error, o change
							apiError(LOGTAG,
									"likeCb: Json null " + mUuid, status, false, Log.ERROR);
						}
					}
				});
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
				
				PlaylistItemView pv = (PlaylistItemView) ((ViewGroup)view).getChildAt(0);
				Log.v(LOGTAG, "onScroll: pv!=null, visible " + (pv!=null) + " " + isTabVisible);
				if (pv!=null && isTabVisible) 
				{
					Log.v(LOGTAG, "onScroll: starting freshly shown tab's first video");
					
					lastTrackedPos = firstVisibleItem;
					if (currentlyPlayed!=null) {
						currentlyPlayed.queueStopVideo();
						currentlyPlayed = null;
					}
					ControlledVideoView videoView = (ControlledVideoView) pv.findViewById(R.id.videoPlayerView);
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
					PlaylistItemView pv = (PlaylistItemView) ((ViewGroup)view).getChildAt(i);
					ControlledVideoView videoView = (ControlledVideoView) pv.findViewById(R.id.videoPlayerView);
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
		private void handleAutostart(PlaylistItemView pv, ControlledVideoView videoView) {


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
    public static void resetUnseenStoriesSet(int[] stories) {
    	if (unseenStories == null) {
    		unseenStories = new HashSet<String>();
    	}
    	else {
    		unseenStories.clear();
    	}
    	if (stories!=null) {
    		for (int i : stories) unseenStories.add(i+"");
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
				resetUnseenStoriesSet(null);
				for (int i = 0; i < jarr.length(); i++) {
					unseenStories.add( jarr.getInt(i)+"" );
				}
				Log.v(LOGTAG, "unseen stories:" + jarr.length()+", set: "+unseenStories.size());
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