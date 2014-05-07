package com.storyroll.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.storyroll.PQuery;
import com.storyroll.R;
import com.storyroll.activity.ArrayClipsFragment;
import com.storyroll.activity.ArrayListFragment;
import com.storyroll.base.Constants;
import com.storyroll.model.Clip;

public class ClipListItemView extends LinearLayout {
	private static final String LOGTAG = "BlinkListItem";

	Clip blink;
	PQuery aq;
	ArrayClipsFragment parent;
	
	public ClipListItemView(Context context) {
		super(context);
		// this will force redraw, but then we can catch the event and redraw the "likes"
		setWillNotDraw(false);
	}
	public ClipListItemView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setWillNotDraw(false);
	}
	public ClipListItemView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setWillNotDraw(false);
	}
	
	// this all pretty sloppy, needs better design
	public void init(Clip blink, PQuery aq, ArrayClipsFragment parent) {
		this.blink = blink;
		this.aq = aq;
	}

	
    @Override
    protected void onDraw(Canvas canvas) {
//        Log.v(LOGTAG, "onDraw()");
    	// update likes indicator
//        ImageView likesControl = (ImageView) findViewById(R.id.likeImage);
//        if (parent.userLikes.contains(blink.getId()+"")) {
//        	likesControl.setImageResource(R.drawable.ic_star_on);
//        } else {
//        	likesControl.setImageResource(R.drawable.ic_star_off);
//        }
//        likesControl.setImageResource(R.drawable.ic_star_off);
        // update unseen indicator
//        ImageView unseenIndicator = (ImageView) findViewById(R.id.unseenIndicator);

        if (parent.unseenClips.contains(blink.getId()+"")) 
        {
        	if (Constants.IS_NEW_BLINK_INDICATED) {
//        		unseenIndicator.setVisibility(View.VISIBLE);
        	} else {
                ImageView playControl = (ImageView) findViewById(R.id.playControl);
                playControl.setImageResource(R.drawable.ic_play_roll_new);
        	}
        }
        else {
//        	unseenIndicator.setVisibility(View.INVISIBLE);
        }
        super.onDraw(canvas);
    }
}
