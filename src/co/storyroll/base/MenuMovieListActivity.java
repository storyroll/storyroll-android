package co.storyroll.base;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import co.storyroll.R;

/**
 * Created by martynas on 18/06/14.
 */
public class MenuMovieListActivity extends MenuListActivity {

    private static final String LOGTAG = "MenuChanListActivity";

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
