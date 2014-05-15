package com.storyroll.activity;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.androidquery.callback.AjaxStatus;
import com.google.analytics.tracking.android.Fields;
import com.storyroll.PQuery;
import com.storyroll.R;
import com.storyroll.base.MenuFragmentActivity;
import com.storyroll.model.Channel;
import com.storyroll.util.ActionBarUtility;
import com.storyroll.util.ErrorUtility;
import com.storyroll.util.ModelUtility;
import com.storyroll.util.PrefUtility;
import com.storyroll.util.ServerUtility;

public class TabbedChannelsActivity extends MenuFragmentActivity {

	private static final String LOGTAG = "TabbedChannelsActivity";
	private static final String SCREEN_NAME = "TabbedChannels";

    /**
     * The {@link android.support.v4.view.ViewPager} that will display the object collection.
     */
    ViewPager mViewPager;
    
    ChannelTabAdapter mAdapter;
//    FragmentPagerAdapter tAdapter;
    private static PQuery aq;
//    private static Set<String> newStories = null;
    private static String mUuid;
    private int unseenStoriesCount = 0;

	private TextView tabUnseenBadgeText = null;
	static List<Channel> mChannels = null;
	private int initialChannelid = 0;
	
    
    
    public PQuery getPQuery(){
    	return aq;
    }

    public void chanListCb(String url, JSONArray jarr, AjaxStatus status)  {
    	Log.v(LOGTAG, "chanListCb");
    	List<Channel> channels = null;
    	
		if (jarr != null) {
			// successful ajax call
	    	channels = ModelUtility.channels(jarr);

		} else {
			// ajax error
			ErrorUtility.apiError(LOGTAG,
					"userLikesCb: null Json, could not get channels for uuid " + mUuid, status, this, false, Log.ERROR);
		}
		
    	// get the list of channels
    	init(channels);
    }
    
    public void init(List<Channel> channels)  {
    	
    	this.mChannels = channels;
    	
        mAdapter = new ChannelTabAdapter(getSupportFragmentManager());
        
        // Set up the ViewPager, attaching the adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mAdapter);
        
        // Set up action bar.
    	ActionBarUtility.initCustomActionBar(this, false);
    	final ActionBar actionBar = getActionBar();
    	
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
	            	
	            	Log.v(LOGTAG, "onTabSelected "+tab.getPosition()+" - "+getChannels().get(tab.getPosition()).getTitle());
	                mViewPager.setCurrentItem(tab.getPosition());
            	}
            }

			@Override
			public void onTabUnselected(Tab tab, FragmentTransaction ft) {
				// TODO Auto-generated method stub
			}
        };

        // Add tabs, specifying the tab's text and TabListener
//        if (isTrial) {
//        	tabHeads = ArrayClipsFragment.TAB_HEADINGS_TRIAL;
//        }
        for (int i = 0; i < getChannels().size(); i++) {
        	if (getChannels().get(i)!=null) {
        		Tab tab = actionBar.newTab();
//        		if (i==ArrayClipsFragment.TAB_TWO) 
//        		{
//        			tab = tab.setText(tabHeads[i]);
//        			
//        			tab.setCustomView(R.layout.custom_actionbar_tab);
//        			View tabView = tab.getCustomView();
//        			TextView tabText  = (TextView) tabView.findViewById(R.id.tabText);
//        			tabText.setText(tabHeads[i]);
//        			
//        			tabUnseenBadgeText  = (TextView) tabView.findViewById(R.id.badgeTextt);
//
//        		}
        		tab = tab.setText(getChannels().get(i).getTitle());
	            actionBar.addTab(tab.setTabListener(tabListener));
        	}
        }

        mViewPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) 
                    {
                        // When swiping between pages, select the
                        // corresponding tab.
                    	Log.v(LOGTAG, "OnPageChangeListener: "+position);
                        getActionBar().setSelectedNavigationItem(position);
                        
                        // manually selected "Mine"? refresh badge
//                        if (position == ArrayClipsFragment.TAB_TWO) {
//                        	updateUnseenVideosFromServer();
//                        }
                        
                        // Also update tracker field to persist for all subsequent hits,
                		// until they are overridden or cleared by assignment to null.
                	    getGTracker().set(Fields.SCREEN_NAME, SCREEN_NAME+"_"+getChannels().get(position));
                    }
                });
        
	    // comes from notification? switch to MINE tab
		if (getIntent().getBooleanExtra("NOTIFICATION", false)) 
		{
			// TODO crappy hack
			ArrayMoviesFragment.resetUnseenMovieSet( getIntent().getIntArrayExtra("clips") );
			initialChannelid = getIntent().getIntExtra("channelId", 0);
			// find which position is that
			int initialPosition = 0;
			for (int i=0; i<channels.size(); i++) {
				if (channels.get(i).getId()==initialChannelid) {
					initialPosition = i;
				}
			}
//			refreshUnseenBadge( getIntent().getIntExtra("count", 0) );
//			actionBar.setSelectedNavigationItem(ArrayClipsFragment.TAB_TWO);
			
//			mViewPager.setCurrentItem(tab.getPosition());
			actionBar.setSelectedNavigationItem(initialPosition);
		}
		else {
			// update unseenStories
//			updateUnseenVideosFromServer();
		}
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clip_playlist);
        
		// Fields set on a tracker persist for all hits, until they are
	    // overridden or cleared by assignment to null.
	    getGTracker().set(Fields.SCREEN_NAME, SCREEN_NAME);
		
        aq = new PQuery(this);
        mUuid = getUuid();
        
        // TODO this is temp hack
        if (isTrial) {
        	mUuid = "test@test.com";
        }
        
        // get chan list 
        // TODO: stub
        aq.ajax(PrefUtility.getApiUrl(ServerUtility.API_CHANNELS, "uuid=" + mUuid), JSONArray.class, this, "chanListCb");

