package com.storyroll.util;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.storyroll.R;

public class ActionBarUtility {

	public static void initCustomActionBar(Activity context) {

		/* Show the custom action bar view and hide the normal Home icon and title */
		final ActionBar actionBar = context.getActionBar();
//		actionBar.setHomeButtonEnabled(false);
//		actionBar.setDisplayHomeAsUpEnabled(false);
//		actionBar.setIcon(R.drawable.ic_ab_som);
//		actionBar.setCustomView(customActionBarView);
		
		actionBar.setCustomView(R.layout.actionbar_custom);
		actionBar.setDisplayShowTitleEnabled(false);
        actionBar
                .setDisplayShowCustomEnabled(true);
//        actionBar.setDisplayUseLogoEnabled(false);
//        actionBar.setDisplayShowHomeEnabled(false);	
        }

	
}
