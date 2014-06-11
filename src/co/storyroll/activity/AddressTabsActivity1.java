package co.storyroll.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import co.storyroll.R;

/**
 * Created by martynas on 11/06/14.
 */
public class AddressTabsActivity1 extends Activity {
    ActionBar.Tab tab1, tab2;
    Fragment fragmentTab1 = new FragmentTab1();
    Fragment fragmentTab2 = new FragmentTab2();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab_test);

        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        tab1 = actionBar.newTab().setText("Friends");
        tab2 = actionBar.newTab().setText("AddressBook");

        tab1.setTabListener(new MyTabListener(fragmentTab1));
        tab2.setTabListener(new MyTabListener(fragmentTab2));

        actionBar.addTab(tab1);
        actionBar.addTab(tab2);
    }

    public class MyTabListener implements ActionBar.TabListener {
        Fragment fragment;

        public MyTabListener(Fragment fragment) {
            this.fragment = fragment;
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {
            ft.replace(R.id.fragment_container, fragment);
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {
            ft.remove(fragment);
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {
            // nothing done here
        }
    }
}