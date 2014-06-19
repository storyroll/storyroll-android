package co.storyroll.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;
import co.storyroll.PQuery;
import co.storyroll.R;
import co.storyroll.adapter.ChannelAdapter;
import co.storyroll.base.MenuChannelListActivity;
import co.storyroll.gcm.GcmIntentService;
import co.storyroll.model.ChannelInfo;
import co.storyroll.util.*;
import com.androidquery.callback.AjaxStatus;
import com.google.analytics.tracking.android.Fields;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by martynas on 17/06/14.
 */
public class MainChannelsActivity extends MenuChannelListActivity implements SwipeRefreshLayout.OnRefreshListener {

    private static final String LOGTAG = "CHAN_LIST";
    private static final String SCREEN_NAME = "TabbedChannels";
    public static final String EXTRA_CHANNEL_ID = "channelId" ;
    public static final String STORED_BUNDLE_CHANNEL_ID = "channelId" ;

    static final int PICK_CONTACTS_REQUEST = 1111;  // The request code
    public static final int VIDEOCAPTURE_REQUEST = 1112;  // The request code
    private static final int MOVIELIST_REQUEST = 1654 ;

    private TextView tabUnseenBadgeText = null;
//    private static List<ChannelInfo> mChannels;
    private ArrayList<ChannelInfo> mChannels;

    private String mUuid;

    private boolean isCallFromNotificationProcessing = false;
    private long initialChannelId = -1L;
    private int lastUpdatedMovieIdx = 0;
    private boolean channelsLoaded = false;

    private SwipeRefreshLayout swipeContainer;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getGTracker().set(Fields.SCREEN_NAME, SCREEN_NAME);

        // We'll define a custom screen layout here (the one shown above), but
        // typically, you could just use the standard ListActivity layout.
        setContentView(R.layout.activity_channel_list);

        mChannels = new ArrayList<ChannelInfo>();
        mUuid = getUuid();

        // Now create a new list adapter bound to the cursor.
        // SimpleListAdapter is designed for binding to a Cursor.
        ListAdapter adapter = new ChannelAdapter(this, mChannels);

        // Bind to our new adapter.
        setListAdapter(adapter);

        // restore the visible channel id
        if ( savedInstanceState!=null //&& !getIntent().getBooleanExtra(GcmIntentService.EXTRA_NOTIFICATION, false)
                ) {
            Log.v(LOGTAG, "1a: Restoring from savedInstanceState");
            initialChannelId = savedInstanceState.getLong(STORED_BUNDLE_CHANNEL_ID);
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
                initialChannelId = Long.valueOf(chIdStr);
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
                initialChannelId = getIntent().getExtras().getLong(EXTRA_CHANNEL_ID);
                Log.v(LOGTAG, "initial channel id from Intent: "+initialChannelId);
            }
        }

        swipeContainer = SwipeUtil.initSwiping(this, getListView(), this);
    }

    @Override
    public void onStart() {
        super.onStart();
        // get chan list
        chanListAjaxCall();

//        mAdapter = new ClipPlaylistTabAdapter(getSupportFragmentManager());
//
//        // Set up the ViewPager, attaching the adapter.
//        mViewPager = (ViewPager) findViewById(R.id.pager);
//        mViewPager.setAdapter(mAdapter);

        // update notification counter
        updateInvitesFromServer();
    }

    @Override
    protected void onListItemClick (ListView l, View v, int position, long id) {
        Toast.makeText(this, "Clicked row " + position, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, ChannelActivity.class);
        intent.putExtra(ChannelActivity.EXTRA_CHANNEL_ID, ((ChannelInfo)getListAdapter().getItem(position)).getChannel().getId());
        startActivityForResult(intent, MOVIELIST_REQUEST);
    }


    /* -------------------- -------------------- -------------------- -------------------- */

    private int movieIdToIdx(long lastUpdatedMovieId) {
        return 1;
    }

    private void chanListAjaxCall(){
        swipeContainer.setRefreshing(true);
        if (!isTrial) {
            aq.auth(basicHandle).ajax(PrefUtility.getApiUrl(ServerUtility.API_CHANNELS2, mUuid == null ? null : ("uuid=" + mUuid)), JSONArray.class, this, "chanListCb");
        }
        else {
            aq.ajax(PrefUtility.getApiUrl(ServerUtility.API_CHANNELS2, mUuid == null ? null : ("uuid=" + mUuid)), JSONArray.class, this, "chanListCb");
        }
    }

    public void chanListCb(String url, JSONArray jarr, AjaxStatus status)  {
        swipeContainer.setRefreshing(false);
        if (ErrorUtility.isAjaxErrorThenReport(LOGTAG, status, this)) {
            channelsLoaded = false;
            return;
        }

        List<ChannelInfo> channels = null;

        if (jarr != null) {
            // successful ajax call
            channels = ModelUtility.channelInfos(jarr);
            channelsLoaded = true;
            // get the list of channels
        } else {
            // ajax error
            ErrorUtility.apiError(LOGTAG,
                    "userLikesCb: null Json, could not get channels for uuid " + mUuid, status, this, false, Log.ERROR);
            channelsLoaded = false;
        }
//        init(channels);
        if (channels!=null)
        {
            mChannels.clear();
            mChannels.addAll(channels);
            ((BaseAdapter)getListAdapter()).notifyDataSetChanged();
        }
        else {
            Toast.makeText(this, "Can't load channels right now. Try agai later.", Toast.LENGTH_SHORT).show();
        }
    }


    public PQuery getAQuery() {
        return aq;
    }

    @Override
    public void onRefresh() {
        Log.v(LOGTAG, "refresh");
        chanListAjaxCall();
//        new Handler().postDelayed(new Runnable() {
//            @Override public void run() {
//                swipeContainer.setRefreshing(false);
//            }
//        }, 5000);
    }
}
