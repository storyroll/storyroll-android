package com.storyroll.base;

import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.widget.Toast;

public abstract class SwipeVideoActivity extends BaseActivity {
	private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    
    int swipeMinDistance = SWIPE_MIN_DISTANCE;
    int swipeThresholdVelocity = SWIPE_THRESHOLD_VELOCITY;
    int swipeMaxOffPath = SWIPE_MAX_OFF_PATH;
    ViewConfiguration vc;
    
    private GestureDetector gestureDetector;
    protected View.OnTouchListener gestureListener;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // handling different pixel density, computing the swipe parameters by hand. 
        // obtain scaled, reasonable values from the system using ViewConfiguration class
        vc = ViewConfiguration.get(getBaseContext());
//        swipeMinDistance = vc.getScaledPagingTouchSlop();
//        swipeThresholdVelocity = vc.getScaledMinimumFlingVelocity();
//        swipeMaxOffPath = vc.getScaledTouchSlop();
        // Gesture detection
        gestureDetector = new GestureDetector(this, new SwipeGestureDetector());
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        };

    }
    
    class SwipeGestureDetector extends SimpleOnGestureListener {
        private static final String LOGTAG = "SwipeVideoActivity.SwipeGestureDetector";

		@Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > swipeMaxOffPath)
                    return false;
                // right to left swipe
                if(e1.getX() - e2.getX() > swipeMinDistance && Math.abs(velocityX) > swipeThresholdVelocity) {
                    Log.v(LOGTAG, "Left Swipe");
                    leftSwipe();
                }  else if (e2.getX() - e1.getX() > swipeMinDistance && Math.abs(velocityX) > swipeThresholdVelocity) {
                    Log.v(LOGTAG, "Right Swipe");
                    rightSwipe();
                }
            } catch (Exception e) {
                // nothing
            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
              return true;
        }
    }
    
    protected abstract void leftSwipe();
    protected abstract void rightSwipe();

}
