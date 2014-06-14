package co.storyroll.activity;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import co.storyroll.R;
import co.storyroll.model.Contact;
import co.storyroll.tasks.AsyncLoadContacts;
import co.storyroll.ui.MatchFriendsDialog;
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

/**
 * Created by martynas on 12/06/14.
 */
public class ContactListFragment extends ListFragment
        implements AsyncLoadContacts.LoadContactsListener, MatchFriendsDialog.MatchFriendsDialogListener{
    private static final String LOGTAG = "TabListFragment";
    int mNum;
    private ArrayList<Contact> contactList = new ArrayList<Contact>();
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
        mNum = getArguments() != null ? getArguments().getInt("num") : 1;
    }

    /**
     * The Fragment's UI is just a simple text view showing its
     * instance number.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        Log.v(LOGTAG, "creating view for tab num " + mNum);
        final View v = inflater.inflate(R.layout.fragment_pager_list, container, false);
        if (mNum==0)
        {
            getFriendsFromServer();
            ImageButton friendMatchBtn = (ImageButton) v.findViewById(R.id.findFriendsBtn);
            friendMatchBtn.setVisibility(View.VISIBLE);
            friendMatchBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new MatchFriendsDialog(ContactListFragment.this, v).show(getActivity().getSupportFragmentManager(), "MatchFriendsDialog");
//                    onUsersMatchClicked();
                }
            });
        }
        else {

        }
//            ListView lv;
        EditText myFilter;
        // Init UI elements
//            lv = (ListView) v.findViewById(R.id.contactList);
        myFilter = (EditText) v.findViewById(R.id.search_txt);

        ProgressBar progress = (ProgressBar)v.findViewById(R.id.progress);

        return v;
    }
//    ContactAdapter contactAdapter;
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//            setListAdapter(new ArrayAdapter<String>(getActivity(),
//                    android.R.layout.simple_list_item_1, friendList));
        ContactAdapter contactAdapter;
        if (mNum==0)
        {
            Log.v(LOGTAG, "friendListAdapter");
            Collections.sort(contactList);
            contactAdapter = new ContactAdapter(getActivity(), contactList);

//            friendListAdapter = new ContactAdapter(AddressTabsActivity.this, friendContacts);
        }
        else
        {
            Log.v(LOGTAG, "contactAdapter");
            contactAdapter = new ContactAdapter(getActivity(), phoneContacts);

            if (phoneContacts == null || phoneContacts.size()<1)
            {
                phoneContacts = new ArrayList<Contact>();
                // Asynchronously load all contacts
                AsyncLoadContacts contactLoaderTask = new AsyncLoadContacts(1, this, getActivity(), null);
                contactLoaderTask.execute(); // will result on interface call onContactsLoaded(), see below
            }
        }
        setListAdapter(contactAdapter);

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
        contactList.clear();
        for (int i=0;i<jarr.length();i++) { //todo

            JSONObject jo = null;
            try {
                jo = jarr.getJSONObject(i);
                contactList.add(new Contact(jo));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Collections.sort(contactList);
//        setListAdapter(new ContactAdapter(getActivity(), contactList));
        ((BaseAdapter)getListAdapter()).notifyDataSetChanged();

    }

    // ------------------------------------- MATCH workflow

    private void onUsersMatchClicked(View v)
    {
        Log.v(LOGTAG, "onUsersMatchClicked");
        // addressbook contact loader
        if (phoneContacts == null || phoneContacts.size()<1)
        {
            phoneContacts = new ArrayList<Contact>();
            // Asynchronously load all contacts
            AsyncLoadContacts contactLoaderTask = new AsyncLoadContacts(0, this, getActivity(), v);
            contactLoaderTask.execute(); // will result on interface call onContactsLoaded(), see below
        }
        else {
            usersMatchServerCall(v);
        }
    }

    @Override
    public void onContactsLoaded(int tabNum, View v) {
        if (tabNum==0) {
            // this comes from onUsersMatchClicked, update the list
            usersMatchServerCall(v);
        }
        else {
            // this comes from PhoneBook fragment initialization
            setListAdapter(new ContactAdapter(getActivity(), phoneContacts));
            ((BaseAdapter)getListAdapter()).notifyDataSetChanged();
        }
    }

    class MatchServieCallback extends AjaxCallback<JSONArray>
    {
        private View v;

        public MatchServieCallback(View v) {
            this.v = v;
        }
        @Override
        public void callback(String url, JSONArray jarr, AjaxStatus status)
        {
            v.findViewById(R.id.findFriendsBtn).setVisibility(View.VISIBLE);
            if (ErrorUtility.isAjaxErrorThenReport(LOGTAG, status, getActivity())) return;

            Log.v(LOGTAG, "callback result: " + jarr.length());

            contactList.clear();
            for( int i=0; i<jarr.length(); i++ ) {
                try {
                    contactList.add(new Contact(jarr.getJSONObject(i)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            Collections.sort(contactList);
            Log.v(LOGTAG, "after upload: "+contactList.size());
//                Collections.sort(friendContacts);
//                setListAdapter(new ContactAdapter(getActivity(), contactList)); // todo optimizieren
            ((BaseAdapter)getListAdapter()).notifyDataSetChanged();
        }
    }

    private void usersMatchServerCall(View v)
    {
        JSONArray emailsJson = new JSONArray();
        int debugI=0;
        for(Contact c:phoneContacts) {
//            if (++debugI<30) {
                emailsJson.put(c.getContactEmail());
//            }
        }
        Log.v(LOGTAG, "uploading "+emailsJson.length()+ " emails");
        String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_USERS_MATCH, "uuid=" + PrefUtility.getUuid());

        int k = 0;
        AjaxCallback ac =  new MatchServieCallback(v);

        try {
            StringEntity entity = new StringEntity(emailsJson.toString());
            v.findViewById(R.id.findFriendsBtn).setVisibility(View.GONE);
            ((ContactManagerActivity)getActivity()).getAQuery().auth(PrefUtility.getBasicHandle())
                    .progress(v.findViewById(R.id.progress)).post(apiUrl, "application/json", entity, JSONArray.class, ac);
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
    public void onMachFriendsConfirm(DialogFragment dialog, View v) {
        Log.v(LOGTAG, "Fragment received Dialog confirmation");
        onUsersMatchClicked(v);
    }
}