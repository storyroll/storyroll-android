package co.storyroll.base;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import co.storyroll.PQuery;
import co.storyroll.R;
import co.storyroll.activity.*;
import co.storyroll.util.ErrorUtility;
import co.storyroll.util.PrefUtility;
import co.storyroll.util.ServerUtility;
import com.androidquery.callback.AjaxStatus;
import com.bugsense.trace.BugSenseHandler;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import org.json.JSONArray;

public class MenuFragmentActivity extends FragmentActivity {
	
	private static final String LOGTAG = "MenuFragment";
    protected static final int MANAGE_INVITES_REQUEST = 1019;
    protected static boolean isTrial=false;
    static int mNotifCount = 0;
    protected PQuery aq;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isTrial = getIntent().getBooleanExtra("TRIAL", false);
        if (!isTrial) {
            isTrial = getUuid() == null;
        }

        aq = new PQuery(this);

        updateInvitesFromServer();

        // Setup search by username on Android
		BugSenseHandler.setUserIdentifier(getUuid());
	}

    protected void updateInvitesFromServer() {
        aq.ajax(PrefUtility.getApiUrl(
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
    public void onStart() {
      super.onStart();
      // The rest of your onStart() code.
      getGTracker().activityStart(this);  // Add this method.
      
      // Send a screen view when the Activity is displayed to the user.
      getGTracker().send(MapBuilder.createAppView().build());
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

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        Log.v(LOGTAG, "onCreateOptionsMenu");
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        if (isTrial) {
        	inflater.inflate(R.menu.home_activity_menu, menu);
//           	inflater.inflate(R.menu.trial_activity_menu, menu);
                   }
        else {
        	inflater.inflate(R.menu.home_activity_menu, menu);

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
        return super.onCreateOptionsMenu(menu);
    }

    private void updateInvitesBadge(){
        if (notifCountButton!=null) {
            notifCountButton.setText(String.valueOf(mNotifCount));
        }
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
		} else if (item.getItemId() == R.id.action_new) 
		{
			 onNewPressed(null);
			 return true;

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


	protected void onNewPressed(Long chanId) {
		Intent intent;
		if (isTrial) {
			fireGAnalyticsEvent("ui_action", "touch", "joinRoll_trial", null);
			intent = new Intent(this, LoginActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		}
		else {
			fireGAnalyticsEvent("ui_action", "touch", "joinRoll_regged", null);
			intent = new Intent(this, VideoCaptureActivity.class);
			intent.putExtra("MODE_NEW", true);
			if (chanId!=null && chanId!=-1L) {
				intent.putExtra("CURRENT_CHANNEL", chanId);
			}
		}
		
		startActivity(intent);
	}

    protected void onNewChanPressed(Long chanId) {
        Intent intent;
        if (isTrial) {
            fireGAnalyticsEvent("ui_action", "touch", "createChan_trial", null);
            intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        else {
            fireGAnalyticsEvent("ui_action", "touch", "createChan_regged", null);
            intent = new Intent(this, ChannelCreateActivity.class);
        }
        startActivity(intent);
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

    /*-- lifecycle --*/

    @Override
    public void onDestroy(){
        super.onDestroy();
        aq.dismiss();
    }
	
}
