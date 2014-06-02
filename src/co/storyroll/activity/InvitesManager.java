package co.storyroll.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import co.storyroll.PQuery;
import co.storyroll.R;
import co.storyroll.model.Invite;
import co.storyroll.model.Profile;
import co.storyroll.util.ErrorUtility;
import co.storyroll.util.PrefUtility;
import co.storyroll.util.ServerUtility;
import com.androidquery.callback.AjaxStatus;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public final class InvitesManager extends Activity {

    private static final String LOGTAG = "InvitesManager";
    private static final String SCREEN_NAME = "InvitesManager";
    private static ArrayList<Invite> invites = null;

	private InviteAdapter inviteAdapter = null;
	private ListView lv;
	ImageView doneSelect;
    PQuery aq;     // todo remove
    String mUuid;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// remove title
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.invite_manager);

        EasyTracker.getInstance(this).set(Fields.SCREEN_NAME, SCREEN_NAME);

        mUuid = getIntent().getStringExtra("UUID");

        aq = new PQuery(this);

		// Init UI elements
		lv = (ListView) findViewById(R.id.inviteList);
		doneSelect = (ImageView) findViewById(R.id.doneSelect);

		// Register handler for UI elements
		aq.id(R.id.doneSelect).clicked(this, "onDonePressed");

		if (invites == null) {
			invites = new ArrayList<Invite>();
			// Asynchronously load all invites
			doLoadInvites();
		} else {
			inviteAdapter = new InviteAdapter(this, R.id.inviteList, invites);
			lv.setAdapter(inviteAdapter);

		}

	}

    public void onDonePressed(View button){
        onBackPressed();
    }


	// Also on back pressed set the selected list, if nothing selected set Intent result to canceled
	@Override
	public void onBackPressed() {


//        if (selectedList.size() > 0) {
//            intent.putParcelableArrayListExtra("SELECTED_CONTACTS", selectedList);
//            setResult(RESULT_OK, intent);
//        } else {
//            setResult(RESULT_CANCELED, intent);
//        }


		finish();

	};

    private void removeInvitationById(long id){
        ArrayAdapter<Invite> adapter =(ArrayAdapter<Invite>)lv.getAdapter();

        Invite found=null;
        for (int i=0;found==null && i<adapter.getCount();i++) {
            Invite invite = adapter.getItem(i);
            if (invite.getId()==id) {
                found = invite;
            }
        }
        if (found!=null) {
            Log.v(LOGTAG, "removing");
            adapter.remove(found);
            adapter.notifyDataSetChanged();
            if (adapter.getCount()<1) {
                // return
                finish();
            }
        }
    }

	// Invite adapter
	public class InviteAdapter extends ArrayAdapter<Invite> {

		private ArrayList<Invite> inviteList;


		public InviteAdapter(Context context, int textViewResourceId, ArrayList<Invite> items) {
			super(context, textViewResourceId, items);

			this.inviteList = new ArrayList<Invite>();

			this.inviteList.addAll(items);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;

			if (view == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = vi.inflate(R.layout.invite_item, null);
			}
			Invite invite = inviteList.get(position);
			if (invite != null) {
				TextView firstLine = (TextView) view.findViewById(R.id.firstLine);
				ImageView thumb = (ImageView) view.findViewById(R.id.thumb);
				TextView secondLine = (TextView) view.findViewById(R.id.secondLine);

                ImageView accept = (ImageView) view.findViewById(R.id.acceptInvite);
                ImageView reject = (ImageView) view.findViewById(R.id.rejectInvite);

                accept.setOnClickListener(onAccept);
                accept.setTag(invite.getId());
                reject.setOnClickListener(onReject);
                reject.setTag(invite.getId());

                Profile inviter = invite.getInvitingUser();
                aq.id(thumb).image(inviter.getAvatarUrl(),
                        false, false, 0, R.drawable.ic_avatar_default);

                String line1 = "Invitation to join "+invite.getChannel().getTitle().toUpperCase();
                String line2 = "by "+inviter.getUsername() + " ("+inviter.getEmail() +")" ;

				firstLine.setText(line1);
                secondLine.setText(line2);

			}

			return view;
		}



        private View.OnClickListener onAccept = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(LOGTAG, "accept.onClick for id: "+view.getTag());
                String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_INVITES_ACCEPT, "uuid=" + mUuid+"&i="+view.getTag());
                aq.progress(R.id.progress).ajax(apiUrl, JSONArray.class, this, "invitesAcceptCb");
                removeInvitationById((Long) view.getTag());
            }
        };
        private View.OnClickListener onReject = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(LOGTAG, "reject.onClick for id: "+view.getTag());
                String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_INVITES_DECLINE, "uuid=" + mUuid+"&i="+view.getTag());
                aq.progress(R.id.progress).ajax(apiUrl, JSONArray.class, this, "invitesDeclineCb");
                removeInvitationById((Long)view.getTag());
            }
        };
    }

    public void invitesAcceptCb(String url, JSONArray jarr, AjaxStatus status) {
        Log.v(LOGTAG, "invitesPendingCb: " + jarr.toString());

        if (status.getCode() != 200 && status.getCode() != AjaxStatus.TRANSFORM_ERROR) {
            ErrorUtility.apiError(LOGTAG, "invitesAccept JSON error, uuid=" + mUuid, status, this, false, Log.ERROR);
            return;
        }
    }

    public void invitesDeclineCb(String url, JSONArray jarr, AjaxStatus status) {
        Log.v(LOGTAG, "invitesDeclineCb: " + jarr.toString());

        if (status.getCode() != 200 && status.getCode() != AjaxStatus.TRANSFORM_ERROR) {
            ErrorUtility.apiError(LOGTAG, "invitesDecline JSON error, uuid=" + mUuid, status, this, false, Log.ERROR);
            return;
        }
    }

    private void doLoadInvites(){
        String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_INVITES_PENDING, "uuid=" + mUuid);
        aq.progress(R.id.progress).ajax(apiUrl, JSONArray.class, this, "invitesPendingCb");
    }

    public void invitesPendingCb(String url, JSONArray jarr, AjaxStatus status)  {
        Log.v(LOGTAG, "invitesPendingCb: "+jarr.toString());

        if (status.getCode() != 200 && status.getCode()!=AjaxStatus.TRANSFORM_ERROR) {
            ErrorUtility.apiError(LOGTAG, "Error getting pending invites for uuid=" + mUuid, status, this, false, Log.ERROR);
            return;
        }

        // get list of invites
        try {
            invites.clear();
            invites.addAll(Invite.invites(jarr));
            Log.v(LOGTAG, "invites got vs loaded: "+jarr.length()+", "+invites.size());

            inviteAdapter = new InviteAdapter(this, R.id.inviteList, invites);
            inviteAdapter.notifyDataSetChanged();
            // set contact adapter
            lv.setAdapter(inviteAdapter);

        } catch (JSONException e) {
           ErrorUtility.apiError(LOGTAG, e.getMessage(), null, this, false, Log.ERROR);
        }

    }


    /*-- lifecycle --*/
    @Override
    public void onDestroy(){
        super.onDestroy();
        aq.dismiss();
    }
}
