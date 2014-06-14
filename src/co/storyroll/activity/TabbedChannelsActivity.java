package co.storyroll.activity;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import co.storyroll.PQuery;
import co.storyroll.R;
import co.storyroll.base.MenuFragmentActivity;
import co.storyroll.gcm.GcmIntentService;
import co.storyroll.model.Channel;
import co.storyroll.ui.LeaveChanDialog;
import co.storyroll.ui.RollMovieDialog;
import co.storyroll.ui.SignupDialog;
import co.storyroll.util.*;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.analytics.tracking.android.Fields;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class TabbedChannelsActivity
        extends MenuFragmentActivity
        implements SignupDialog.SigninDialogListener, LeaveChanDialog.LeaveChanDialogListener {

	private static final String LOGTAG = "TABBED_CHANS";
	private static final String SCREEN_NAME = "TabbedChannels";
    public static final String EXTRA_CHANNEL_ID = "channelId" ;
    public static final String STORED_BUNDLE_CHANNEL_ID = "channelId" ;

    static final int PICK_CONTACTS_REQUEST = 1111;  // The request code
    public static final int VIDEOCAPTURE_REQUEST = 1112;  // The request code

    /**
     * The {@link android.support.v4.view.ViewPager} that will display the object collection.
     */
    ViewPager mViewPager;
    
    ChannelTabAdapter mAdapter;
//    FragmentPagerAdapter tAdapter;
//    private static Set<String> newStories = null;
    private static String mUuid;
    private long unseenStoriesCount = 0;

	private TextView tabUnseenBadgeText = null;
	static List<Channel> mChannels = null;
	
	private boolean isCallFromNotificationProcessing = false;
	private long initialChannelId = -1L;
	private int lastUpdatedMovieIdx = 0;
	private boolean channelsLoaded = false;
    public static ProgressBar mLoadingProgressBar = null;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clip_playlist);
        // Fields set on a tracker persist for all hits, until they are
        // overridden or cleared by assignment to null.
        getGTracker().set(Fields.SCREEN_NAME, SCREEN_NAME);

        // Initial set up for action bar.
        ActionBarUtility.initCustomActionBar(this, false);

        // Custom indefinite ProgressBar
        final ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 20); //Use dp resources
        mLoadingProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        mLoadingProgressBar.setIndeterminate(true);
        mLoadingProgressBar.setLayoutParams(lp);
        mLoadingProgressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor("#FF5E48"), PorterDuff.Mode.SRC_IN);
        mLoadingProgressBar.setVisibility(View.VISIBLE);
        final ViewGroup decor = (ViewGroup) getWindow().getDecorView();
        decor.addView(mLoadingProgressBar);
        final ViewTreeObserver vto = decor.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            final View content = findViewById(android.R.id.content);
            @Override
            public void onGlobalLayout() {
                int top = content.getTop();
                //Dont do anything until getTop has a value above 0.
                if (top == 0)
                    return;
//                ViewTreeObserver observer = mLoadingProgressBar.getViewTreeObserver();
//                observer.removeGlobalOnLayoutListener(this);
                mLoadingProgressBar.setY(top-12);
            }
        });

        // continue initialization
        mUuid = getUuid();

//        // TODO this is temp hack
//        if (isTrial) {
//        	mUuid = "test@test.com";
//        }

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

    public PQuery getPQuery(){
    	return aq;
    }

    private void initializeActionBar() {
//        // Set up action bar.
//    	ActionBarUtility.initCustomActionBar(this, false);
    	final ActionBar actionBar = getActionBar();
        actionBar.removeAllTabs();
    	
//    	actionBar.setHomeButtonEnabled(true);
        
        // Specify that tabs should be displayed in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        // TODO: custom underline?
        actionBar.setStackedBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.sr_actionbar_tab_underline)));

        // Create a tab listener that is called when the user changes tabs.
        ActionBar.TabListener tabListener = new ActionBar.TabListener() 
        {
			@Override
			public void onTabReselected(Tab tab, FragmentTransaction ft) {
				// TODO Auto-generated method stub
			}

            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                // When the tab is selected, switch to the
                // corresponding page in the ViewPager.
            	
            	if (mViewPager!=null) {
	            	fireGAnalyticsEvent("ui_action", "onTabSelected", getChannels().get(tab.getPosition()).getTitle(), null);
	            	
//	            	Log.v(LOGTAG, "onTabSelected "+tab.getPosition()+" - "+getChannels().get(tab.getPosition()).getTitle());
	                mViewPager.setCurrentItem(tab.getPosition());
            	}
            }

			@Override
			public void onTabUnselected(Tab tab, FragmentTransaction ft) {
				// TODO Auto-generated method stub
			}
        };	
        
        // Add tabs, specifying the tab's text and TabListener
