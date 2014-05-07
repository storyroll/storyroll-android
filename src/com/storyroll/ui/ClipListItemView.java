package com.storyroll.ui;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.androidquery.callback.AjaxStatus;
import com.storyroll.PQuery;
import com.storyroll.R;
import com.storyroll.activity.ArrayClipsFragment;
import com.storyroll.activity.ArrayListFragment;
import com.storyroll.base.Constants;
import com.storyroll.model.Clip;
import com.storyroll.util.PrefUtility;

public class ClipListItemView extends LinearLayout {
	public final static int[] castIds = {R.id.cast1, R.id.cast2, R.id.cast3};
	private static final String LOGTAG = "BlinkListItem";

	Clip clip;
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
	public void initAndLoadCast(Clip blink, PQuery aq, ArrayClipsFragment parent) {
		Log.v(LOGTAG, "initAndLoadCast");
		this.clip = blink;
		this.aq = aq;
		this.parent = parent;
		if (blink.getCast() == null) {
			// try to load cast
//			this.aq.ajax(PrefUtility.getApiUrl()+"getClipCast?story="+blink.getId(), JSONArray.class, this, "getClipCastCb");
			this.aq.ajax(PrefUtility.getApiUrl()+"getStoryCast?story="+12, JSONArray.class, this, "getClipCastCb");
		}
	}

	public void getClipCastCb(String url, JSONArray jarr, AjaxStatus status){
//      progressBar.setVisibility(View.GONE);
      if(jarr != null){               
          //successful ajax call
      	Log.i(LOGTAG, "getClipCastCb success: "+jarr.toString());
      	// make cast avatars visible
  		try {
  			// TODO: sloppy
  			String[] cast = new String[jarr.length()];
	        	for (int i = 0; i < jarr.length(); i++) {
					JSONObject userObj = jarr.getJSONObject(i);
					ImageView castImage = (ImageView) findViewById(castIds[i]);
					String uuid = userObj.getString("uuid");
					cast[i] = uuid;
					aq.id(castImage).image(PrefUtility.getApiUrl()+"avatar?uuid="+uuid, true, false, 0, R.drawable.ic_avatar_default);
	        	}
	        	clip.setCast(cast);
	        	
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
  		
      }else{          
          //ajax error
      	Log.e(LOGTAG, "getStoryCastCb: null Json, cast not received for clipId="+clip.getId());
      }
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

        if (parent.unseenClips.contains(clip.getId()+"")) 
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
