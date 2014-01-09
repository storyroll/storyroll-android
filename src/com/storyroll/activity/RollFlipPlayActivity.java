
package com.storyroll.activity;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.DirectionalViewPager;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.androidquery.callback.AjaxStatus;
import com.storyroll.PQuery;
import com.storyroll.R;
import com.storyroll.base.Constants;
import com.storyroll.util.ActionBarUtility;
import com.storyroll.util.AppUtility;

/**
 * Demonstrates a "screen-slide" animation using a {@link ViewPager}. Because {@link ViewPager}
 * automatically plays such an animation when calling {@link ViewPager#setCurrentItem(int)}, there
 * isn't any animation-specific code in this sample.
 *
 * <p>This sample shows a "next" button that advances the user to the next step in a wizard,
 * animating the current screen out (to the left) and the next screen in (from the right). The
 * reverse animation is played when the user presses the "previous" button.</p>
 *
 * @see ScreenSlidePageFragment
 */
public class RollFlipPlayActivity extends FragmentActivity {
	private final static String LOGTAG = "ScreenSlidePagerActivity";
    /**
     * The number of pages to scroll.
     */
    private static final int NUM_PAGES = 5;

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private DirectionalViewPager mPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter mPagerAdapter;
    private Integer lastPosition = 0;
    private PQuery aq;
	private ArrayList<Integer> storyIds = new ArrayList<Integer>();
    
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roll_slide);
        
        
        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (DirectionalViewPager) findViewById(R.id.pager);
        
        getWindow().setFlags(
				WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

        aq = new PQuery(this);

        mPagerAdapter = new ScreenSlidePagerAdapter(getFragmentManager());

        // set custom action bar
    	ActionBarUtility.initCustomActionBar(this);
    	ActionBar ab = getActionBar();
    	ab.setIcon(R.drawable.ic_action_join);
		ab.setHomeButtonEnabled(true);
        
        // get your list of stories to scroll
//        String apiUrl = AppUtility.API_URL + "getUserStories?uuid="+getUuid();
        String apiUrl = AppUtility.API_URL + "getLatestPublishedStories?limit="+NUM_PAGES;

		aq.progress(R.id.progress).ajax(apiUrl, JSONArray.class, this, "getStoryListCb");
    }

	
	
	// - - - callbacks & helpers 
    public void getStoryListCb(String url, JSONArray jarr, AjaxStatus status) {
		Log.v(LOGTAG, "getStoryListCb");
        if(jarr != null){               
            //successful ajax call
        	Log.i(LOGTAG, "getStoryListCb success: "+jarr.toString());
        	// do something with the jsonarray
    		try {
	        	for (int i = 0; i < jarr.length(); i++) {
					JSONObject storyObj = jarr.getJSONObject(i);
					// Re https://www.assembla.com/spaces/storyroll/tickets/14#/activity/ticket:
					if (storyObj.getBoolean("published")) {
						storyIds.add(storyObj.getInt("id"));
					}
					Log.v(LOGTAG, "storyObj:"+i+":"+storyObj.get("id"));
	        	}
	        	Log.v(LOGTAG, "stories:"+storyIds.size());
	        	// initialize the adapter now
	            
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		
        }else{          
            //ajax error
        	Log.e(LOGTAG, "getStoryListCb: null Json, could not get stories for uuid="+getUuid());
        }
        if (storyIds.size()<1) {
            Toast.makeText(aq.getContext(), R.string.no_rolls_message, Toast.LENGTH_SHORT).show();
        }
        
        // the idea is that adapter is set only once it's been loaded
        mPager.setAdapter(mPagerAdapter);
        mPager.setOnPageChangeListener(new DirectionalViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
            	super.onPageSelected(position);
        		cache.get(position).queueStartVideo();
        		Log.v(LOGTAG, "onPageSelected: "+position);
    			cache.get(lastPosition).stopVideo();
        		lastPosition = position;
                // When changing pages, reset the action bar actions since they are dependent
                // on which page is currently active. An alternative approach is to have each
                // fragment expose actions itself (rather than the activity exposing actions),
                // but for simplicity, the activity provides the actions in this sample.
                invalidateOptionsMenu();

            }
        });
        // TODO: temp fix, to get rid of that CNF exception
        // android bug
        // http://code.google.com/p/android/issues/detail?id=37484#c1
        mPager.setSaveEnabled(false);
    }
            
	
    public PQuery getPQuery(){
    	return aq;
    }

//    private final static int storyId = 5;
    private ScreenSlidePageFragment lastFragment = null;
    HashMap<Integer, ScreenSlidePageFragment> cache = new HashMap<Integer, ScreenSlidePageFragment>();
    /**
     * A simple pager adapter that represents 5 {@link ScreenSlidePageFragment} objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
    	
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return storyIds.size();
        }

        @Override
        public Fragment getItem(int position) {

	    	ScreenSlidePageFragment fragment;
	    	Log.v(LOGTAG, "ScreenSlidePagerAdapter.getItem: "+position);
	    	if (cache.containsKey(position)) {
	    		fragment = cache.get(position);
	    	}
	    	else{
	    		fragment = ScreenSlidePageFragment.create(position, storyIds.get(position), getUuid());
	    		cache.put(position, fragment);
	    		if (position==0) {
	    			fragment.queueStartVideo();
	    		}
	    	}
	    	lastFragment = fragment;
	    	return fragment;
		}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
	  // Handle item selection
	  Intent intent;
	  if (item.getItemId() == android.R.id.home) // this will be our left action item 
	  {
		// go to Login?
			intent = new Intent(this, VideoCaptureActivity.class);
			//					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
          return true;
	  } 
	  else
//	    if (item.getItemId() == R.id.action_join) {
//			// go to Login
//			intent = new Intent(this, VideoCaptureActivity.class);
//			//					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//			startActivity(intent);
//			return true;
//		} else 
				if (item.getItemId() == R.id.action_profile) {
			intent = new Intent (this, ProfileActivity.class);
			startActivity(intent);
			return true;
		} else if (item.getItemId() == R.id.action_settings) {
			intent = new Intent (this, SettingsActivity.class);
			startActivity(intent);
			return true;
		} else if (item.getItemId() == R.id.action_rolls_favs || item.getItemId() == R.id.action_rolls_my
				|| item.getItemId() == R.id.action_rolls_new || item.getItemId() == R.id.action_rolls_top) {
			intent = new Intent (this, RollFlipPlayActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
    }
    
	protected String getUuid() {
		SharedPreferences settings = getSharedPreferences(Constants.PREF_PROFILE_FILE, 0);
		String uuid = settings.getString(Constants.PREF_EMAIL, null);
		String username = settings.getString(Constants.PREF_USERNAME, null);
		Log.i(LOGTAG, "uuid: " + uuid + ", username: " + username);
		return uuid;
	}
	
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	aq.dismiss();
    }

}
