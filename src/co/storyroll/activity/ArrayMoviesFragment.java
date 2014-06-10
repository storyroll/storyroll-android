package co.storyroll.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AbsListView.OnScrollListener;
import co.storyroll.PQuery;
import co.storyroll.R;
import co.storyroll.enums.AutostartMode;
import co.storyroll.model.Movie;
import co.storyroll.ui.*;
import co.storyroll.util.*;
import com.androidquery.auth.BasicHandle;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

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
	private Calendar c = Calendar.getInstance();

	private static int unseenMoviesCount=0;
    private BasicHandle basicHandle;

    /**
	 * Create a new instance of CountingFragment, providing "num" as an
	 * argument.
	 * @param chanId channel id
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
        basicHandle = AppUtility.makeBasicHandle(getActivity());

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
			aq.auth(basicHandle).progress(R.id.progress).ajax(apiUrl, JSONArray.class, this, "userLikesIdsCb");
		}
		// get unseen only once
		if (!isTrial) {
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

        showMainProgress(true);
        if (isTrial) {
            aq.ajax(apiUrl, JSONArray.class, this, "getMovieListCb");
        }
        else {
            aq.auth(basicHandle).ajax(apiUrl, JSONArray.class, this, "getMovieListCb");
        }
		
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        //never gets here :(
        Log.v(LOGTAG, "onActivityResult: "+requestCode+", "+resultCode);
        super.onActivityResult(requestCode, resultCode, data);
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
				Log.v(LOGTAG, "user liked videos:" + userLikes.size()+", "+userLikes.toString());
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
        showMainProgress(false);
		getMovieListSorted(url, jarr, status, true);
	}

	public void updateMovieListCb(String url, JSONArray jarr, AjaxStatus status) 
	{
        showMainProgress(false);
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
						
						aa.insert(movie, 0);
						movies.add(movie);
						updates++;
					}
                    else if (foundIdx==-1) {
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
		unseenMoviesCount--;
		
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
		private ImageButton.OnClickListener onMoviehideClick;

        public String LOGTAG = "ARR_M_FRG.MOVIE_LST_FRG";


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
            onMoviehideClick = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fireGAnalyticsEvent("ui_action", "touch", "action_roll_movie", null);
                    new HideMovieDialog().show(getActivity().getSupportFragmentManager(), "HideMovieDialog");
                }
            };
			
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
            Movie movie = (Movie)getListAdapter().getItem(position);
            aq.id(videoThumb).image(movie.getThumbUrl()); // first things first, this takes time to load

            ImageView playControl = (ImageView) rowView.findViewById(R.id.playControl);
            ImageView removeControl = (ImageView) rowView.findViewById(R.id.removeControl);
            removeControl.setOnClickListener(onMoviehideClick);

            ImageView trailerIndicator = (ImageView) rowView.findViewById(R.id.trailerIndicator);
			TextView likesNum = (TextView) rowView.findViewById(R.id.numLikes);
			ImageView likeControl = (ImageView) rowView.findViewById(R.id.likeImage);
			ControlledMovieView videoView = (ControlledMovieView) rowView.findViewById(R.id.videoPlayerView);
			ProgressBar progressBar = (ProgressBar) rowView.findViewById(R.id.progress);
			View unseenIndicator = rowView.findViewById(R.id.unseenIndicator);
			ImageButton replyButton = (ImageButton)rowView.findViewById(R.id.replyButton);

			// 4. set data & callbacks
			rowView.initAndLoadCast(movie, aq, ArrayMoviesFragment.this);

			if (!movie.isSeen()) {
	            playControl.setImageResource(R.drawable.ic_play_roll_new);
			}
            if (movie.getClipCount()<2) {
                // hide multiple movie indicator
                trailerIndicator.setVisibility(View.GONE);
            }
            else {
                ViewUtility.setViewWidth(trailerIndicator, calculcatedVideoWidth);
            }
			
			ViewUtility.setViewSquare(videoThumb, calculcatedVideoWidth);
			ViewUtility.setViewSquare(playControl, calculcatedVideoWidth);

			
			videoView.init(ArrayMoviesFragment.this, videoThumb, calculcatedVideoWidth, position, 
					movie, mUuid, progressBar, unseenIndicator, playControl);
            // todo: optimize to not create a listener for each, but reuse one listener
			videoThumb.setOnClickListener(new ThumbClickListener(videoView));
			replyButton.setOnClickListener(new ReplyClickListener(movie));

            aq.id(rowView.findViewById(R.id.shareImage)).clicked(this, "onShareClicked");
			
			String ageText = DateUtils.getRelativeTimeSpanString(
					movie.getPublishedOn(), c.getTimeInMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString();
			if (HIDE_AGE_AGO_POSTFIX) {
				ageText = ageText.replace(" ago", "");
			}
			aq.id(rowView.findViewById(R.id.ageText)).text(ageText);
			likesNum.setText(shortLikesString(movie.getLikeCount()));
			
			if (movie.getCast()!=null) {
				for (int i=0; i<movie.getCast().length && i< MovieItemView.castIds.length; i++) {
					RoundedImageView castImage = (RoundedImageView)
							rowView.findViewById(MovieItemView.castIds[i]);
					aq.id(castImage).image(movie.getCast()[i], true, false, 0, R.drawable.ic_avatar_default);
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
			public ThumbClickListener(ControlledMovieView v){
				this.pv = v;
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

			public ReplyClickListener(Movie movie){
				this.movie = movie;
			}
			
			@Override
			public void onClick(View v) {
                if (isTrial) {
                    fireGAnalyticsEvent("ui_action", "touch", "replyButton_trial", null);
                    Toast.makeText(context, "Please sign in to reply to this video. You can sign in from app menu.", Toast.LENGTH_SHORT).show();
                }
                else {
                    fireGAnalyticsEvent("ui_action", "touch", "replyButton", null);
                    Intent intent = new Intent(getActivity(), VideoCaptureActivity.class);
                    intent.putExtra(VideoCaptureActivity.RESPOND_TO_CLIP, movie.getLastClipId());
                    intent.putExtra(VideoCaptureActivity.RESPOND_TO_CLIP_URL, movie.getLastClipUrl());
                    intent.putExtra(VideoCaptureActivity.CURRENT_CHANNEL, mChanId);
                    intent.putExtra(VideoCaptureActivity.MOVIE, movie.getId());
                    intent.putExtra(VideoCaptureActivity.LAST_USER_UUID, movie.getLastUserId());
                    intent.putExtra(VideoCaptureActivity.LAST_USER_AVATAR, movie.getLastUserAvatar());
                    ((TabbedChannelsActivity)getActivity()).
                            startActivityForResult(intent, TabbedChannelsActivity.VIDEOCAPTURE_REQUEST);
//                    startActivity(intent);
                }
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
				fireGAnalyticsEvent("ui_action", "touch", "likeButton"+(isTrial?"_trial":""), null);
				
				if (trial) {
					count++;
					if (count==1){
						Toast.makeText(context, "You can \"like\" the video after signing in via app menu.", Toast.LENGTH_SHORT).show();
					}
					else {
						Toast.makeText(context, "Sign in already! ;)", Toast.LENGTH_SHORT).show();
						count=0;
					}
					return;
				}
				// first, invert the likes
				movie.setUserLikes(!movie.isUserLikes());

				if (movie.isUserLikes()) {
					movie.setLikeCount(movie.getLikeCount() + 1);
					view.setImageResource(R.drawable.ic_star_on);
					likesNum.setText(shortLikesString(movie.getLikeCount()));
					userLikes.add(movie.getId()+"");
				} else {
					movie.setLikeCount(movie.getLikeCount() - 1);
					view.setImageResource(R.drawable.ic_star_off);
					likesNum.setText(shortLikesString(movie.getLikeCount()));
					userLikes.remove(movie.getId()+"");
				}

				if (!trial) 
				{
					String url = PrefUtility.getApiUrl(ServerUtility.API_STORY_LIKE, "uuid=" + this.uuid + "&story=" + movie.getId());
					
					if (!movie.isUserLikes()) 
					{
						url = PrefUtility.getApiUrl(ServerUtility.API_STORY_DISLIKE, "uuid=" + this.uuid + "&story=" + movie.getId());
					}
					
	
					aq.auth(basicHandle).ajax(url, JSONObject.class, new AjaxCallback<JSONObject>() {
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

        public void onShareClicked()
        {
            fireGAnalyticsEvent("ui_action", "touch", "share", null);
            new ShareDialog().show(getActivity().getSupportFragmentManager(), "ShareDialog");
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
    public static void resetUnseenMoviesNumber(int num) {
    	unseenMoviesCount = num;
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
			resetUnseenMoviesNumber(0);
			for (int i = 0; i < jarr.length(); i++) {
				unseenMoviesCount++;
			}
			Log.v(LOGTAG, "unseen movie:" + jarr.length()+", set: "+unseenMoviesCount);
//				refreshUnseenBadge(unseenStories);
		} else {
			// ajax error
			apiError(LOGTAG,
					"unseenMoviesCb: null Json, could not get unseenMovies for uuid " + mUuid, status, false, Log.ERROR);
		}
	}

	public void updateMovieList() {
		Log.v(LOGTAG, "updateMovieList on chan: "+mChanId);
		String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_CHAN_MOVIES,
				"uuid=" + mUuid +"&channel="+mChanId + "&limit=" + LIMIT_ITEMS);

        showMainProgress(true);
        if (isTrial) {
            aq.ajax(apiUrl, JSONArray.class, this, "updateMovieListCb");
        }
        else {
            aq.auth(basicHandle).ajax(apiUrl, JSONArray.class, this, "updateMovieListCb");
        }
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

    private void showMainProgress(boolean show) {
        ((TabbedChannelsActivity)getActivity()).showProgress(show);
    }

}