//        mAdapter = new ClipPlaylistTabAdapter(getSupportFragmentManager());
//        
//        // Set up the ViewPager, attaching the adapter.
//        mViewPager = (ViewPager) findViewById(R.id.pager);
//        mViewPager.setAdapter(mAdapter);



    }
    
    @Override
    public void onStart(){
    	super.onStart();
		// show help
//		DialogUtility.showHelpOverlay(this);
    }
    
    private void updateUnseenVideosFromServer() {
    	Log.v(LOGTAG, "updateUnseenStoriesFromServer");
    	if (isTrial) {
    		Log.v(LOGTAG, "updateUnseenStoriesFromServer -- skip in trial");
    	}
    	else {
    		aq.ajax(PrefUtility.getApiUrl(ServerUtility.API_UNSEEN_STORIES, "uuid=" + mUuid), JSONArray.class, this, "unseenStoriesCb");
    	}
	}

	public void unseenStoriesCb(String url, JSONArray jarr, AjaxStatus status) 
	{
		Log.v(LOGTAG, "unseenStoriesCb");
		if (jarr != null) {
			// successful ajax call
			try {
				// TODO crappy hack
				int[] stories = new int[jarr.length()];
				for (int i = 0; i < jarr.length(); i++) {
					stories[i] = jarr.getInt(i);
				}
				ArrayMoviesFragment.resetUnseenMovieSet(stories);
				unseenStoriesCount = jarr.length();
				Log.v(LOGTAG, "unseen stories:" + unseenStoriesCount);
				refreshUnseenBadge(unseenStoriesCount);
			} catch (JSONException e) {
				Log.e(LOGTAG, "jsonexception", e);
			}

		} else {
			// ajax error
			ErrorUtility.apiError(LOGTAG,
					"userLikesCb: null Json, could not get unseenStories for uuid " + mUuid, status, this, false, Log.ERROR);
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

        @Override
        public int getCount() {
        	return count;
        }

        @Override
        public Fragment getItem(int position) {
            return ArrayMoviesFragment.newInstance(position, mChannels.get(position).getId(), mUuid, isTrial);
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            return getChannels().get(position % getChannels().size()).getTitle();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
		int idx = getActionBar().getSelectedNavigationIndex();

//    	if (item.getItemId() == R.id.action_join) 
//		{
//			onJoinPressed(null, mChannels.get(idx).getId());
//			return true;
//			 
//		} else 
		if (item.getItemId() == R.id.action_new) 
		{
			 onNewPressed(mChannels.get(idx).getId());
			 return true;
			 
		}
		else if (item.getItemId() == R.id.action_refresh) {
			onRefreshChannel();
			return true;
		}
			
		return super.onOptionsItemSelected(item);
    }
    /*-- helper --*/
    
    private void onRefreshChannel() {
		// TODO Auto-generated method stub
		Log.v(LOGTAG, "onRefreshChannel");
		int pos = getActionBar().getSelectedNavigationIndex();
		ArrayMoviesFragment amf = (ArrayMoviesFragment)findFragmentByPosition(pos);
		amf.updateMovieList();
	}
    
    // a hack?
    // http://stackoverflow.com/questions/11976397/android-getting-fragment-that-is-in-fragmentpageradapter
    public Fragment findFragmentByPosition(int position) {
        
        return getSupportFragmentManager().findFragmentByTag(
                "android:switcher:" + mViewPager.getId() + ":"
                        + mAdapter.getItemId(position));
    }
    /*-- lifecycle --*/

	@Override
    public void onDestroy(){
    	super.onDestroy();
    	aq.dismiss();
    }

	public static void setChannels(List<Channel> mChannels) {
		mChannels = mChannels;
	}

	public static List<Channel> getChannels() {
		if (mChannels==null) {
			Log.e(ArrayMoviesFragment.LOGTAG, "trying to access null channel list!");
		}
		return mChannels;
	}

}

