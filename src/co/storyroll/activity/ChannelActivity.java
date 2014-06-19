package co.storyroll.activity;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import co.storyroll.PQuery;
import co.storyroll.R;
import co.storyroll.adapter.MovieAdapter;
import co.storyroll.base.MenuChannelActivity;
import co.storyroll.gcm.GcmIntentService;
import co.storyroll.model.Movie;
import co.storyroll.util.ErrorUtility;
import co.storyroll.util.PrefUtility;
import co.storyroll.util.ServerUtility;
import co.storyroll.util.SwipeUtil;
import com.androidquery.callback.AjaxStatus;
import com.google.analytics.tracking.android.Fields;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by martynas on 17/06/14.
 */
public class ChannelActivity extends MenuChannelActivity implements SwipeRefreshLayout.OnRefreshListener {

    private static final String LOGTAG = "MOVIE_LIST";
    private static final String SCREEN_NAME = "ListMovies";
    public static final String EXTRA_CHANNEL_ID = "channelId" ;
    public static final String STORED_BUNDLE_CHANNEL_ID = "channelId" ;

    static final int PICK_CONTACTS_REQUEST = 1111;  // The request code
    public static final int VIDEOCAPTURE_REQUEST = 1112;  // The request code
    private static final int LIMIT_ITEMS = 40;

    private ArrayList<Movie> movies;

    private String mUuid;

    private boolean isCallFromNotificationProcessing = false;
    private long mChannelId = -1L;
    private int lastUpdatedMovieIdx = 0;

    private SwipeRefreshLayout swipeContainer;


    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getGTracker().set(Fields.SCREEN_NAME, SCREEN_NAME);
//        ActionBarUtility.initCustomActionBar(this, true);

        // We'll define a custom screen layout here (the one shown above), but
        // typically, you could just use the standard ListActivity layout.
        setContentView(R.layout.activity_channel_list);
        aq.id(android.R.id.empty).text("Loading clips...");

        movies = new ArrayList<Movie>();
        mUuid = getUuid();

        // restore the visible channel id
        if ( savedInstanceState!=null //&& !getIntent().getBooleanExtra(GcmIntentService.EXTRA_NOTIFICATION, false)
                ) {
            Log.v(LOGTAG, "1a: Restoring from savedInstanceState");
            mChannelId = savedInstanceState.getLong(STORED_BUNDLE_CHANNEL_ID);
        }
        // comes from notification? switch to indicated tab and then scroll to indicated item on list
        else if (getIntent().getBooleanExtra(GcmIntentService.EXTRA_NOTIFICATION, false))
        {
            Log.v(LOGTAG, "1b: coming from notification: "+true);
            isCallFromNotificationProcessing = true;
            // TODO crappy hack / set properties for each channels badges
//			ArrayMoviesFragment.resetUnseenMoviesNumber( getIntent().getInt("clips") );
            Bundle extras = getIntent().getExtras();
            String chIdStr = extras.getString(GcmIntentService.EXTRA_CHANNEL_ID_STR);
            Log.v(LOGTAG, "from notification, EXTRA_CHANNEL_ID_STR: "+chIdStr);
            if (!TextUtils.isEmpty(chIdStr)){
                mChannelId = Long.valueOf(chIdStr);
            }

            String lumIdStr = extras.getString(GcmIntentService.EXTRA_LAST_UPDATED_MOVIE);
//            Log.v(LOGTAG, "from notification, EXTRA_LAST_UPDATED_MOVIE: "+lumIdStr);
            long lastUpdatedMovieId = TextUtils.isEmpty(lumIdStr)?-1L:Long.valueOf(chIdStr);
            lastUpdatedMovieIdx = movieIdToIdx(lastUpdatedMovieId);

//			refreshUnseenBadge( getIntent().getIntExtra("count", 0) );
//			actionBar.setSelectedNavigationItem(ArrayClipsFragment.TAB_TWO);

        }
        else {
            // update unseenStories
//			updateUnseenVideosFromServer();
            Log.v(LOGTAG, "1c: new activity");
            if (getIntent().getExtras()!=null && getIntent().getExtras().containsKey(EXTRA_CHANNEL_ID))
            {
                mChannelId = getIntent().getExtras().getLong(EXTRA_CHANNEL_ID);
                Log.v(LOGTAG, "initial channel id from Intent: "+ mChannelId);
            }
        }

