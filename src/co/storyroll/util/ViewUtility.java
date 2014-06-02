package co.storyroll.util;

import android.view.View;

public class ViewUtility {
	public static void setViewSquare(View v, int calculatedWidth) {
		// set preview window to square
		android.view.ViewGroup.LayoutParams lp = v.getLayoutParams();
		lp.width = calculatedWidth;
		lp.height = calculatedWidth;
		v.setLayoutParams(lp);
	}
}
