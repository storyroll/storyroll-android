package co.storyroll.activity;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.*;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import co.storyroll.PQuery;
import co.storyroll.R;
import co.storyroll.adapter.ContactAdapter;
import co.storyroll.model.Contact;
import co.storyroll.tasks.AsyncLoadContacts;
import co.storyroll.ui.dialog.MatchFriendsDialog;
import co.storyroll.util.*;
import com.androidquery.auth.BasicHandle;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.bugsense.trace.BugSenseHandler;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by martynas on 11/06/14.
 */
public class ContactManagerActivity extends FragmentActivity implements AsyncLoadContacts.LoadContactsListener, MatchFriendsDialog.MatchFriendsDialogListener {

    private static final int[] TAB_HEAD = {R.string.tab_friends, R.string.tab_adressbook};
    private static final String LOGTAG = "CONTACT_MNGR";

    private String mUuid;

    ContactTabAdapter mAdapter;

    ViewPager mTabPager;

    protected PQuery aq;
    protected BasicHandle basicHandle = null;
//    private static ArrayList<String> friendList = new ArrayList<String>();

//    private ContactAdapter contactAdapter = null;

//    private static ListAdapter friendListAdapter;

    public static AsyncLoadContacts contactLoaderTask = null;
    private ProgressBar progress;
    private ImageView doneSelect;

    public static SwipeRefreshLayout swipeContainer;

    public static final int MAX_INVITES_ALLOWED = 5 ;
//    public static ArrayList<Contact> phoneContacts = new ArrayList<Contact>();
//    private static ArrayList<Contact> friendContacts = new ArrayList<Contact>();


    // --- phone contact list

    // Indexing fo the list
//    HashMap<String, Integer> contactsAlphaIndexer;
//    HashMap<String, Integer> friendsAlphaIndexer;

    public PQuery getAQuery(){
        return aq;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // remove title ...

        final ActionBar actionBar = getActionBar();

        // Specify that tabs should be displayed in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);


        mUuid = PrefUtility.getUuid();
        basicHandle = PrefUtility.getBasicHandle();
        aq = new PQuery(this);

        setContentView(R.layout.fragment_pager);
        progress = (ProgressBar) findViewById(R.id.progress);
        doneSelect = (ImageView) findViewById(R.id.doneSelect);

        // Register handler for UI elements
//        doneSelect.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.v(LOGTAG, "doneSelect.onClick");
//                ContactUtil.hideSoftKeyboard(AddressTabsActivity.this);
//                setSelectedContacts();
//                clearAllSelected();
//            }
//
//        });


        //------------------------------------ TABS + PAGES ------------------------------------//

        mAdapter = new ContactTabAdapter(getSupportFragmentManager());
        mTabPager = (ViewPager)findViewById(R.id.pager);
        mTabPager.setAdapter(mAdapter);


        // Create a tab listener that is called when the user changes tabs.
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            @Override
            public void onTabSelected(ActionBar.Tab tab, android.app.FragmentTransaction fragmentTransaction) {
                // show the given tab
                mTabPager.setCurrentItem(tab.getPosition());
                Log.v(LOGTAG, "onTabSelected=="+tab.getPosition());

                // switch the progress on?
                if (tab.getPosition()==1) {
                    if (contactLoaderTask != null && contactLoaderTask.getStatus() == AsyncTask.Status.RUNNING) {
                        swipeContainer.setRefreshing(true);
                    }
                    else {
                        swipeContainer.setRefreshing(false);
                    }
                }
            }

            @Override
            public void onTabUnselected(ActionBar.Tab tab, android.app.FragmentTransaction fragmentTransaction) {
                // hide the given tab

            }

            @Override
            public void onTabReselected(ActionBar.Tab tab, android.app.FragmentTransaction fragmentTransaction) {
                // probably ignore this event

            }
        };

