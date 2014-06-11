package co.storyroll.activity;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.*;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import co.storyroll.PQuery;
import co.storyroll.R;
import co.storyroll.util.ErrorUtility;
import co.storyroll.util.PrefUtility;
import co.storyroll.util.ServerUtility;
import com.androidquery.auth.BasicHandle;
import com.androidquery.callback.AjaxStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by martynas on 11/06/14.
 */
public class AddressTabsActivity extends FragmentActivity {

    private static final int[] TAB_HEAD = {R.string.tab_friends, R.string.tab_adressbook};
    private static final String LOGTAG = "ADDRESSBOOK";

    private String mUuid;

    TabAdapter mAdapter;

    ViewPager mPager;

    protected PQuery aq;
    protected BasicHandle basicHandle = null;
    private static ArrayList<String> friendList = new ArrayList<String>();

    private static ListAdapter friendListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final ActionBar actionBar = getActionBar();
        super.onCreate(savedInstanceState);

        mUuid = PrefUtility.getUuid();
        basicHandle = new BasicHandle(mUuid, PrefUtility.getPassword());
        aq = new PQuery(this);
        updateFriendsFromServer();

        setContentView(R.layout.fragment_pager);

        mAdapter = new TabAdapter(getSupportFragmentManager());
        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        // Specify that tabs should be displayed in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create a tab listener that is called when the user changes tabs.
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            @Override
            public void onTabSelected(ActionBar.Tab tab, android.app.FragmentTransaction fragmentTransaction) {
                // show the given tab
                mPager.setCurrentItem(tab.getPosition());
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

        // additional plumbing
        friendListAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, friendList);
    }

    // ********* HELPERS

    protected void updateFriendsFromServer()
    {
        aq.auth(basicHandle).ajax(PrefUtility.getApiUrl(
                ServerUtility.API_USER_FRIENDS, "uuid=" + mUuid), JSONArray.class, this, "userFriendsCb");
    }


    public void userFriendsCb(String url, JSONArray jarr, AjaxStatus status) throws JSONException {
        Log.v(LOGTAG, "invitesPendingCb");

        if (status.getCode() != 200 && status.getCode()!=AjaxStatus.TRANSFORM_ERROR) {
            ErrorUtility.apiError(LOGTAG, "Error getting friends", status, this, true, Log.ERROR);
            return;
        }
        Log.v(LOGTAG, "got friends: "+jarr.length());

        // update list
        friendList.clear();
        for (int i=0;i<jarr.length();i++) {
            JSONObject jo = jarr.getJSONObject(i);
            friendList.add(jo.getString("username")+ " ("+jo.getString("email")+")");
            ((BaseAdapter)friendListAdapter).notifyDataSetChanged();
        }
    }

    // ********* ADAPTER

    public static class TabAdapter extends FragmentPagerAdapter {
        public TabAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return TAB_HEAD.length;
        }

        @Override
        public Fragment getItem(int position) {
            return ArrayListFragment.newInstance(position);
        }
    }

    public static class ArrayListFragment extends ListFragment {
        int mNum;

        /**
         * Create a new instance of CountingFragment, providing "num"
         * as an argument.
         */
        static ArrayListFragment newInstance(int num) {
            ArrayListFragment f = new ArrayListFragment();

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
            mNum = getArguments() != null ? getArguments().getInt("num") : 1;
        }

        /**
         * The Fragment's UI is just a simple text view showing its
         * instance number.
         */
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_pager_list, container, false);

            Button findBtn = (Button)v.findViewById(R.id.findFriendsBtn);
            findBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    doUploadAddressBook();
                }
            });
            return v;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
//            setListAdapter(new ArrayAdapter<String>(getActivity(),
//                    android.R.layout.simple_list_item_1, friendList));
            setListAdapter(friendListAdapter);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            Log.i("FragmentList", "Item clicked: " + id);
        }

        // helper
        private void doUploadAddressBook() {
            Log.v("ddd", "upload");

        }
    }


}