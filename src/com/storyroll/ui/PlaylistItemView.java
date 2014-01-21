package com.storyroll.ui;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.androidquery.callback.AjaxStatus;
import com.storyroll.PQuery;
import com.storyroll.R;
import com.storyroll.activity.ArrayListFragment;
import com.storyroll.model.Story;
import com.storyroll.util.AppUtility;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class PlaylistItemView extends LinearLayout {
	public final static int[] castIds = {R.id.cast1, R.id.cast2, R.id.cast3, R.id.cast4, R.id.cast5};
	private static final String LOGTAG = "PlaylistItem";

	Story story;
	PQuery aq;
	ArrayListFragment parent;
	
	public PlaylistItemView(Context context) {
		super(context);
		// this will force redraw, but then we can catch the event and redraw the "likes"
		setWillNotDraw(false);
	}
	public PlaylistItemView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setWillNotDraw(false);
	}
	public PlaylistItemView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setWillNotDraw(false);
	}
	
	// this all pretty sloppy, needs better design
	public void initAndLoadCast(Story story, PQuery aq, ArrayListFragment parent) {
		this.story = story;
		this.aq = aq;
		this.parent = parent;
		if (story.getCast() == null) {
			// try to load cast
			this.aq.ajax(AppUtility.API_URL+"getStoryCast?story="+story.getId(), JSONArray.class, this, "getStoryCastCb");
		}
	}

	public void getStoryCastCb(String url, JSONArray jarr, AjaxStatus status){
//        progressBar.setVisibility(View.GONE);
        if(jarr != null){               
            //successful ajax call
        	Log.i(LOGTAG, "getStoryCastCb success: "+jarr.toString());
        	// make cast avatars visible
    		try {
    			String[] cast = new String[jarr.length()];
	        	for (int i = 0; i < jarr.length(); i++) {
					JSONObject userObj = jarr.getJSONObject(i);
					ImageView castImage = (ImageView) findViewById(castIds[i]);
					String uuid = userObj.getString("uuid");
					cast[i] = uuid;
					aq.id(castImage).image(AppUtility.API_URL+"avatar?uuid="+uuid, true, false, 0, R.drawable.ic_avatar_default);
	        	}
	        	story.setCast(cast);
	        	
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		
        }else{          
            //ajax error
        	Log.e(LOGTAG, "getStoryCastCb: null Json, cast not received for storyId="+story.getId());
        }
       
	}
	
    @Override
    protected void onDraw(Canvas canvas) {
//        Log.v(LOGTAG, "onDraw()");
        ImageView likesControl = (ImageView) findViewById(R.id.likeImage);
        if (parent.userLikes.contains(story.getId()+"")) {
        	likesControl.setImageResource(R.drawable.ic_star_on);
        } else {
        	likesControl.setImageResource(R.drawable.ic_star_off);
        }
        super.onDraw(canvas);
    }
}
