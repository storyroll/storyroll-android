package co.storyroll.base;


import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import co.storyroll.R;
import co.storyroll.util.ActionBarUtility;

/*
 * This activity adds Main Home (Channel List) menu
 */
public class MenuChannelListActivity extends MenuListActivity
{
	private static final String LOGTAG = "MenuChanListAct";

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Initial set up for action bar.
        ActionBarUtility.initCustomActionBar(this, false);
	}

    @Override
    public void onStart() {
        super.onStart();
        getGTracker().activityStart(this);  // Add this method. // todo is this needed here?
    }

    // ------- menus

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        Log.v(LOGTAG, "onCreateOptionsMenu");
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        if (isTrial)
        {
            Log.v(LOGTAG, "initializing trial menu");
           	inflater.inflate(R.menu.trial_channels_menu, menu);
        }
        else {
        	inflater.inflate(R.menu.home_channels_menu, menu);
        }
        // hide LogCat menu item
        return super.onCreateOptionsMenu(menu);
    }
}
