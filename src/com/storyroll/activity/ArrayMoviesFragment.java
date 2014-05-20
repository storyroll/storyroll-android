package com.storyroll.activity;

import java.util.ArrayList;
import java.util.Calendar;
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
import android.text.format.DateUtils;
import android.text.format.Time;
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
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.storyroll.PQuery;
import com.storyroll.R;
import com.storyroll.enums.AutostartMode;
import com.storyroll.model.Movie;
import com.storyroll.ui.MovieItemView;
import com.storyroll.ui.ControlledMovieView;
import com.storyroll.ui.PlaylistItemView;
import com.storyroll.ui.RoundedImageView;
import com.storyroll.util.ErrorUtility;
import com.storyroll.util.NetworkUtility;
import com.storyroll.util.PrefUtility;
import com.storyroll.util.ServerUtility;
import com.storyroll.util.ViewUtility;

public class ArrayMoviesFragment extends ListFragment {
	static final String LOGTAG = "ArrayMoviesFragment";

	public static final String[] TAB_HEADINGS_TRIAL = new String[] { "StoryRoll_", null };
	
	static final Integer LIMIT_ITEMS = 40;
	
	// how many percents of width should a squared video take
	private static final int PERCENT_SQUARE = 95;

	// the range of display which acts as an "autostart" area for videos
	// when the center of VideoView enters this range, it is autostarted
	// TODO: must be calculated as a function of display w, h and PERCENT_SQUARE
	private static final float AUTO_RANGE_TOP = 0.33f;
	private static final float AUTO_RANGE_BOTTOM = 0.75f;


	private int mNum;
	private String mUuid;
	private long mChanId;

	private boolean isTrial;
	private PQuery aq;
	
	private ArrayList<Movie> movies = new ArrayList<Movie>();
	public static Set<String> userLikes = null;
	public static Set<String> unseenMovies = null;
	private Calendar c = Calendar.getInstance(); 
	

	/**
	 * Create a new instance of CountingFragment, providing "num" as an
	 * argument.
	 * @param chanId TODO
	 */
	static ArrayMoviesFragment newInstance(int num, long chanId, String uuid, boolean isTrial) {
		ArrayMoviesFragment f = new ArrayMoviesFragment();

		// Supply num input as an argument.
		Bundle args = new Bundle();
		args.putInt("num", num);
		args.putLong("channelId", chanId);
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
		
		mNum = getArguments() != null ? getArguments().getInt("num") : 0;
		mChanId = getArguments() != null ? getArguments().getLong("channelId") : -1;
		mUuid = getArguments() != null ? getArguments().getString("uuid") : "";
		isTrial = getArguments() != null ? getArguments().getBoolean("trial") : false;
		
		aq = ((TabbedChannelsActivity) getActivity()).getPQuery();
		
		// get user likes only once
		if (isTrial) {
			userLikes = new HashSet<String>();
		}
		else if (userLikes==null || userLikes.isEmpty()) 
		{
			if (userLikes == null) {
				userLikes = new HashSet<String>();
			}
			String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_STORIES_LIKED_BY_USER, "uuid=" + mUuid + "&limit=" + LIMIT_ITEMS);
			aq.progress(R.id.progress).ajax(apiUrl, JSONArray.class, this, "userLikesIdsCb");
		}
		// get unseen only once
		if (isTrial) {
			unseenMovies = new HashSet<String>();
		}
		else if (unseenMovies==null || unseenMovies.isEmpty()) 
		{
			if (unseenMovies == null) {
				unseenMovies = new HashSet<String>();
			}
			updateUnseenMoviesFromServer();
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

		String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_CHAN_MOVIES,
				"uuid=" + mUuid +"&channel="+mChanId + "&limit=" + LIMIT_ITEMS);
		
		aq.progress(R.id.progress).ajax(apiUrl, JSONArray.class, this, "getMovieListCb");
		
//		default:
//			Log.e(LOGTAG, "Unrecognized tabnum: " + mNum);
//			break;

		// using application context to avoid the leak
		// see
		// http://stackoverflow.com/questions/18896880/passing-context-to-arrayadapter-inside-fragment-with-setretaininstancetrue-wil
		MovieListAdapter pla = new MovieListAdapter(getActivity()
				.getApplicationContext(), movies, aq, mUuid, isTrial);
		
