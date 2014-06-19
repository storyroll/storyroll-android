package co.storyroll.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;
import co.storyroll.R;
import co.storyroll.adapter.MovieAdapter;
import co.storyroll.base.MenuChannelActivity;
import co.storyroll.gcm.GcmIntentService;
import co.storyroll.model.Movie;
import co.storyroll.ui.dialog.LeaveChanDialog;
import co.storyroll.ui.dialog.SignupDialog;
import co.storyroll.util.ErrorUtility;
import co.storyroll.util.PrefUtility;
import co.storyroll.util.ServerUtility;
import co.storyroll.util.SwipeUtil;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.analytics.tracking.android.Fields;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by martynas on 17/06/14.
 */
public class ChannelActivity extends MenuChannelActivity
        implements SwipeRefreshLayout.OnRefreshListener, LeaveChanDialog.LeaveChanDialogListener {

    private static final String LOGTAG = "MOVIE_LIST";
    private static final String SCREEN_NAME = "ListMovies";
    public static final String EXTRA_CHANNEL_ID = "channelId" ;
    public static final String STORED_BUNDLE_CHANNEL_ID = "channelId" ;
    public static final String EXTRA_CHANNEL_TITLE = "channelTitle";
    public static final String STORED_BUNDLE_CHANNEL_TITLE = "channelTitle" ;


    static final int PICK_CONTACTS_REQUEST = 1111;  // The request code
    public static final int VIDEOCAPTURE_REQUEST = 1112;  // The request code
    private static final int LIMIT_ITEMS = 40;

    private ArrayList<Movie> movies;

    private String mUuid;

    private boolean isCallFromNotificationProcessing = false;
    private long mChannelId = -1L;
    private String mChannelTitle = "<Unnamed>";
    private int lastUpdatedMovieIdx = 0;
    private MovieAdapter movieAdapter;

    private SwipeRefreshLayout swipeContainer;


    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getGTracker().set(Fields.SCREEN_NAME, SCREEN_NAME);

        mChannelTitle = getIntent().getExtras().getString(EXTRA_CHANNEL_TITLE);
        mChannelId = getIntent().getExtras().getLong(EXTRA_CHANNEL_ID, -1L);

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
            mChannelTitle = savedInstanceState.getString(STORED_BUNDLE_CHANNEL_TITLE);
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
            mChannelTitle = extras.getString(GcmIntentService.EXTRA_CHANNEL_TITLE);


            String lumIdStr = extras.getString(GcmIntentService.EXTRA_LAST_UPDATED_MOVIE);
//            Log.v(LOGTAG, "from notification, EXTRA_LAST_UPDATED_MOVIE: "+lumIdStr);
            long lastUpdatedMovieId = TextUtils.isEmpty(lumIdStr)?-1L:Long.valueOf(chIdStr);
            lastUpdatedMovieIdx = movieIdToIdx(lastUpdatedMovieId);

//			refreshUnseenBadge( getIntent().getIntExtra("count", 0) );
//			actionBar.setSelectedNavigationItem(ArrayClipsFragment.TAB_TWO);

        }
        else {
            // update unseenStories
            Log.v(LOGTAG, "1c: new activity");
            if (getIntent().getExtras()!=null && getIntent().getExtras().containsKey(EXTRA_CHANNEL_ID))
            {
                mChannelId = getIntent().getExtras().getLong(EXTRA_CHANNEL_ID);
                Log.v(LOGTAG, "initial channel id from Intent: "+ mChannelId);
                mChannelTitle = getIntent().getExtras().getString(EXTRA_CHANNEL_TITLE);

            }
        }
        setTitle(mChannelTitle);
        Log.v(LOGTAG, "channelId: "+mChannelId);

        // set adapter
        movieAdapter = new MovieAdapter(this, movies, aq, mUuid, mChannelId, isTrial);

        ListView lv = (ListView)findViewById(android.R.id.list);
        // Bind to our new adapter.
        lv.setAdapter(movieAdapter);

        swipeContainer = SwipeUtil.initSwiping(this, lv, this);

    }

    @Override
    public void onStart() {
        super.onStart();

        // get chan list
        movieListAjaxCall();

        // update notification counter
        updateInvitesFromServer();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        if (item.getItemId() == R.id.action_leave_channel) {
            fireGAnalyticsEvent("ui_action", "touch", "action_leave_channel", null);
            onLeaveChannelDialog();
            return true;
        }
        else if (item.getItemId() == R.id.action_add_group) {
            fireGAnalyticsEvent("ui_action", "touch", "action_add_group", null);
            onAddGroup();
            return true;
        } else if (item.getItemId() == R.id.action_new)
        {
            onNewPressed(mChannelId);
            return true;

        } else {
            return super.onOptionsItemSelected(item);
        }
    }


    /* -------------------- -------------------- BUSINESS LOGICS -------------------- -------------------- */

    private void onAddGroup() {
        // call address picker
        Intent pickContactsIntent = new Intent(getApplicationContext(), ContactManagerActivity.class);
        startActivityForResult(pickContactsIntent, PICK_CONTACTS_REQUEST);
    }


    private void onLeaveChannelDialog()
    {
        fireGAnalyticsEvent("ui_action", "touch", "leaveChannel", null);
        if (mChannelId!=1 && mChannelId!=-1) {
            new LeaveChanDialog().show(getSupportFragmentManager(), "LeaveChanDialog");
        }
        else {
            Toast.makeText(this, "Currently we'd like you to keep at least one channel running.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDialogLeaveChannelConfirm(DialogFragment dialog)
    {
        String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_CHANNEL_LEAVE);
        apiUrl = apiUrl.replaceFirst("\\{uuid\\}", getUuid());
        apiUrl = apiUrl.replaceFirst("\\{channelId\\}", mChannelId+"");
        aq.auth(basicHandle).ajax(apiUrl, JSONObject.class, this, "apiChannelLeaveCb");
    }


    public void apiChannelLeaveCb(String url, JSONObject json, AjaxStatus status) throws JSONException
    {
//        Log.v(LOGTAG, "apiChannelLeaveCb: "+(json==null?"null":json.toString()));
        if (ErrorUtility.isAjaxErrorThenReport(LOGTAG, status, this)) return;
        if (json.getBoolean("result"))
        {
            // return home
            // todo: indicate via bundle that channel list has to be updated?
            finish();
        }
        else {
            ErrorUtility.apiError(LOGTAG, "Could not leave channel. Please try again later", status, this, true, Log.WARN);
            Log.e(LOGTAG, "Could not remove channel: "+url);
        }
    }

    // todo crappy hack
    static int k = 0;
    private int sendInvites(ArrayList<String> cons){
        JSONArray emailsJson = new JSONArray();

        for (String email: cons)
        {
            Log.e(LOGTAG, "contact: " + email);
            emailsJson.put(email);
        }

        String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_INVITES_ADD, "uuid=" + mUuid + "&c=" + mChannelId);


        AjaxCallback ac =  new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject json, AjaxStatus status)
            {
                Log.v(LOGTAG, "callback result: "+json.toString());
                String s = json.toString();
                k = 0;
                for( int i=0; i<s.length(); i++ ) {
                    if( s.charAt(i) == '@' ) {
                        k++;
                    }
                }
            }
        };

        try {
            StringEntity entity = new StringEntity(emailsJson.toString());
            aq.auth(basicHandle).post(apiUrl, "application/json", entity, JSONObject.class, ac);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return k;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.v(LOGTAG, "onActivityResult, requestCode: "+requestCode+", resultCode"+resultCode);
        // Check which request we're responding to
        if (requestCode == PICK_CONTACTS_REQUEST)
        {
            // Make sure the request was successful
            if (resultCode == RESULT_OK)
            {
                Log.v(LOGTAG, "PICK_CONTACTS_REQUEST: RESULT_OK");
                // The user picked a contact.
//                ArrayList<Contact> cons = intent.getParcelableArrayListExtra("SELECTED_CONTACTS");
                ArrayList<String> emails = intent.getStringArrayListExtra("SELECTED_CONTACTS");

                if (emails.size()>0) {
                    sendInvites(emails);
                    Toast.makeText(this, emails.size()+" invitations sent", Toast.LENGTH_SHORT).show(); // TODO - show to how many people it was sent?
                }
            }
            else if (resultCode == RESULT_CANCELED) {
                Log.v(LOGTAG, "PICK_CONTACTS_REQUEST: RESULT_CANCELED");
            }
        }
        else if (requestCode == MANAGE_INVITES_REQUEST)
        {
            updateInvitesFromServer();
            // todo: switch channel to the accepted one
        }
        else if (requestCode == VIDEOCAPTURE_REQUEST)
        {
            if (resultCode == RESULT_OK)
            {
                Log.v(LOGTAG, "VIDEOCAPTURE_REQUEST: RESULT_OK");
                Toast.makeText(this, R.string.video_uploaded, Toast.LENGTH_SHORT).show();
                // refresh after some seconds
                new Handler().postDelayed(new Runnable() {
                    @Override public void run() {
                        movieListAjaxCall();
                    }
                }, 5000);
            }
            else if (resultCode == RESULT_CANCELED) {
                Log.v(LOGTAG, "VIDEOCAPTURE_REQUEST: RESULT_CANCELED");
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }

    private int movieIdToIdx(long lastUpdatedMovieId) {
        return 1;
    }

    private void movieListAjaxCall() {
        String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_CHAN_MOVIES,
                "uuid=" + mUuid +"&channel="+mChannelId + "&limit=" + LIMIT_ITEMS);

        swipeContainer.setRefreshing(true);
        // todo - synchronize on refresh's AsyncTask
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
                int updates = 0;
                for (int i = 0; i < jarr.length(); i++) {
                    JSONObject movieObj = jarr.getJSONObject(i);
                    Movie movie = new Movie(movieObj);

                    // movie with this id exists?
                    int foundIdx = getMovieIndexById(movie.getId());


                    if (foundIdx!=-1 && movies.size()>0 && movies.get(foundIdx).getPublishedOn() != movie.getPublishedOn())
                    {
                        // movie updated, remove and insert
                        movieAdapter.remove(movies.get(foundIdx));
                        movies.remove(foundIdx);

                        // manually set userLikes flag
//                        movie.setUserLikes(userLikes.contains(movie.getId()+""));

                        movieAdapter.insert(movie, 0);
                        movies.add(movie);
                        updates++;
                    }
                    else if (foundIdx==-1) {
                        movieAdapter.insert(movie, 0);
                        movies.add(movie);
                        updates++;
                    }
                }
                Log.v(LOGTAG, "mergeMovieListSorted - movies:" + movies.size()+ ", Adapter: "+movieAdapter.getCount()+", updated items: "+updates);

                // refresh the adapter now
                movieAdapter.notifyDataSetChanged();
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
                    aq.id(android.R.id.empty).visible().text("No videos yet. You can post first!");
                }
                else {
                    aq.id(android.R.id.empty).gone();
                }
                // refresh the adapter now
                movieAdapter.notifyDataSetChanged();
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

    protected void onNewPressed(Long chanId) {
        Intent intent;
        if (isTrial) {
            fireGAnalyticsEvent("ui_action", "touch", "joinRoll_trial", null);
            new SignupDialog().show(getSupportFragmentManager(), "SignupDialog");
        }
        else {
            fireGAnalyticsEvent("ui_action", "touch", "joinRoll_regged", null);
            intent = new Intent(this, VideoCaptureActivity.class);
            intent.putExtra("MODE_NEW", true);
            if (chanId!=null && chanId!=-1L) {
                intent.putExtra(VideoCaptureActivity.CURRENT_CHANNEL, chanId);
            }
            startActivityForResult(intent, TabbedChannelsActivity.VIDEOCAPTURE_REQUEST);
        }

    }

    @Override
    public void onRefresh() {
        Log.v(LOGTAG, "refresh");
        movieListAjaxCall();
    }

//    public PQuery getAQuery() {
//        return aq;
//    }

}