        // Add 2 tabs,
        for (int i = 0; i < 2; i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(getResources().getString(TAB_HEAD[i]))
                            .setTabListener(tabListener));
        }

        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        // When swiping between pages, select the
                        // corresponding tab.
                        getActionBar().setSelectedNavigationItem(position);
                    }
                });

        swipeContainer = (SwipeRefreshLayout)findViewById(R.id.swipe_container);
        swipeContainer.setColorScheme(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
    }

    @Override
    public void onMachFriendsConfirm(DialogFragment dialog) {
        onUsersMatchClicked();
    }

    // ********* ADAPTER

    public class ContactTabAdapter extends FragmentPagerAdapter {

        public ContactTabAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return TAB_HEAD.length;
        }

        @Override
        public Fragment getItem(int position) {
            return ContactListFragment.newInstance(position);
        }
    }

    // set selected contacts on DONE button press
    private void setSelectedContacts()
    {
        Log.v(LOGTAG, "setSelectedContacts");
        ArrayList<String> selectedList = new ArrayList<String>();

        Intent intent = new Intent();
        Log.v(LOGTAG, "current item: "+mTabPager.getCurrentItem());
        ContactListFragment frag = (ContactListFragment)getActiveFragment(mTabPager, mTabPager.getCurrentItem());
        ContactAdapter contactAdapter = (ContactAdapter)frag.getListAdapter();
        Log.v(LOGTAG, "contactAdapter: "+contactAdapter);

        if (contactAdapter!=null && contactAdapter.selected!=null)
        {

            selectedList.addAll(contactAdapter.selected);
            Log.v(LOGTAG, "total entries: "+selectedList.size());
        }
        if (selectedList.size() > 0) {
            Log.v(LOGTAG, "Passing contacts: "+selectedList.toString());
//            intent.putParcelableArrayListExtra("SELECTED_CONTACTS", selectedList);
            intent.putStringArrayListExtra("SELECTED_CONTACTS", selectedList);

            setResult(RESULT_OK, intent);
        } else {
            setResult(RESULT_CANCELED, intent);
        }
        // Tip: Here you can finish this activity and on the Activty result of the calling activity you fetch the Selected contacts
        finish();
    }

    // Also on back pressed set the selected list, if nothing selected set Intent result to canceled
    @Override
    public void onBackPressed()
    {
        stopLoadingContacts();
        clearAllSelected();
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
    };

    private void clearAllSelected() {
        ContactListFragment frag = (ContactListFragment)getActiveFragment(mTabPager, mTabPager.getCurrentItem());
        ContactAdapter contactAdapter = (ContactAdapter)frag.getListAdapter();
        if (contactAdapter==null || contactAdapter.originalList==null) return;
        ArrayList<Contact> contactList = contactAdapter.originalList;
        for (int i = 0; i < contactList.size(); i++) {
            contactList.get(i).setSelected(false);
        }
    }


    @Override
    protected void onDestroy() {
        Log.v(LOGTAG, "onDestroy");
        super.onDestroy();
        stopLoadingContacts();
        aq.dismiss();
    }

    private void stopLoadingContacts() {
        if (contactLoaderTask!=null && contactLoaderTask.getStatus() == AsyncTask.Status.RUNNING)
        {
            Log.v(LOGTAG, "stopping contact loading task...");
            contactLoaderTask.cancel(true);
            contactLoaderTask = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contacts_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.v(LOGTAG, "onOptionsItemSelected");

        // Handle item selection
        Intent intent;
        if (item.getItemId() == R.id.action_ok_selected) {
            Log.v(LOGTAG, "doneSelect.onClick");
            ContactUtil.hideSoftKeyboard(this);
            setSelectedContacts();
            clearAllSelected(); // todo this is not accessible? see method above
            return true;
        } else if (item.getItemId() == R.id.action_match_contacts) {
            Log.v(LOGTAG, "doneSelect.onClick");
            ContactUtil.hideSoftKeyboard(this);
            new MatchFriendsDialog(this).show(getSupportFragmentManager(), "MatchFriendsDialog");

//            onUsersMatchClicked();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }


    private void onUsersMatchClicked()
    {
        Log.v(LOGTAG, "onUsersMatchClicked");
        // addressbook contact loader
        if (ContactManagerActivity.contactLoaderTask==null || ContactManagerActivity.contactLoaderTask.getStatus()!= AsyncTask.Status.RUNNING)
        {
            Log.v(LOGTAG, "ContactLoadTask not running");

            if ((ContactListFragment.phoneContacts == null || ContactListFragment.phoneContacts.size()<1))
            {
                ContactListFragment.phoneContacts = new ArrayList<Contact>();
                // Asynchronously load all contacts
                ContactManagerActivity.contactLoaderTask = new AsyncLoadContacts(0, ContactManagerActivity.this, ContactManagerActivity.this);
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

    @Override
    public void onContactsLoaded(int tabNum) {
        if (tabNum==0) {
            // this comes from onUsersMatchClicked, update the list
            doServerUsersMatchCall();
        }
        else {
            swipeContainer.setRefreshing(false);

//            // this comes from PhoneBook fragment initialization
//            setListAdapter(new ContactAdapter(this, phoneContacts));
//            ((BaseAdapter)getListAdapter()).notifyDataSetChanged();
        }
    }

    class MatchServieCallback extends AjaxCallback<JSONArray>
    {
        @Override
        public void callback(String url, JSONArray jarr, AjaxStatus status)
        {
            if (ErrorUtility.isAjaxErrorThenReport(LOGTAG, status, ContactManagerActivity.this)) {
                swipeContainer.setRefreshing(false);
                return;
            }

            Log.v(LOGTAG, "callback result: " + jarr.length());

            ContactListFragment.friendContacts.clear();
            for( int i=0; i<jarr.length(); i++ ) {
                try {
                    ContactListFragment.friendContacts.add(new Contact(jarr.getJSONObject(i)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            Collections.sort(ContactListFragment.friendContacts);
            Log.v(LOGTAG, "after upload: " + ContactListFragment.friendContacts.size());
//                Collections.sort(friendContacts);
//                setListAdapter(new ContactAdapter(getActivity(), contactList)); // todo optimizieren
            Log.d(LOGTAG, "?refresh adapter for tab "+mTabPager.getCurrentItem());

            Log.d(LOGTAG, "yes, trying to refresh adapter for tab "+mTabPager.getCurrentItem());
            Fragment fr = getActiveFragment(mTabPager, mTabPager.getCurrentItem());
            ((BaseAdapter)((ContactListFragment)fr).getListAdapter()).notifyDataSetChanged();
            swipeContainer.setRefreshing(false);

        }
    }

    private void doServerUsersMatchCall()
    {
        swipeContainer.setRefreshing(true);
        JSONArray idsJson = new JSONArray();
        TelephonyManager tMgr = (TelephonyManager)getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
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
            StringEntity entity = new StringEntity(idsJson.toString());
            aq.auth(PrefUtility.getBasicHandle())
                    .post(apiUrl, "application/json", entity, JSONArray.class, ac);
        }
        catch (UnsupportedEncodingException e)
        {
            BugSenseHandler.sendException(e);
            e.printStackTrace();
        }
    }

    public Fragment getActiveFragment(ViewPager container, int position) {
        String name = makeFragmentName(container.getId(), position);
        return  getSupportFragmentManager().findFragmentByTag(name);
    }

    private static String makeFragmentName(int viewId, int index) {
        return "android:switcher:" + viewId + ":" + index;
    }
}