		setListAdapter(pla);
		getListView().setOnScrollListener(pla);
		

	}

	// @Override
	// public void onListItemClick(ListView l, View v, int position, long id) {
	// Log.i("FragmentList", "Item clicked: " + id);
	// }

	// inner callbacks
    // get initial, centralized (static) user "liked" video ids
	public void userLikesIdsCb(String url, JSONArray jarr, AjaxStatus status) 
	{
		Log.v(LOGTAG, "userLikesIdsCb");
		if (isAjaxErrorThenReport(status)) {
			aq.id(R.id.emptyMessage).gone();
			return;
		}
		
		if (jarr != null) {
			// successful ajax call
			try {
				
				userLikes.clear();
				for (int i = 0; i < jarr.length(); i++) {
					JSONObject likeObj = jarr.getJSONObject(i);
					JSONObject videoObj = (JSONObject) likeObj.get("movie"); // TODO
					userLikes.add(videoObj.getLong("id")+"");
				}
				Log.v(LOGTAG, "user liked videos:" + userLikes.size()+" "+userLikes.toString());
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

	public void getMovieListCb(String url, JSONArray jarr, AjaxStatus status) 
	{
		getMovieListSorted(url, jarr, status, true);
	}

	public void updateMovieListCb(String url, JSONArray jarr, AjaxStatus status) 
	{
		refreshMovieListSorted(url, jarr, status, true);
	}

	public void refreshMovieListSorted(String url, JSONArray jarr, AjaxStatus status, boolean sorted) 
	{
		if (isAjaxErrorThenReport(status)) {
			aq.id(R.id.emptyMessage).gone();
			return;
		}
			
		Log.v(LOGTAG, "mergeMovieListSorted for tab no " + mNum);
		if (jarr != null) {
			// successful ajax call
			Log.i(LOGTAG, "mergeMovieListSorted success: " + jarr.length());
			// do something with the jsonarray
			try {
				ArrayAdapter<Movie> aa = (ArrayAdapter<Movie>)getListAdapter();
				int updates = 0;
				for (int i = 0; i < jarr.length(); i++) {
					JSONObject movieObj = jarr.getJSONObject(i);
					Movie movie = new Movie(movieObj);
					
					// movie with this id exists?
					int foundIdx = getMovieIndexById(movie.getId());
					
					
					if (foundIdx!=-1 && movies.size()>0 && movies.get(foundIdx).getPublishedOn() != movie.getPublishedOn()) 
					{
						// movie updated, remove and insert
						aa.remove(movies.get(foundIdx));
						movies.remove(foundIdx);
						
						// manually set userLikes flag
						movie.setUserLikes(userLikes.contains(movie.getId()+""));
						movie.setUnseen(unseenMovies.contains(movie.getId()+""));
						
						aa.insert(movie, 0);
						movies.add(movie);
						updates++;
					}
				}
				Log.v(LOGTAG, "movies:" + movies.size()+ ", ArrayAdapter: "+aa.getCount()+", updated items: "+updates);

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
//					"getMovieListSorted: null Json, could not get blink list for uuid " + mUuid, status, false, Log.ERROR);
			"Error getting movies", status, true, Log.ERROR);
		}
		if (movies.size()<1) {
			aq.id(R.id.emptyMessage).gone();
		}
	}
	
	private int getMovieIndexById(long id) {
	    for(int i = 0; i < movies.size(); ++i) {
	        if(movies.get(i).getId() == id) return i;
	    }
	    return -1;
	}
	
	public void getMovieListSorted(String url, JSONArray jarr, AjaxStatus status, boolean sorted) 
	{
		if (isAjaxErrorThenReport(status)) {
			aq.id(R.id.emptyMessage).gone();
			return;
		}
			
		Log.v(LOGTAG, "getMovieListSorted for tab no " + mNum);
		if (jarr != null) {
			// successful ajax call
			Log.i(LOGTAG, "getMovieListSorted success: " + jarr.length());
			// do something with the jsonarray
			try {
				movies.clear();
				for (int i = 0; i < jarr.length(); i++) {
					JSONObject movieObj = jarr.getJSONObject(i);
					Movie movie = new Movie(movieObj);
//					if (clip.isPublished()) {
						// manually set userLikes flag
						movie.setUserLikes(userLikes.contains(movie.getId()+""));
						movie.setUnseen(unseenMovies.contains(movie.getId()+""));
						movies.add(movie);
//				}
				}
				if (sorted) {
					Collections.sort(movies);
				}
				Log.v(LOGTAG, "movies:" + movies.size());

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
//					"getMovieListSorted: null Json, could not get blink list for uuid " + mUuid, status, false, Log.ERROR);
			"Error getting movies", status, true, Log.ERROR);
		}
		if (movies.size()<1) {
			aq.id(R.id.emptyMessage).gone();
		}
	}

	private static ControlledMovieView currentlyPlayed = null;

	
	// stop currently played video and start new one, setting it as currently played
	// needed to avoid playing two or more videos at once
	// using this as a central point for play event counter
	public void switchCurrentlyPlayedMovie(ControlledMovieView v)
	{
		if (currentlyPlayed!=null && currentlyPlayed.isPlaying()) {
			currentlyPlayed.stopPlayback();
			currentlyPlayed.markPlayable(true);
		}
		currentlyPlayed = v;
		
		// hide "newness" indicator
		v.markSeen();
		unseenMovies.remove(v.getMovieId()+"");
		
		// fire an event about new video start
		// TODO do we track in trial?
		String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_VIEW_ADD, "story="+ v.getMovieId() +"&uuid=" + v.getUuid());
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
	            ((MovieListAdapter)getListAdapter()).resetAutoplayTracker();
	        }
	        else {
	        	isTabVisible = true;
	        }
	    }
	    isTabVisible = isVisibleToUser;
	    Log.d(LOGTAG, "Tab num "+mNum +" set to visible "+isTabVisible);
	}
	
	public class MovieListAdapter extends ArrayAdapter<Movie> implements OnScrollListener {

		private static final boolean HIDE_AGE_AGO_POSTFIX = true;
		private final Context context;
		private final ArrayList<Movie> movies;
		private final PQuery aq;
		private final String uuid;
		private int screenWidth, screenHeight, calculcatedVideoWidth;

		private int autoRangeTop;
		private int autoRangeBottom;
		private boolean isTrial;
		
		

		public MovieListAdapter(Context context, ArrayList<Movie> movies,
				PQuery aq, String uuid, boolean trial) {

			super(context, R.layout.tab_movie_item, movies);

			this.context = context;
			this.movies = movies;
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
			MovieItemView rowView = (MovieItemView)inflater.inflate(R.layout.tab_movie_item, parent, false);

			// 3. Get the views from the rowView
			ImageView videoThumb = (ImageView) rowView.findViewById(R.id.videoThumb);
			ImageView playControl = (ImageView) rowView.findViewById(R.id.playControl);
			TextView likesNum = (TextView) rowView.findViewById(R.id.numLikes);
			ImageView likeControl = (ImageView) rowView.findViewById(R.id.likeImage);
			ControlledMovieView videoView = (ControlledMovieView) rowView.findViewById(R.id.videoPlayerView);
			ProgressBar progressBar = (ProgressBar) rowView.findViewById(R.id.progress);
			View unseenIndicator = rowView.findViewById(R.id.unseenIndicator);
			ImageButton replyButton = (ImageButton)rowView.findViewById(R.id.replyButton);

			// 4. set data & callbacks
//			Movie movie = movies.get(position);
			Movie movie = (Movie)getListAdapter().getItem(position);
			rowView.initAndLoadCast(movie, aq, ArrayMoviesFragment.this);

			// TODO:
			aq.id(videoThumb).image(movie.getThumbUrl());
			
			ViewUtility.setViewSquare(videoThumb, calculcatedVideoWidth);
			ViewUtility.setViewSquare(playControl, calculcatedVideoWidth);
			
			videoView.init(ArrayMoviesFragment.this, videoThumb, calculcatedVideoWidth, position, 
					movie.getId(), movie.getLastClipId(), mUuid, progressBar, unseenIndicator, playControl, movie.getUrl());
			videoThumb.setOnClickListener(new ThumbClickListener(videoView, movie.getId()));
			replyButton.setOnClickListener(new ReplyClickListener(movie, context));
			
			String ageText = DateUtils.getRelativeTimeSpanString(
					movie.getPublishedOn(), c.getTimeInMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString();
			if (HIDE_AGE_AGO_POSTFIX) {
				ageText = ageText.replace(" ago", "");
			}
			aq.id(rowView.findViewById(R.id.ageText)).text(ageText);
			likesNum.setText(shortLikesString(movie.getLikes()));
			
			if (movie.getCast()!=null) {
				for (int i=0; i<movie.getCast().length && i<PlaylistItemView.castIds.length; i++) {
					RoundedImageView castImage = (RoundedImageView) 
							rowView.findViewById(PlaylistItemView.castIds[i]);
					aq.id(castImage).image(PrefUtility.getApiUrl(
							ServerUtility.API_AVATAR, "uuid="+movie.getCast()[i]), 
							true, false, 0, R.drawable.ic_avatar_default);
					aq.id(castImage).clicked(this, "onCastClickedCb");
				}
			}
			
			// set current user-video like state
			movie.setUserLikes( userLikes.contains(movie.getId()+"") );
			if (movie.isUserLikes()) {
				likeControl.setImageResource(R.drawable.ic_star_on);
			}
			else {
				likeControl.setImageResource(R.drawable.ic_star_off);
			}
			
			// disable liking in trial
//			if (!isTrial) {
				likeControl.setOnClickListener(new LikeClickListener(likeControl,
						likesNum, uuid, movie, isTrial));
//			}

				
			// 5. return rowView
			return rowView;
		}

		// play/stop click listener
		class ThumbClickListener implements ImageView.OnClickListener {
			ControlledMovieView pv;
//			long movieId;
			public ThumbClickListener(ControlledMovieView v, long movieId){
				this.pv = v;
//				this.movieId = movieId;
			}

			@Override
			public void onClick(View v) {
				fireGAnalyticsEvent("ui_action", "touch", "movieThumb", null);
				pv.startVideoPreloading(true);
			}
		}
		
		// reply listener
		class ReplyClickListener implements ImageButton.OnClickListener {
			Movie movie;
			Context ctx;
			
			public ReplyClickListener(Movie movie, Context ctx){
				this.movie = movie;
				this.ctx = ctx;
			}
			
			@Override
			public void onClick(View v) {
				fireGAnalyticsEvent("ui_action", "touch", "replyButton", null);
				Intent intent = new Intent(ctx, VideoCaptureActivity.class);
				intent.putExtra("RESPOND_TO_CLIP", movie.getLastClipId());
				intent.putExtra("CURRENT_CHANNEL", mChanId);
				intent.putExtra("MOVIE", movie.getId());
//				Log.v(LOGTAG, "movie.getLastUserId()="+movie.getLastUserId());
				intent.putExtra("LAST_USER", movie.getLastUserId());
				startActivity(intent);
			}
		}
		
		// like click listener
		class LikeClickListener implements ImageView.OnClickListener {
			String uuid;
			Movie movie;
			ImageView view;
			TextView likesNum;
			boolean trial;
			int count;

			public LikeClickListener(ImageView view, TextView likesNum,
					String uuid, Movie movie, boolean trial) 
			{
				this.uuid = uuid;
				this.movie = movie;
				this.view = view;
				this.likesNum = likesNum;
				this.trial = trial;
				this.count=0;
			}

			@Override
			public void onClick(View v) {
				fireGAnalyticsEvent("ui_action", "touch", "likeButton", null);
				
				if (trial) {
					count++;
					if (count==1){
						Toast.makeText(context, "Can like after logging in.", Toast.LENGTH_SHORT).show();
					}
					else {
						Toast.makeText(context, "Login already!", Toast.LENGTH_SHORT).show();
						count=0;
					}
					return;
				}
				// first, invert the likes
				movie.setUserLikes(!movie.isUserLikes());

				if (movie.isUserLikes()) {
					movie.setLikes(movie.getLikes() + 1);
					view.setImageResource(R.drawable.ic_star_on);
					likesNum.setText(shortLikesString(movie.getLikes()));
					userLikes.add(movie.getId()+"");
				} else {
					movie.setLikes(movie.getLikes() - 1);
					view.setImageResource(R.drawable.ic_star_off);
					likesNum.setText(shortLikesString(movie.getLikes()));
					userLikes.remove(movie.getId()+"");
				}

				if (!trial) 
				{
					String url = PrefUtility.getApiUrl(ServerUtility.API_STORY_LIKE, "uuid=" + this.uuid + "&story=" + movie.getId());
					
					if (!movie.isUserLikes()) 
					{
						url = PrefUtility.getApiUrl(ServerUtility.API_STORY_DISLIKE, "uuid=" + this.uuid + "&story=" + movie.getId());
					}
					
	
					aq.ajax(url, JSONObject.class, new AjaxCallback<JSONObject>() {
						@Override
						public void callback(String url, JSONObject json,
								AjaxStatus status) {
							if (json != null) {
								// successful ajax call
								Log.v(LOGTAG, "likeCb success: " + json.toString());
								// TODO: update like icon, and increase the count.
								// TODO: collateral?
							} else {
								// ajax error, o change
								apiError(LOGTAG,
										"likeCb: Json null " + mUuid, status, false, Log.ERROR);
							}
						}
					});
				}
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
				
				MovieItemView pv = (MovieItemView) ((ViewGroup)view).getChildAt(0);
				Log.v(LOGTAG, "onScroll: pv!=null, visible " + (pv!=null) + " " + isTabVisible);
				if (pv!=null && isTabVisible) 
				{
					Log.v(LOGTAG, "onScroll: starting freshly shown tab's first video");
					
					lastTrackedPos = firstVisibleItem;
					if (currentlyPlayed!=null) {
						currentlyPlayed.queueStopVideo();
						currentlyPlayed = null;
					}
					ControlledMovieView videoView = (ControlledMovieView) pv.findViewById(R.id.videoPlayerView);
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
					MovieItemView pv = (MovieItemView) ((ViewGroup)view).getChildAt(i);
					ControlledMovieView videoView = (ControlledMovieView) pv.findViewById(R.id.videoPlayerView);
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
		private void handleAutostart(MovieItemView pv, ControlledMovieView videoView) {


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
    public static void resetUnseenMovieSet(int[] stories) {
    	if (unseenMovies == null) {
    		unseenMovies = new HashSet<String>();
    	}
    	else {
    		unseenMovies.clear();
    	}
    	if (stories!=null) {
    		for (int i : stories) unseenMovies.add(i+"");
    	}
    }
    
    // TODO deduplicate code (TabbedPlaylistActivity)
    private void updateUnseenMoviesFromServer() {
    	Log.v(LOGTAG, "updateUnseenMoviesFromServer");
    	// TODO
//    	aq.ajax(PrefUtility.getApiUrl(ServerUtility.API_UNSEEN_MOVIES, "uuid=" + mUuid), 
//    			JSONArray.class, this, "unseenMoviesCb");
	}

	public void unseenMoviesCb(String url, JSONArray jarr, AjaxStatus status) 
	{
		Log.v(LOGTAG, "unseenMoviesCb");
		if (jarr != null) {
			// successful ajax call
			try {
				resetUnseenMovieSet(null);
				for (int i = 0; i < jarr.length(); i++) {
					unseenMovies.add( jarr.getInt(i)+"" );
				}
				Log.v(LOGTAG, "unseen movie:" + jarr.length()+", set: "+unseenMovies.size());
//				refreshUnseenBadge(unseenStories);
			} catch (JSONException e) {
				Log.e(LOGTAG, "jsonexception", e);
			}

		} else {
			// ajax error
			apiError(LOGTAG,
					"userLikesCb: null Json, could not get unseenMovies for uuid " + mUuid, status, false, Log.ERROR);
		}
	}

	public void updateMovieList() {
		Log.v(LOGTAG, "updateMovieList on chan: "+mChanId);
		String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_CHAN_MOVIES,
				"uuid=" + mUuid +"&channel="+mChanId + "&limit=" + LIMIT_ITEMS);
		
		aq.progress(R.id.progress).ajax(apiUrl, JSONArray.class, this, "updateMovieListCb");
		
	}

	public void postSelectItem(final int pos) {
		Log.v(LOGTAG, "postSelectItem: "+pos);
//		getListView().setSelection(pos);
		getListView().post(new Runnable() {
	        @Override
	        public void run() {
	            getListView().setSelection(pos);
	        }
	    });
	}


}