package com.storyroll.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import com.storyroll.util.PrefUtility;

public class TabbedChannelsActivity extends MenuFragmentActivity {

	private static final String LOGTAG = "TabbedChanListActivity";
	private static final String SCREEN_NAME = "ChanList";

    /**
     * The {@link android.support.v4.view.ViewPager} that will display the object collection.
     */
    ViewPager mViewPager;
    
    ChannelTabAdapter mAdapter;
    FragmentPagerAdapter tAdapter;
    private static PQuery aq;
//    private static Set<String> newStories = null;
    private static String mUuid;
    private int unseenStoriesCount = 0;
    
    public PQuery getPQuery(){
    	return aq;
    }

    public void chanListCb(String url, JSONArray jarr, AjaxStatus status)  {
    	Log.v(LOGTAG, "chanListCb stub");
    	// get the list of channels
    	List<Channel> chans = new ArrayList<Channel>();
    	chans.add(new Channel(0, "Chan1"));
    	chans.add(new Channel(1, "Chan2"));
    	
    	init(chans);
    }
    
    public void init(List<Channel> channels)  {
    	
    	// TODO bad, bad temporary hack
    	ArrayClipsFragment.CHANNELS = channels;
    	
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
	            	fireGAnalyticsEvent("ui_action", "onTabSelected", ArrayClipsFragment.CHANNELS.get(tab.getPosition()).getName(), null);
	            	
	            	Log.v(LOGTAG, "onTabSelected "+tab.getPosition()+" - "+ArrayClipsFragment.CHANNELS.get(tab.getPosition()).getName());
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
        for (int i = 0; i < ArrayClipsFragment.CHANNELS.size(); i++) {
        	if (ArrayClipsFragment.CHANNELS.get(i)!=null) {
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
        		tab = tab.setText(ArrayClipsFragment.CHANNELS.get(i).getName());
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
                	    getGTracker().set(Fields.SCREEN_NAME, SCREEN_NAME+"_"+ArrayClipsFragment.CHANNELS.get(position));
                    }
                });
        
	    // comes from notification? switch to MINE tab
		if (getIntent().getBooleanExtra("NOTIFICATION", false)) 
		{
			// TODO crappy hack
			ArrayClipsFragment.resetUnseenClipSet( getIntent().getIntArrayExtra("clips") );
			refreshUnseenBadge( getIntent().getIntExtra("count", 0) );
//			actionBar.setSelectedNavigationItem(ArrayClipsFragment.TAB_TWO);
		}
		else {
			// update unseenStories
			updateUnseenVideosFromServer();
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
        aq.ajax(PrefUtility.getApiUrl()+"unseenStories?uuid=" + mUuid, JSONArray.class, this, "chanListCb");

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
    		aq.ajax(PrefUtility.getApiUrl()+"unseenStories?uuid=" + mUuid, JSONArray.class, this, "unseenStoriesCb");
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
				ArrayClipsFragment.resetUnseenClipSet(stories);
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
    


	private static final long DOUBLE_PUSH_BACK_MILLIS = 1000;
    private int backTries = 0;
    private Long lastPressed = 0L;
	private TextView tabUnseenBadgeText = null;
    
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
            List<Channel> chans = ArrayClipsFragment.CHANNELS;
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
            return ArrayClipsFragment.newInstance(position, mUuid, isTrial);
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            return ArrayClipsFragment.CHANNELS.get(position % ArrayClipsFragment.CHANNELS.size()).getName();
        }
    }
    
    /*-- lifecycle --*/
    
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	aq.dismiss();
    }

}