//      if (isTrial) {
//      	tabHeads = ArrayClipsFragment.TAB_HEADINGS_TRIAL;
//      }
      for (int i = 0; i < getChannels().size(); i++) {
      	Channel chan = getChannels().get(i);
      	if (chan!=null) {
      		Tab tab = actionBar.newTab();

            if (chan.isPublic())
      		{
      			tab.setCustomView(R.layout.custom_actionbar_tab);
      			View tabView = tab.getCustomView();
      			TextView tabText  = (TextView) tabView.findViewById(R.id.tabText);
      			tabText.setText(chan.getTitle());
//      			tabUnseenBadgeText  = (TextView) tabView.findViewById(R.id.badgeTextt);
                ((TextView) tabView.findViewById(R.id.badgeTextt)).setText("P");

            } else {
                tab = tab.setText(chan.getTitle());
            }
//            Log.v(LOGTAG, "chanel id, default: "+chan.getId()+", "+(initialChannelId==chan.getId()));
            Log.v(LOGTAG, "chanel id, public: "+chan.isPublic());
	        actionBar.addTab(tab.setTabListener(tabListener), initialChannelId==chan.getId());

        }
      }

	}

	private int movieIdToIdx(long lastUpdatedMovieId) {
		return 1;
	}

	private void chanListAjaxCall(){
        showProgress(true);
        aq.auth(basicHandle).ajax(PrefUtility.getApiUrl(ServerUtility.API_CHANNELS, mUuid == null ? null : ("uuid=" + mUuid)), JSONArray.class, this, "chanListCb");
	}

    public void chanListCb(String url, JSONArray jarr, AjaxStatus status)  {
        showProgress(false);
        if (ErrorUtility.isAjaxErrorThenReport(LOGTAG, status, this)) {
            channelsLoaded = false;
            return;
        }

        List<Channel> channels = null;

        if (jarr != null) {
            // successful ajax call
            channels = ModelUtility.channels(jarr);
            channelsLoaded = true;
            // get the list of channels
        } else {
            // ajax error
            ErrorUtility.apiError(LOGTAG,
                    "userLikesCb: null Json, could not get channels for uuid " + mUuid, status, this, false, Log.ERROR);
            channelsLoaded = false;
            if (channels==null) {
                channels = new ArrayList<Channel>();
            }
        }
        init(channels);
    }

    public void init(List<Channel> newChannels)  {

        List<Channel> chans = getChannels();
        chans.clear();
        chans.addAll(newChannels);

        mAdapter = new ChannelTabAdapter(getSupportFragmentManager());

        // Set up the ViewPager, attaching the adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mAdapter);

        initializeActionBar();

        mViewPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position)
                    {
                        // When swiping between pages, select the
                        // corresponding tab.
//                        Log.v(LOGTAG, "OnPageChangeListener:onPageSelected, pos="+position);
                        getActionBar().setSelectedNavigationItem(position);

                        // TODO is this called when 1st (0) tab selected?

                        Log.v(LOGTAG, "lastUpdatedMovieIdx: "+lastUpdatedMovieIdx);
                        if (isCallFromNotificationProcessing && lastUpdatedMovieIdx!=0)
                        {
                            postSelectItem(lastUpdatedMovieIdx);
                            isCallFromNotificationProcessing = false;
                        }

                        // manually selected "Mine"? refresh badge
//                        if (position == ArrayClipsFragment.TAB_TWO) {
//                        	updateUnseenVideosFromServer();
//                        }

                        // Also update tracker field to persist for all subsequent hits,
                        // until they are overridden or cleared by assignment to null.
                        getGTracker().set(Fields.SCREEN_NAME, SCREEN_NAME+"_"+getChannels().get(position).getId());
                    }
                });

    }

    @Override
    public void onStart(){
    	super.onStart();
		// show help
//		DialogUtility.showHelpOverlay(this);
    }
    
    private void updateUnseenVideosFromServer() {
//    	Log.v(LOGTAG, "updateUnseenStoriesFromServer");
//    	if (isTrial) {
//    		Log.v(LOGTAG, "updateUnseenStoriesFromServer -- skip in trial");
//    	}
//    	else {
//    		aq.ajax(PrefUtility.getApiUrl(ServerUtility.API_UNSEEN_STORIES, mUuid==null?null:("uuid=" + mUuid)), JSONArray.class, this, "unseenStoriesCb");
//    	}
	}

	public void unseenStoriesCb(String url, JSONArray jarr, AjaxStatus status) 
	{
//		Log.v(LOGTAG, "unseenStoriesCb");
//		if (jarr != null) {
//			// successful ajax call
//			try {
//				// TODO crappy hack
//				int[] stories = new int[jarr.length()];
//				for (int i = 0; i < jarr.length(); i++) {
//					stories[i] = jarr.getInt(i);
//				}
//				ArrayMoviesFragment.resetUnseenMovieSet(stories);
//				unseenStoriesCount = jarr.length();
//				Log.v(LOGTAG, "unseen stories:" + unseenStoriesCount);
//				refreshUnseenBadge(unseenStoriesCount);
//			} catch (JSONException e) {
//				Log.e(LOGTAG, "jsonexception", e);
//			}
//
//		} else {
//			// ajax error
//			ErrorUtility.apiError(LOGTAG,
//					"userLikesCb: null Json, could not get unseenStories for uuid " + mUuid, status, this, false, Log.ERROR);
//		}
	}
    

    public void showProgress(boolean show) {
        if (mLoadingProgressBar!=null) {
            mLoadingProgressBar.setVisibility(show?View.VISIBLE:View.INVISIBLE);
        }
    }

    private void refreshUnseenBadge(int num) {
    	unseenStoriesCount = num;
    	if (tabUnseenBadgeText!=null) {
	    	tabUnseenBadgeText.setText(num+"");
			tabUnseenBadgeText.setVisibility(num==0?View.GONE:View.VISIBLE);
    	}
	}
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            event.startTracking();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.isTracking()
                && !event.isCanceled()) {
            // Back button press complete, handle
        	
    		fireGAnalyticsEvent("ui_action", "click", "SystemBack", 0L);
    			    
    		Intent intent = new Intent(Intent.ACTION_MAIN);
    		intent.addCategory(Intent.CATEGORY_HOME);
    		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    		intent.putExtra("EXIT", true);
    		startActivity(intent);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onDialogSigninClick(DialogFragment dialog) {
        // User touched the dialog's Sign-in button
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("overrideBackPress", false);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public void onDialogLeaveChannelConfirm(DialogFragment dialog)
    {
        String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_CHANNEL_LEAVE);
        apiUrl = apiUrl.replaceFirst("\\{uuid\\}", getUuid());
        apiUrl = apiUrl.replaceFirst("\\{channelId\\}", getCurrentChannelId()+"");
        aq.auth(basicHandle).ajax(apiUrl, JSONObject.class, TabbedChannelsActivity.this, "apiChannelLeaveCb");
    }


//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            // back button pressed
//        	long currentTime = System.currentTimeMillis();
//        	long tDiff = currentTime - lastPressed;
//        	if (tDiff>DOUBLE_PUSH_BACK_MILLIS) {
//        		backTries = 0;
//        	}
//        	if (backTries++>0) {
//        		// on second press, exit
//        		System.exit();
//        	}
//        	else {
//        		// on first press, show note
//        		Toast.makeText(this, "To exit, press back twice", Toast.LENGTH_SHORT);
//        	}
//            return true;
//        }
//
//        return super.onKeyDown(keyCode, event);
//    }


//    /**
//     * A {@link android.support.v4.app.FragmentStatePagerAdapter} that returns a fragment
//     * representing an object in the collection.
//     */
//    public static class DemoCollectionPagerAdapter extends FragmentPagerAdapter {
//
//        public DemoCollectionPagerAdapter(FragmentManager fm) {
//            super(fm);
//        }
//
//        @Override
//        public Fragment getItem(int i) {
//            Fragment fragment = new DemoObjectFragment();
//            Bundle args = new Bundle();
//            args.putInt(DemoObjectFragment.ARG_OBJECT, i + 1); // Our object is just an integer :-P
//            fragment.setArguments(args);
//            return fragment;
//        }
//
//        @Override
//        public int getCount() {
//            // We have 4 views
//            return 4;
//        }
//
//        @Override
//        public CharSequence getPageTitle(int position) {
//            return "OBJECT " + (position + 1);
//        }
//    }
//    
//
//
//    /**
//     * A dummy fragment representing a section of the app, but that simply displays dummy text.
//     */
//    public static class DemoObjectFragment extends Fragment {
//
//        public static final String ARG_OBJECT = "object";
//
//        @Override
//        public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                Bundle savedInstanceState) {
//            View rootView = inflater.inflate(R.layout.fragment_tab_list, container, false);
//            Bundle args = getArguments();
//            ((TextView) rootView.
//            		findViewById(R.id.text)).
//            		setText(
//                    Integer.toString(args.getInt(ARG_OBJECT)));
//            return rootView;
//        }
//    }
    
    /* 0-*/
    
    // TODO:
//    if you want to switch out the actual fragments that are being displayed, 
//    you need to avoid FragmentPagerAdapter and use FragmentStatePagerAdapter.
//    FragmentPagerAdapter never destroys a fragment after it's been displayed the first time.
    
    public static class ChannelTabAdapter extends FragmentPagerAdapter {
    	private int count;
    	
        public ChannelTabAdapter(FragmentManager fm) {
            super(fm);
            List<Channel> chans = getChannels();
//            if (isTrial) {
//            	th = ArrayClipsFragment.TAB_HEADINGS_TRIAL;
//            }
        	count = chans.size();
        }

        public void updateCount(int count){
            this.count = count;
        }

        @Override
        public int getCount() {
        	return (getChannels()==null?0:mChannels.size());
        }

        @Override
        public Fragment getItem(int position) {
            return ArrayMoviesFragment.newInstance(position, getChannels().get(position).getId(), mUuid, isTrial);
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            return getChannels().get(position % getChannels().size()).getTitle();
        }

        // see http://stackoverflow.com/questions/9061325/fragmentpageradapter-is-not-removing-items-fragments-correctly
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (position >= getCount()) {
                FragmentManager manager = ((Fragment) object).getFragmentManager();
                android.support.v4.app.FragmentTransaction trans = manager.beginTransaction();
                trans.remove((Fragment) object);
                trans.commit();
            }
        }
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean ret = super.onPrepareOptionsMenu(menu);
        MenuItem item = menu.findItem(R.id.action_roll_movie);
        item.getIcon().setAlpha(112);
//        Drawable resIcon = getResources().getDrawable(R.drawable.ic_action_roll_movie);
//        // grey down the icon
//        resIcon.mutate().setColorFilter(Color.LTGRAY, PorterDuff.Mode.SRC_IN);
//        item.setIcon(resIcon);
        return ret;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
		if (item.getItemId() == R.id.action_new)
		{
            if (isTrial) {
                fireGAnalyticsEvent("ui_action", "touch", "newVidepButton_trial", null);
                new SignupDialog().show(getSupportFragmentManager(), "SignupDialog");
            }
            else {
                fireGAnalyticsEvent("ui_action", "touch", "newVideoButton_trial", null);
                int tabIdx = getActionBar().getSelectedNavigationIndex();
                if (tabIdx < 0) tabIdx = 0;
//                Log.v(LOGTAG, "New item in channel idx=" + tabIdx + ", channels = " + mChannels);
                onNewPressed(mChannels.get(tabIdx).getId());
            }
			 return true;
			 
		}
		else if (item.getItemId() == R.id.action_refresh) {
            fireGAnalyticsEvent("ui_action", "touch", "action_refresh", null);
            updateInvitesFromServer();
            onRefreshChannel();
			return true;
		}
        else if (item.getItemId() == R.id.action_add_group) {
            fireGAnalyticsEvent("ui_action", "touch", "action_add_group", null);
            onAddGroup();
            return true;
        }
        else if (item.getItemId() == R.id.action_roll_movie) {
            fireGAnalyticsEvent("ui_action", "touch", "action_roll_movie", null);
            new RollMovieDialog().show(getSupportFragmentManager(), "RollMovieDialog");
            return true;
        }
        else if (item.getItemId() == R.id.action_leave_channel) {
            fireGAnalyticsEvent("ui_action", "touch", "action_leave_channel", null);
            onLeaveChannelDialog();
            return true;
        }

		return super.onOptionsItemSelected(item);
    }

    /*-- helper --*/
    
    private void onRefreshChannel() {
		Log.v(LOGTAG, "onRefreshChannel, channels been loaded: "+channelsLoaded);
		if (channelsLoaded) {
			int channelIdx = getActionBar().getSelectedNavigationIndex();
			if (channelIdx<0) channelIdx=0;
			ArrayMoviesFragment amf = (ArrayMoviesFragment)findFragmentByPosition(channelIdx);
			amf.updateMovieList();
		}
		else {
			// try updating channels
			chanListAjaxCall();
		}
	}


    private void onAddGroup() {
        if (channelsLoaded) {
            // call address picker
            Intent pickContactsIntent = new Intent(getApplicationContext(), ContactManagerActivity.class);
            startActivityForResult(pickContactsIntent, PICK_CONTACTS_REQUEST);
        }
        else {
            // try updating channels
            chanListAjaxCall();
        }
    }


    private void onLeaveChannelDialog()
    {
        fireGAnalyticsEvent("ui_action", "touch", "leaveChannel", null);

        Long chanId = getCurrentChannelId();
        if (chanId!=null && chanId!=1) {
            new LeaveChanDialog().show(getSupportFragmentManager(), "LeaveChanDialog");
        }
    }

    public void apiChannelLeaveCb(String url, JSONObject json, AjaxStatus status) throws JSONException
    {
//        Log.v(LOGTAG, "apiChannelLeaveCb: "+(json==null?"null":json.toString()));
        if (ErrorUtility.isAjaxErrorThenReport(LOGTAG, status, TabbedChannelsActivity.this)) return;
//        Log.v(LOGTAG, "removing idx "+getActionBar().getSelectedNavigationIndex() + "chan: "+mChannels.get(getActionBar().getSelectedNavigationIndex()).toString());
        if (json.getBoolean("result"))
        {
            // remove tab
            final ActionBar bar = getActionBar();
            int selectedIdx = bar.getSelectedNavigationIndex();
            Log.v(LOGTAG, "removing index: "+selectedIdx);
            if (mChannels.size() > 0)
            {
                if (selectedIdx==0) {
                    bar.setSelectedNavigationItem(1);
                }
                else {
                    bar.setSelectedNavigationItem(selectedIdx-1);
                }
                mChannels.remove(selectedIdx);
                mAdapter.updateCount(mChannels.size());
                mAdapter.notifyDataSetChanged();
                bar.removeTab(bar.getTabAt(selectedIdx));
//                initializeActionBar();
            }
        }
        else {
            ErrorUtility.apiError(LOGTAG, "Could not remove channel", status, this, false, Log.WARN);
            Log.e(LOGTAG, "Could not remove channel: "+url);
        }
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
            chanListAjaxCall();
        }
        else if (requestCode == VIDEOCAPTURE_REQUEST)
        {
            if (resultCode == RESULT_OK)
            {
                Log.v(LOGTAG, "VIDEOCAPTURE_REQUEST: RESULT_OK");
                Toast.makeText(this, R.string.video_uploaded, Toast.LENGTH_SHORT).show();
            }
            else if (resultCode == RESULT_CANCELED) {
                Log.v(LOGTAG, "VIDEOCAPTURE_REQUEST: RESULT_CANCELED");
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STORED_BUNDLE_CHANNEL_ID, getCurrentChannelId());
        Log.v(LOGTAG, "onSaveInstanceState, current chanId: "+getCurrentChannelId());
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

        String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_INVITES_ADD, "uuid=" + mUuid + "&c=" + getCurrentChannelId());


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

    private Long getCurrentChannelId()
    {
        int channelIdx = getActionBar().getSelectedNavigationIndex();
        if (channelIdx<0) channelIdx=0;
        if (getChannels().size()>0) {
            return getChannels().get(channelIdx).getId();
        }
        else {
            return null;
        }
    }
    
    private void postSelectItem(int idx) {
		// TODO Auto-generated method stub
//		Log.v(LOGTAG, "postSelectItem: "+idx);
		int channelIdx = getActionBar().getSelectedNavigationIndex();
		if (channelIdx<0) channelIdx=0;
		ArrayMoviesFragment amf = (ArrayMoviesFragment)findFragmentByPosition(channelIdx);
		amf.postSelectItem(idx);
	}
    
    
    // a hack?
    // http://stackoverflow.com/questions/11976397/android-getting-fragment-that-is-in-fragmentpageradapter
    public Fragment findFragmentByPosition(int position) {
        
        return getSupportFragmentManager().findFragmentByTag(
                "android:switcher:" + mViewPager.getId() + ":"
                        + mAdapter.getItemId(position));
    }

//	public static void setChannels(List<Channel> mChannels) {
//		mChannels = mChannels;
//	}

	public static List<Channel> getChannels() {
		if (mChannels==null) {
            mChannels = new ArrayList<Channel>();
			Log.w(ArrayMoviesFragment.LOGTAG, "trying to access null channel list!");
		}
		return mChannels;
	}


}