        // Now create a new list adapter bound to the cursor.
        // SimpleListAdapter is designed for binding to a Cursor.
        ListAdapter adapter = new MovieAdapter(this, movies, aq, mUuid, mChannelId, isTrial);

        // Bind to our new adapter.
        setListAdapter(adapter);

        swipeContainer = SwipeUtil.initSwiping(this, getListView(), this);

    }

    @Override
    public void onStart() {
        super.onStart();

        // get chan list
        movieListAjaxCall();

        // update notification counter
        updateInvitesFromServer();
    }

    /* -------------------- -------------------- -------------------- -------------------- */

    private int movieIdToIdx(long lastUpdatedMovieId) {
        return 1;
    }

    private void movieListAjaxCall() {
        String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_CHAN_MOVIES,
                "uuid=" + mUuid +"&channel="+mChannelId + "&limit=" + LIMIT_ITEMS);

        swipeContainer.setRefreshing(true);
        if (isTrial) {
            aq.ajax(apiUrl, JSONArray.class, this, "getMovieListCb");
        }
        else {
            aq.auth(basicHandle).ajax(apiUrl, JSONArray.class, this, "getMovieListCb");
        }
    }

    public void getMovieListCb(String url, JSONArray jarr, AjaxStatus status)
    {
        swipeContainer.setRefreshing(false);
        getMovieListSorted(url, jarr, status, true);
    }

    public void updateMovieListCb(String url, JSONArray jarr, AjaxStatus status)
    {
        swipeContainer.setRefreshing(false);
        refreshMovieListSorted(url, jarr, status, true);
    }

    public void refreshMovieListSorted(String url, JSONArray jarr, AjaxStatus status, boolean sorted)
    {
        if (ErrorUtility.isAjaxErrorThenReport(LOGTAG, status, this)) {
            aq.id(R.id.emptyMessage).gone();
            return;
        }

        if (jarr != null) {
            // successful ajax call
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
//                        movie.setUserLikes(userLikes.contains(movie.getId()+""));

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
                Log.v(LOGTAG, "mergeMovieListSorted - movies:" + movies.size()+ ", Adapter: "+aa.getCount()+", updated items: "+updates);

                // refresh the adapter now
                ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
            }
            catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        } else {
            // ajax error
            ErrorUtility.apiError(LOGTAG,
//					"getMovieListSorted: null Json, could not get blink list for uuid " + mUuid, status, false, Log.ERROR);
                    "Error getting movies", status, this, true, Log.ERROR);
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
        if (ErrorUtility.isAjaxErrorThenReport(LOGTAG, status, this)) {
            aq.id(R.id.emptyMessage).gone();
            aq.id(android.R.id.empty).text("Something broke while loading videos. Please try refreshing later.");
            return;
        }

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
//                    movie.setUserLikes(userLikes.contains(movie.getId()+""));
                    movies.add(movie);
//				}
                }
                if (sorted) {
                    Collections.sort(movies);
                }
                Log.v(LOGTAG, "movies:" + movies.size());
                if (movies.size()<1) {
                    aq.id(android.R.id.empty).text("No videos yet. You can post first!");
                }

                // refresh the adapter now
                ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
            }
            catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        } else {
            // ajax error
            aq.id(android.R.id.empty).text("Something broke while loading videos. Please try refreshing later.");
            ErrorUtility.apiError(LOGTAG,
//					"getMovieListSorted: null Json, could not get blink list for uuid " + mUuid, status, false, Log.ERROR);
                    "Error getting movies", status, this, true, Log.ERROR);
        }
        if (movies.size()<1) {
            aq.id(R.id.emptyMessage).gone();
        }
    }

    @Override
    public void onRefresh() {
        Log.v(LOGTAG, "refresh");
        movieListAjaxCall();
    }


    public PQuery getAQuery() {
        return aq;
    }
}
