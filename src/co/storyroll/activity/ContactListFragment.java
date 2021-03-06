package co.storyroll.activity;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import co.storyroll.R;
import co.storyroll.adapter.ContactAdapter;
import co.storyroll.model.Contact;
import co.storyroll.tasks.AsyncLoadContacts;
import co.storyroll.ui.dialog.MatchFriendsDialog;
import co.storyroll.util.DataUtility;
import co.storyroll.util.ErrorUtility;
import co.storyroll.util.PrefUtility;
import co.storyroll.util.ServerUtility;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.bugsense.trace.BugSenseHandler;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by martynas on 12/06/14.
 */
public class ContactListFragment extends ListFragment
        implements AsyncLoadContacts.LoadContactsListener, MatchFriendsDialog.MatchFriendsDialogListener{
    private static final String LOGTAG = "TabListFragment";
    int mTabNum;
    public static ArrayList<Contact> friendContacts = new ArrayList<Contact>();
    public static ArrayList<Contact> phoneContacts = new ArrayList<Contact>();


    /**
     * Create a new instance of CountingFragment, providing "num"
     * as an argument.
     */
    static ContactListFragment newInstance(int num) {
        ContactListFragment f = new ContactListFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putInt("num", num);

        f.setArguments(args);

        return f;
    }

    /**
     * When creating, retrieve this instance's number from its arguments.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mTabNum = getArguments() != null ? getArguments().getInt("num") : 1;
    }

    /**
     * The Fragment's UI is just a simple text view showing its
     * instance number.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        Log.v(LOGTAG, "creating view for tab num " + mTabNum);

        if (mTabNum == ContactManagerActivity.TAB_2_FEMAIL) {
            // Special fragment
            final View v = inflater.inflate(R.layout.fragment_invite_email, container, false);
            return v;
        }

        final View v = inflater.inflate(R.layout.fragment_pager_list, container, false);

        // Init UI elements
        EditText myFilter = (EditText) v.findViewById(R.id.search_txt);
        myFilter.setSelected(false);
        myFilter.setFocusable(true);
        myFilter.setFocusableInTouchMode(true);
        myFilter.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // call the filter with the current text on the editbox
                ((ContactAdapter) getListAdapter()).getFilter().filter(s.toString());
            }
        });

        if (mTabNum ==ContactManagerActivity.TAB_0_FRIENDS)
        {
//            friendMatchBtn.setVisibility(View.VISIBLE);
//            friendMatchBtn.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
////                    new MatchFriendsDialog(ContactListFragment.this, v).show(getActivity().getSupportFragmentManager(), "MatchFriendsDialog");
//                    onUsersMatchClicked(view);
//                }
//            });
        }
        else {
            // Auto-select filter field, and show soft keyboard

        }

        return v;
    }
//    ContactAdapter contactAdapter;
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//            setListAdapter(new ArrayAdapter<String>(getActivity(),
//                    android.R.layout.simple_list_item_1, friendList));
        if (mTabNum ==ContactManagerActivity.TAB_0_FRIENDS)
        {
            getFriendsFromServer();

//            Log.v(LOGTAG, "friendListAdapter");
//            Collections.sort(friendContacts);
//            contactAdapter = new ContactAdapter(getActivity(), friendContacts);

//            friendListAdapter = new ContactAdapter(AddressTabsActivity.this, friendContacts);
        }
        else if (mTabNum ==ContactManagerActivity.TAB_1_ADDRESSBOOK)
        {
            Log.v(LOGTAG, "contactAdapter");
            ContactAdapter contactAdapter = new ContactAdapter(getActivity(), phoneContacts);

            if (phoneContacts == null || phoneContacts.size()<1)
            {
                phoneContacts = new ArrayList<Contact>();
                // Asynchronously load all contacts
                ContactManagerActivity.swipeContainer.setRefreshing(true);
                AsyncLoadContacts contactLoaderTask = new AsyncLoadContacts(1, this, getActivity());
                contactLoaderTask.execute(); // will result on interface call onContactsLoaded(), see below
            }
            else {
                setListAdapter(contactAdapter);
            }
        }
    }

    @Override
    public void onContactsLoaded(int tabNum) {
        switch (tabNum){
            case ContactManagerActivity.TAB_0_FRIENDS:
                // this comes from onUsersMatchClicked, update the list
                doServerUsersMatchCall();
                break;

            case ContactManagerActivity.TAB_1_ADDRESSBOOK:
                // this comes from PhoneBook fragment initialization
                setListAdapter(new ContactAdapter(getActivity(), phoneContacts));
                ((BaseAdapter)getListAdapter()).notifyDataSetChanged();
                ContactManagerActivity.swipeContainer.setRefreshing(false);
                break;
            default:
                Log.e(LOGTAG, "Unexpected callback for tabNum "+tabNum);
                break;
        }

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Log.i("FragmentList", "Item clicked: " + id);
    }

    protected void getFriendsFromServer()
    {
        ((ContactManagerActivity)getActivity()).getAQuery().auth(PrefUtility.getBasicHandle()).ajax(PrefUtility.getApiUrl(
                ServerUtility.API_USER_FRIENDS, "uuid=" + PrefUtility.getUuid()), JSONArray.class, this, "userFriendsCb");
    }

    public void userFriendsCb(String url, JSONArray jarr, AjaxStatus status) {
        Log.v(LOGTAG, "userFriendsCb");

        if (status.getCode() != 200 && status.getCode()!=AjaxStatus.TRANSFORM_ERROR) {
            ErrorUtility.apiError(LOGTAG, "Error getting friends", status, getActivity(), true, Log.ERROR);
            return;
        }
        Log.v(LOGTAG, "got friends: "+jarr.length());

        // update list
        friendContacts.clear();
        for (int i=0;i<jarr.length();i++) { //todo

            JSONObject jo = null;
            try {
                jo = jarr.getJSONObject(i);
                friendContacts.add(new Contact(jo));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Collections.sort(friendContacts);
        setListAdapter(new ContactAdapter(getActivity(), friendContacts));
        ((BaseAdapter)getListAdapter()).notifyDataSetChanged();

    }

    // ------------------------------------- MATCH workflow

    private void onUsersMatchClicked()
    {
        Log.v(LOGTAG, "onUsersMatchClicked");
        // addressbook contact loader
        if (ContactManagerActivity.contactLoaderTask==null || ContactManagerActivity.contactLoaderTask.getStatus()!= AsyncTask.Status.RUNNING)
        {
            Log.v(LOGTAG, "ContactLoadTask not running");

            if ((phoneContacts == null || phoneContacts.size()<1))
            {
                phoneContacts = new ArrayList<Contact>();
                // Asynchronously load all contacts
                ContactManagerActivity.contactLoaderTask = new AsyncLoadContacts(0, this, getActivity());
                ContactManagerActivity.contactLoaderTask.execute(); // will result on interface call onContactsLoaded(), see below
            }
            else {
                doServerUsersMatchCall();
            }
        }
        else {
            // Load task is running, wait to complete
            Log.v(LOGTAG, "waiting for ContactLoadTask to complete");
            // wait for the task to complete for at most 10 sec
            try {
                ContactManagerActivity.contactLoaderTask.get(10, TimeUnit.SECONDS);
                doServerUsersMatchCall();
            }
            catch (InterruptedException e) {
                Log.w(LOGTAG, "InterruptedException", e);
            } catch (ExecutionException e) {
                Log.w(LOGTAG, "ExecutionException", e);
            } catch (TimeoutException e) {
                Log.e(LOGTAG, "TimeoutException", e);
                BugSenseHandler.sendException(e);
            }
        }

    }



    class MatchServieCallback extends AjaxCallback<JSONArray>
    {
        @Override
        public void callback(String url, JSONArray jarr, AjaxStatus status)
        {
            if (ErrorUtility.isAjaxErrorThenReport(LOGTAG, status, getActivity())) return;

            Log.v(LOGTAG, "callback result: " + jarr.length());

            friendContacts.clear();
            for( int i=0; i<jarr.length(); i++ ) {
                try {
                    friendContacts.add(new Contact(jarr.getJSONObject(i)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            Collections.sort(friendContacts);
            Log.v(LOGTAG, "after upload: " + friendContacts.size());
//                Collections.sort(friendContacts);
//                setListAdapter(new ContactAdapter(getActivity(), contactList)); // todo optimizieren
            ((BaseAdapter)getListAdapter()).notifyDataSetChanged();
        }
    }

    private void doServerUsersMatchCall()
    {
        JSONArray idsJson = new JSONArray();
        TelephonyManager tMgr = (TelephonyManager)getActivity().getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        String defaultCountryCode = tMgr.getNetworkCountryIso().toUpperCase();
//        for(Contact c:phoneContacts) {
        for(Contact c:AsyncLoadContacts.allContacts.values()) // todo remove dep on static refs
        {
            if ( !TextUtils.isEmpty(c.getContactEmail()) )
                idsJson.put( DataUtility.md5(c.getContactEmail()) );

            String num =  c.getContactNumber();
            if ( !TextUtils.isEmpty(num) )
            {
                idsJson.put( DataUtility.md5(num) );
                String intNum = DataUtility.getInternationalPhoneNumber(num, defaultCountryCode);
                if (!num.equals(intNum))
                    idsJson.put( DataUtility.md5(intNum) );
            }
        }
        Log.v(LOGTAG, "uploading "+idsJson.length()+ " strings");
        String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_USERS_MATCH, "uuid=" + PrefUtility.getUuid());

        int k = 0;
        AjaxCallback ac =  new MatchServieCallback();

        try {
            Log.v(LOGTAG, "strings: "+idsJson.toString());
            StringEntity entity = new StringEntity(idsJson.toString());
            ((ContactManagerActivity)getActivity()).getAQuery().auth(PrefUtility.getBasicHandle())
                    .post(apiUrl, "application/json", entity, JSONArray.class, ac);
        }
        catch (UnsupportedEncodingException e)
        {
            BugSenseHandler.sendException(e);
            e.printStackTrace();
        }
    }

    //-------------------------------------- -------------------------------------- --------------------------------------
    private LinkedHashMap<String, Contact> allContacts = new LinkedHashMap<String, Contact>();


    @Override
    public void onMachFriendsConfirm(DialogFragment dialog) {
        Log.v(LOGTAG, "Fragment received Dialog confirmation");
        onUsersMatchClicked();
    }
}