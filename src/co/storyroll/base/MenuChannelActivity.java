package co.storyroll.base;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import co.storyroll.R;
import co.storyroll.util.ActionBarUtility;

/**
 * Created by martynas on 18/06/14.
 */
public class MenuChannelActivity extends MenuListActivity {

    private static final String LOGTAG = "MenuChanAct";

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Initial set up for action bar.
        ActionBarUtility.initCustomActionBar(this, true);
    }

    @Override
    public void onStart() {
        super.onStart();
        getGTracker().activityStart(this);  // Add this method. // todo is this needed here?
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        Log.v(LOGTAG, "onCreateOptionsMenu");
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        if (isTrial)
        {
            Log.v(LOGTAG, "initializing trial menu");
            inflater.inflate(R.menu.trial_movies_menu, menu);
        }
        else {
            inflater.inflate(R.menu.home_movies_menu, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }
}
