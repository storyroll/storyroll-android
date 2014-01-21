package com.storyroll.activity;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.storyroll.PQuery;
import com.storyroll.R;
import com.storyroll.base.Constants;
import com.storyroll.base.MenuFragmentActivity;
import com.storyroll.util.ActionBarUtility;

public class TabbedPlaylistActivity extends MenuFragmentActivity {

	private static final String LOGTAG = "TabbedPlaylist";

    /**
     * The {@link android.support.v4.view.ViewPager} that will display the object collection.
     */
    ViewPager mViewPager;
    
    RollPlaylistTabAdapter mAdapter;
    FragmentPagerAdapter tAdapter;
    private static PQuery aq;
    private static String uuid;
    
    public PQuery getPQuery(){
    	return aq;
    }

    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabbed_playlist);
        
        aq = new PQuery(this);
        uuid = getUuid();
        
        mAdapter = new RollPlaylistTabAdapter(getSupportFragmentManager());
        
        // Set up the ViewPager, attaching the adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mAdapter);

        // Set up action bar.
    	ActionBarUtility.initCustomActionBar(this);
    	final ActionBar actionBar = getActionBar();
    	actionBar.setIcon(R.drawable.ic_action_join);
    	actionBar.setHomeButtonEnabled(true);
        
        // Specify that tabs should be displayed in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        // TODO: custom underline?
        actionBar.setStackedBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.sr_actionbar_tab_underline)));


        // Create a tab listener that is called when the user changes tabs.
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {


			@Override
			public void onTabReselected(Tab tab, FragmentTransaction ft) {
				// TODO Auto-generated method stub
				
			}

            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                // When the tab is selected, switch to the
                // corresponding page in the ViewPager.
                mViewPager.setCurrentItem(tab.getPosition());
            }

			@Override
			public void onTabUnselected(Tab tab, FragmentTransaction ft) {
				// TODO Auto-generated method stub
				
			}
        };

        // Add 3 tabs, specifying the tab's text and TabListener
        for (int i = 0; i < 4; i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(ArrayListFragment.TAB_HEADINGS[i])
                            .setTabListener(tabListener));
        }

        mViewPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        // When swiping between pages, select the
                        // corresponding tab.
                        getActionBar().setSelectedNavigationItem(position);
                    }
                });


    }


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
        public RollPlaylistTabAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return ArrayListFragment.TAB_HEADINGS.length;
        }

        @Override
        public Fragment getItem(int position) {
            return ArrayListFragment.newInstance(position, uuid);
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
    
    /*-- callbacks & helpers --*/

    
	protected String getUuid() {
		SharedPreferences settings = getSharedPreferences(Constants.PREF_PROFILE_FILE, 0);
		String uuid = settings.getString(Constants.PREF_EMAIL, null);
		String username = settings.getString(Constants.PREF_USERNAME, null);
		Log.i(LOGTAG, "uuid: " + uuid + ", username: " + username);
		return uuid;
	}
}

