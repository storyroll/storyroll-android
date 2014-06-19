package co.storyroll.base;


import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import co.storyroll.PQuery;
import co.storyroll.R;
import co.storyroll.util.ErrorUtility;
import co.storyroll.util.PrefUtility;
import co.storyroll.util.ServerUtility;
import com.androidquery.auth.BasicHandle;
import com.androidquery.callback.AjaxStatus;
import com.bugsense.trace.BugSenseHandler;
import com.google.analytics.tracking.android.MapBuilder;
import org.json.JSONArray;

public class MenuChannelListActivity extends MenuListActivity
{
	private static final String LOGTAG = "MenuListAct";
    protected static final int MANAGE_INVITES_REQUEST = 1019;
    protected static boolean isTrial=false;
    static int mNotifCount = 0;
    protected PQuery aq;
    protected BasicHandle basicHandle = null;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        isTrial = getIntent().getBooleanExtra("TRIAL", false);
        if (!isTrial) {
            isTrial = getUuid() == null;
        }
        if (!isTrial){
           basicHandle = new BasicHandle(getUuid(), getPassword());
        }
        aq = new PQuery(this);

        // Setup search by username on Android
		BugSenseHandler.setUserIdentifier(getUuid());
	}

    @Override
    public void onStart() {
        super.onStart();
        // The rest of your onStart() code.
        getGTracker().activityStart(this);  // Add this method.

        // check for invites and update badge accordingly
        updateInvitesFromServer();

        // Send a screen view when the Activity is displayed to the user.
        getGTracker().send(MapBuilder.createAppView().build());
    }

    /* ------------- -------------- Helper classes -------------- ------------ */

    protected void updateInvitesFromServer() {
        aq.auth(basicHandle).ajax(PrefUtility.getApiUrl(
                ServerUtility.API_INVITES_PENDING, "uuid=" + getUuid()), JSONArray.class, this, "invitesPendingCb");
    }


    public void invitesPendingCb(String url, JSONArray jarr, AjaxStatus status)  {
        Log.v(LOGTAG, "invitesPendingCb");

        if (status.getCode() != 200 && status.getCode()!=AjaxStatus.TRANSFORM_ERROR) {
            ErrorUtility.apiError(LOGTAG, "Error getting pending invites for uuid="+getUuid(), status, this, false, Log.ERROR);
            return;
        }

        mNotifCount = jarr.length();
        updateInvitesBadge();
        invalidateOptionsMenu();
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
