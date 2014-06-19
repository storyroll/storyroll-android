package co.storyroll.util;

import android.app.Activity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.widget.AbsListView;
import android.widget.ListView;
import co.storyroll.R;

/**
 * Created by martynas on 18/06/14.
 */
public class SwipeUtil
{
    public static SwipeRefreshLayout initSwiping(Activity act, final ListView chanListView, SwipeRefreshLayout.OnRefreshListener listener) {
        final SwipeRefreshLayout  swipeContainer = (SwipeRefreshLayout) act.findViewById(R.id.swipe_container);
        swipeContainer.setOnRefreshListener(listener);
        swipeContainer.setColorScheme(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        chanListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int topRowVerticalPosition =
                        (chanListView == null || chanListView.getChildCount() == 0) ?
                                0 : chanListView.getChildAt(0).getTop();
                swipeContainer.setEnabled(topRowVerticalPosition >= 0);
            }
        });
        return swipeContainer;
    }
}
