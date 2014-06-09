package co.storyroll.util;

import android.app.ActionBar;
import android.app.Activity;
import android.view.ViewConfiguration;

import java.lang.reflect.Field;

public class ActionBarUtility {

	private static final String LOGTAG = "ActionBarUtility";

	public static void initCustomActionBar(Activity activity, boolean homeButtonEnabled) {

		/* Show the custom action bar view and hide the normal Home icon and title */
		final ActionBar actionBar = activity.getActionBar();
//		actionBar.setIcon(R.drawable.ic_icon_custom); // 06-07-2014
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(homeButtonEnabled);
//		actionBar.setIcon(R.drawable.ic_ab_som);
//		actionBar.setCustomView(customActionBarView);
		
//		actionBar.setCustomView(R.layout.actionbar_custom);
//		actionBar.setDisplayShowTitleEnabled(false); // 06-07-2014
//        actionBar
//                .setDisplayShowCustomEnabled(true);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        
        // force overflow menu key?
        try {
        	  ViewConfiguration config = ViewConfiguration.get(activity);
        	  Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
        	  if (menuKeyField != null)
        	  {
        	    menuKeyField.setAccessible(true);
        	    menuKeyField.setBoolean(config, false);
        	  }
        	}
        	catch (Exception e) 
        	{
        	  // presumably, not relevant
        	}
        }

//	public static void adjustActionBarLogoCentering(Activity activity) {
//		// set margin to center
//		View actionBarLogoHolder = activity.findViewById(R.id.actionBarLogoHolder);
//		LayoutParams lp =  (LayoutParams) actionBarLogoHolder.getLayoutParams();
//		
//		/// Converts dip into its equivalent px
//		Resources r = activity.getResources();
//		float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 52, r.getDisplayMetrics());
//		lp.setMargins(-Math.round(px), 0, 0, 0);
//		actionBarLogoHolder.setLayoutParams(lp);
//
//	}

	
}
