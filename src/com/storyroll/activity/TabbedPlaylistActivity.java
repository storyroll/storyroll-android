package com.storyroll.activity;

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
import android.widget.Toast;

import com.androidquery.callback.AjaxStatus;
import com.google.analytics.tracking.android.Fields;
import com.storyroll.PQuery;
import com.storyroll.R;
import com.storyroll.base.MenuFragmentActivity;
import com.storyroll.util.ActionBarUtility;
import com.storyroll.util.AppUtility;
import com.storyroll.util.DialogUtility;
import com.storyroll.util.ErrorUtility;
import com.storyroll.util.PrefUtility;

public class TabbedPlaylistActivity extends MenuFragmentActivity {

	private static final String LOGTAG = "TabbedPlaylist";
	private static final String SCREEN_NAME = "TabbedPlaylist";

    /**
     * The {@link android.support.v4.view.ViewPager} that will display the object collection.
     */
    ViewPager mViewPager;
    
    RollPlaylistTabAdapter mAdapter;
    FragmentPagerAdapter tAdapter;
    private static PQuery aq;
//    private static Set<String> newStories = null;
    private static String mUuid;
    private int unseenStoriesCount = 0;
    
    public PQuery getPQuery(){
    	return aq;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabbed_playlist);
        
		// Fields set on a tracker persist for all hits, until they are
	    // overridden or cleared by assignment to null.
	    getGTracker().set(Fields.SCREEN_NAME, SCREEN_NAME);
		
        aq = new PQuery(this);
        mUuid = getUuid();
        // TODO this is temp hack
        if (isTrial) {
        	mUuid = "test@test.com";
        }
        
        mAdapter = new RollPlaylistTabAdapter(getSupportFragmentManager());
        
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
            	
            	fireGAnalyticsEvent("ui_action", "onTabSelected", ArrayListFragment.TAB_HEADINGS[tab.getPosition()], null);
            	
            	Log.v(LOGTAG, "onTabSelected "+tab.getPosition()+" - "+ArrayListFragment.TAB_HEADINGS[tab.getPosition()]);
                mViewPager.setCurrentItem(tab.getPosition());
            }

			@Override
			public void onTabUnselected(Tab tab, FragmentTransaction ft) {
				// TODO Auto-generated method stub
			}
        };

        // Add tabs, specifying the tab's text and TabListener
        String[] tabHeads = ArrayListFragment.TAB_HEADINGS;
        if (isTrial) {
        	tabHeads = ArrayListFragment.TAB_HEADINGS_TRIAL;
        }
        for (int i = 0; i < 4; i++) {
        	if (tabHeads[i]!=null) {
        		Tab tab = actionBar.newTab();
        		if (i==ArrayListFragment.TAB_MINE) 
        		{
        			tab = tab.setText(tabHeads[i]);
        			
        			tab.setCustomView(R.layout.custom_actionbar_tab);
        			View tabView = tab.getCustomView();
        			TextView tabText  = (TextView) tabView.findViewById(R.id.tabText);
        			tabText.setText(tabHeads[i]);
        			
        			tabUnseenBadgeText  = (TextView) tabView.findViewById(R.id.badgeTextt);

        		}
        		else {
        			tab = tab.setText(tabHeads[i]);
        		}
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
                        if (position == ArrayListFragment.TAB_MINE) {
                        	updateUnseenStoriesFromServer();
                        }
                        
                        // Also update tracker field to persist for all subsequent hits,
                		// until they are overridden or cleared by assignment to null.
                	    getGTracker().set(Fields.SCREEN_NAME, SCREEN_NAME+"_"+ArrayListFragment.TAB_HEADINGS[position]);
                    }
                });
        
	    // comes from notification? switch to MINE tab
		if (getIntent().getBooleanExtra("NOTIFICATION", false)) 
		{
			// TODO crappy hack
			ArrayListFragment.resetUnseenStoriesSet( getIntent().getIntArrayExtra("stories") );
			refreshUnseenBadge( getIntent().getIntExtra("count", 0) );
			actionBar.setSelectedNavigationItem(ArrayListFragment.TAB_MINE);
		}
		else {
			// update unseenStories
			updateUnseenStoriesFromServer();
		}

    }
    
    @Override
    public void onStart(){
    	super.onStart();
		// show help
//		DialogUtility.showHelpOverlay(this);
    }
    
//    private void resetNewStoriesSet(int[] stories) {
//    	if (newStories == null) {
//    		newStories = new HashSet<String>();
//    	}
//    	else {
//    		newStories.clear();
//    	}
//    	if (stories!=null) {
//    		for (int i : stories) newStories.add(i+"");
//    	}
//    }
//    
    private void updateUnseenStoriesFromServer() {
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
				ArrayListFragment.resetUnseenStoriesSet(stories);
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
        	
        	long currentTime = System.currentTimeMillis();
        	long tDiff = currentTime - lastPressed;
        	if (tDiff>DOUBLE_PUSH_BACK_MILLIS) {
        		backTries = 0;
        	}
        	if (backTries++>0) {
        		// on second press, exit
        		
        		fireGAnalyticsEvent("ui_action", "click", "Back", 1L);
        			    
        		Intent intent = new Intent(Intent.ACTION_MAIN);
        		intent.addCategory(Intent.CATEGORY_HOME);
        		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        		intent.putExtra("EXIT", true);
        		startActivity(intent);
        	}
        	else {
        		// on first press, show note
        		Toast.makeText(this, "To exit, press back twice", Toast.LENGTH_SHORT).show();
        		
        		fireGAnalyticsEvent("ui_action", "click", "Back", 2L);
        	}
        	lastPressed = currentTime;
        	
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
    
    public static class RollPlaylistTabAdapter extends FragmentPagerAdapter {
    	private int count;
    	
        public RollPlaylistTabAdapter(FragmentManager fm) {
            super(fm);
            String[] th = ArrayListFragment.TAB_HEADINGS;
            if (isTrial) {
            	th = ArrayListFragment.TAB_HEADINGS_TRIAL;
            }
        	count = 0;
    		for(int i=0;i<th.length;i++) {
    			if (th[i]!=null) count++;
    		}
        }

        @Override
        public int getCount() {
        	return count;
        }

        @Override
        public Fragment getItem(int position) {
            return ArrayListFragment.newInstance(position, mUuid, isTrial);
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            return ArrayListFragment.TAB_HEADINGS[position % ArrayListFragment.TAB_HEADINGS.length];
        }
    }
    
    /*-- lifecycle --*/
    
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	aq.dismiss();
    }

}

