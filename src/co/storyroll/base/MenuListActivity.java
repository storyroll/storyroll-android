package co.storyroll.base;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import co.storyroll.PQuery;
import co.storyroll.R;
import co.storyroll.activity.*;
import co.storyroll.ui.dialog.SignupDialog;
import co.storyroll.util.AppUtility;
import co.storyroll.util.ErrorUtility;
import co.storyroll.util.PrefUtility;
import co.storyroll.util.ServerUtility;
import com.androidquery.auth.BasicHandle;
import com.androidquery.callback.AjaxStatus;
import com.bugsense.trace.BugSenseHandler;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import org.json.JSONArray;

public abstract class MenuListActivity extends FragmentActivity implements SignupDialog.SigninDialogListener
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

        isTrial = !AppUtility.isLoggedIn();
        Log.d(LOGTAG, "isTrial: "+isTrial);

        aq = new PQuery(this);

        if (!isTrial) {
            isTrial = getUuid() == null;
        }
        if (!isTrial){
           basicHandle = new BasicHandle(getUuid(), getPassword());
        }

        // Setup search by username on Android
		BugSenseHandler.setUserIdentifier(getUuid());
	}

    @Override
    public void onStart() {
        super.onStart();
        // The rest of your onStart() code.
        getGTracker().activityStart(this);  // Add this method.

        // Send a screen view when the Activity is displayed to the user.
        getGTracker().send(MapBuilder.createAppView().build());

        // check for invites and update badge accordingly
        if (!isTrial) {
            updateInvitesFromServer();
        }

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


    
    @Override
    public void onStop() {
      super.onStop();
      // The rest of your onStop() code.
      getGTracker().activityStop(this);  // Add this method.
    }
    
    // ------- protected methods
    
    protected void fireGAnalyticsEvent(String category, String action, String label, Long value) {
    	getGTracker().send(MapBuilder
			    .createEvent(category, action, label, value)
			    .build()
			);
    }
    
    // ------- menus

    static Button notifCountButton = null;
    static MenuItem countMenuItem = null;

    protected void initInviteNotificationsBadge(final Menu menu) {
        countMenuItem = menu.findItem(R.id.action_invitations_badge);
        notifCountButton = (Button) countMenuItem.getActionView().findViewById(R.id.invites_notif_count);
        countMenuItem.getActionView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menu.performIdentifierAction(countMenuItem.getItemId(), 0);
            }
        });

        if (mNotifCount<1){
            countMenuItem.setVisible(false);
        } else {
            countMenuItem.setVisible(true);
        }
        notifCountButton.setText(String.valueOf(mNotifCount));
    }

    protected void updateInvitesBadge(){
        if (notifCountButton!=null) {
            notifCountButton.setText(String.valueOf(mNotifCount));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        Log.v(LOGTAG, "onCreateOptionsMenu");
        if (!isTrial)
        {
            initInviteNotificationsBadge(menu);
        }
        // hide LogCat menu item
        if (!PrefUtility.isTestDevice()) {
            MenuItem logCatMenuItem = menu.findItem(R.id.action_log);
            logCatMenuItem.setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		Intent intent;
		if (item.getItemId() == android.R.id.home) // this will be our left action item
		{
			fireGAnalyticsEvent("ui_action", "touch", "home", null);
			return super.onOptionsItemSelected(item);
			
//		} else if (item.getItemId() == R.id.action_join) 
//		{
//			 onJoinPressed(null, null);
//			 return true;
//
        } else if (item.getItemId() == R.id.action_chan)
        {
            onNewChanPressed(null);
            return true;

//		} else if (item.getItemId()==R.id.action_login) {
//			// TOOD
//			return true;
		} else if (item.getItemId() == R.id.action_help) {
			intent = new Intent (this, HelpActivity.class);
			startActivity(intent);
			return true;
			
		} else if (item.getItemId() == R.id.action_profile) {
			intent = new Intent(this, ProfileActivity.class);
			startActivity(intent);
			return true;
			
		} else if (item.getItemId() == R.id.action_settings) {
			intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;

		} else if (item.getItemId() == R.id.action_log) {
            intent = new Intent(this, LogReadActivity.class);
            startActivity(intent);
            return true;

        } else if (item.getItemId() == R.id.action_invitations_badge) {
            Log.v(LOGTAG, "onOptionsItemSelected:invitations_badge");
            onNewInvitations();
            return true;

        } else if (item.getItemId() == R.id.action_login) {
            intent = new Intent(this, LoginActivity.class);
            intent.putExtra("overrideBackPress", false);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;

        } else {
			return super.onOptionsItemSelected(item);
		}
	}
	
    /*-- callbacks & helpers --*/
    private void onNewInvitations()
    {
        Log.v(LOGTAG, "onNewInvitations");
        Intent invitesIntent = new Intent(getApplicationContext(), InvitesManager.class);
        invitesIntent.putExtra("UUID", getUuid());
        startActivityForResult(invitesIntent, MANAGE_INVITES_REQUEST);
    }


    protected void onNewChanPressed(Long chanId) {
        if (isTrial) {
            fireGAnalyticsEvent("ui_action", "touch", "createChan_trial", null);
            new SignupDialog().show(getSupportFragmentManager(), "SignupDialog");
        }
        else {
            fireGAnalyticsEvent("ui_action", "touch", "createChan_regged", null);
            Intent intent = new Intent(this, ChannelCreateActivity.class);
            startActivity(intent);
        }
    }

	
	protected void onJoinPressed(Long clipId, Long chanId){
		Intent intent;
//		if (isTrial) {
//			fireGAnalyticsEvent("ui_action", "touch", "joinRoll_trial", null);
//			intent = new Intent(this, LoginActivity.class);
//			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//		}
//		else 
		{
			fireGAnalyticsEvent("ui_action", "touch", "joinRoll_regged", null);
			intent = new Intent(this, VideoCaptureActivity.class);
		}
		if (clipId!=null && clipId!=-1L) {
			intent.putExtra("RESPOND_TO_CLIP", clipId);
		}
		if (chanId!=null && chanId!=-1L) {
			intent.putExtra("CURRENT_CHANNEL", chanId);
		}
		
		startActivity(intent);
	}

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.v(LOGTAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, intent);
    }

    protected EasyTracker getGTracker() {
    	return EasyTracker.getInstance(this);
    }
    
	protected String getUuid() {
		SharedPreferences settings = getSharedPreferences(Constants.PREF_PROFILE_FILE, 0);
		String uuid = settings.getString(Constants.PREF_EMAIL, null);
		String username = settings.getString(Constants.PREF_USERNAME, null);
		Log.v(LOGTAG, "uuid: " + uuid + ", username: " + username);
		return uuid;
	}

    protected String getPassword() {
        SharedPreferences settings = getSharedPreferences(Constants.PREF_PROFILE_FILE, 0);
        String password = settings.getString(Constants.PREF_PASSWORD, null);
        return password;
    }

    /*-- lifecycle --*/

    @Override
    public void onDestroy(){
        super.onDestroy();
        aq.dismiss();
    }

    @Override
    public void onDialogSigninClick(DialogFragment dialog)
    {
        // User touched the dialog's Sign-in button
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(LoginActivity.EXTRA_OVERRIDE_BACKPRESS, false);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
