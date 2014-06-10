package co.storyroll.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import co.storyroll.PQuery;
import co.storyroll.R;
import co.storyroll.activity.ArrayMoviesFragment;
import co.storyroll.model.Movie;
import co.storyroll.util.PrefUtility;
import co.storyroll.util.ServerUtility;
import com.androidquery.callback.AjaxStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MovieItemView extends LinearLayout {
	public final static int[] castIds = {R.id.cast1, R.id.cast2, R.id.cast3};
	private static final String LOGTAG = "MovieItemView";

	Movie movie;
	PQuery aq;
	ArrayMoviesFragment parent;
	
	public MovieItemView(Context context) {
		super(context);
		// this will force redraw, but then we can catch the event and redraw the "likes"
		setWillNotDraw(false);
	}
	public MovieItemView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setWillNotDraw(false);
	}
	public MovieItemView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setWillNotDraw(false);
	}
	
	// this all pretty sloppy, needs better design
	public void initAndLoadCast(Movie movie, PQuery aq, ArrayMoviesFragment parent) {
		Log.v(LOGTAG, "initAndLoadCast");
		this.movie = movie;
		this.aq = aq;
		this.parent = parent;
		if (movie.getCast() == null) {
			// try to load cast
			this.aq.ajax(PrefUtility.getApiUrl(ServerUtility.API_CAST, "story=" + movie.getId()), JSONArray.class, this, "getMovieCastCb");
		}
	}

	public void getMovieCastCb(String url, JSONArray jarr, AjaxStatus status){

//      progressBar.setVisibility(View.GONE);
      if(jarr != null){               
          //successful ajax call
      	Log.i(LOGTAG, "getClipCastCb success, items: "+jarr.length());
      	// make cast avatars visible
  		try {
  			// TODO: sloppy
  			String[] castAvatars = new String[jarr.length()];
  			int i = 0;
        	for (; i < jarr.length(); i++) {
				JSONObject userObj = jarr.getJSONObject(i);
				String avatarUrl = userObj.getString("avatarUrl");
				castAvatars[i] = avatarUrl;
				if (i<castIds.length) {
					ImageView castImage = (ImageView) findViewById(castIds[i]);
                    if (!TextUtils.isEmpty(avatarUrl) && !"null".equals(avatarUrl)) {
                        aq.id(castImage).image(avatarUrl, true, false, 0, R.drawable.ic_avatar_default);
                    }
                    else {
                        aq.id(castImage).image(R.drawable.ic_avatar_default);
                    }
				}
        	}
        	movie.setCast(castAvatars);
        	// indicate that there's more fragments
        	if (i>=castIds.length && false) { // currently disabled 06/03/2014
        		TextView castMore = (TextView) findViewById(R.id.cast_more);
        		aq.id(castMore).text("+"+(i-castIds.length+1)).visible();
        	}
	        	
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
  		
      }else{          
          //ajax error
      	Log.e(LOGTAG, "getStoryCastCb: null Json, cast not received for clipId=" + movie.getId());
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
        super.onDraw(canvas);
    }
}
