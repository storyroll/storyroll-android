package co.storyroll.activity;

import android.app.ActionBar;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ProgressBar;
import co.storyroll.PQuery;
import co.storyroll.R;
import co.storyroll.model.Contact;
import co.storyroll.tasks.AsyncLoadContacts;
import co.storyroll.util.ContactUtil;
import co.storyroll.util.PrefUtility;
import com.androidquery.auth.BasicHandle;

import java.util.ArrayList;

/**
 * Created by martynas on 11/06/14.
 */
public class ContactManagerActivity extends FragmentActivity {

    private static final int[] TAB_HEAD = {R.string.tab_friends, R.string.tab_adressbook};
    private static final String LOGTAG = "ADDRESSBOOK";

    private String mUuid;

    TabAdapter mAdapter;

    ViewPager mTabPager;

    protected PQuery aq;
    protected BasicHandle basicHandle = null;
//    private static ArrayList<String> friendList = new ArrayList<String>();

//    private ContactAdapter contactAdapter = null;

//    private static ListAdapter friendListAdapter;

    private AsyncLoadContacts contactLoaderTask = null;
    private ProgressBar progress;
    private ImageView doneSelect;


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

        mAdapter = new TabAdapter(getSupportFragmentManager());
        mTabPager = (ViewPager)findViewById(R.id.pager);
        mTabPager.setAdapter(mAdapter);


        // Create a tab listener that is called when the user changes tabs.
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            @Override
            public void onTabSelected(ActionBar.Tab tab, android.app.FragmentTransaction fragmentTransaction) {
                // show the given tab
                mTabPager.setCurrentItem(tab.getPosition());
                Log.v(LOGTAG, "onTabSelected=="+tab.getPosition());

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

        // Add 3 tabs, specifying the tab's text and TabListener
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

    }

    // ********* ADAPTER

    public class TabAdapter extends FragmentPagerAdapter {

        public TabAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return TAB_HEAD.length;
        }

        @Override
        public Fragment getItem(int position) {
            return TabListFragment.newInstance(position);
        }
    }

    // set selected contacts on DONE button press
    private void setSelectedContacts()
    {
        Log.v(LOGTAG, "setSelectedContacts");
        ArrayList<String> selectedList = new ArrayList<String>();

        Intent intent = new Intent();
        Log.v(LOGTAG, "current item: "+mTabPager.getCurrentItem());
        TabListFragment frag = (TabListFragment)getActiveFragment(mTabPager, mTabPager.getCurrentItem());
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
        TabListFragment frag = (TabListFragment)getActiveFragment(mTabPager, mTabPager.getCurrentItem());
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
        } else {
            return super.onOptionsItemSelected(item);
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