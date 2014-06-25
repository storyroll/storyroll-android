package co.storyroll.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import co.storyroll.PQuery;
import co.storyroll.R;
import co.storyroll.model.Movie;

public class MovieItemView extends LinearLayout {
    public final static int MAX_SHOWN_CLIPS = 3;
    public final static int MAX_AVAIL_POSITIONS_IN_DESIGN = 4;
    public final static int[] castIds = {R.id.cast1, R.id.cast2, R.id.cast3, R.id.cast4};
    public final static int[] holderIds = new int[] {R.id.position1, R.id.position2, R.id.position3, R.id.position4};
    public final static int[] vidPlayIds = {R.id.videoPlayerView1, R.id.videoPlayerView2, R.id.videoPlayerView3, R.id.videoPlayerView4};
    public final static int[] progressIds = {R.id.progress1, R.id.progress2, R.id.progress3, R.id.progress4};
    public final static int[] thumbIds = {R.id.thumb1, R.id.thumb2, R.id.thumb3, R.id.thumb4};

    private static final String LOGTAG = "MovieItemView";
    private static final int[] btnCameraResIds = {R.drawable.btn_camera_0, R.drawable.btn_camera_1, R.drawable.btn_camera_2, R.drawable.btn_camera_3};
    public static final int[] arrowResIds = {R.drawable.arrow_0, R.drawable.arrow_1, R.drawable.arrow_2, R.drawable.arrow_3};

    Movie movie;
	PQuery aq;
//	ArrayMoviesFragment parent;
	
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
	
//	// this all pretty sloppy, needs better design
//	public void initAndLoadCast(Movie movie, PQuery aq){ //}, ArrayMoviesFragment parent) {
//		Log.v(LOGTAG, "initAndLoadCast");
//		this.movie = movie;
//		this.aq = aq;
////		this.parent = parent;
//		if (movie.getCast() == null) {
//			// try to load cast
//			this.aq.ajax(PrefUtility.getApiUrl(ServerUtility.API_CAST, "story=" + movie.getId()), JSONArray.class, this, "getMovieCastCb");
//		}
//	}

//	public void getMovieCastCb(String url, JSONArray jarr, AjaxStatus status){
//
////      progressBar.setVisibility(View.GONE);
//      if(jarr != null){
//          //successful ajax call
//      	Log.i(LOGTAG, "getClipCastCb success, items: "+jarr.length());
//      	// make cast avatars visible
//  		try {
//  			// TODO: sloppy
//  			String[] castAvatars = new String[jarr.length()];
//  			int i = 0;
//        	for (; i < jarr.length(); i++) {
//				JSONObject userObj = jarr.getJSONObject(i);
//				String avatarUrl = userObj.getString("avatarUrl");
//				castAvatars[i] = avatarUrl;
//				if (i<MAX_SHOWN_CLIPS)
//                {
//					ImageView castImage = (ImageView) findViewById(castIds[i]);
//                    if (!TextUtils.isEmpty(avatarUrl) && !"null".equals(avatarUrl))
//                    {
//                        aq.id(castImage).image(avatarUrl, true, false, 0, R.drawable.ic_avatar_default);
//                    }
//                    else {
//                        aq.id(castImage).image(R.drawable.ic_avatar_default).visible();
//                    }
//				}
//        	}
//        	movie.setCast(castAvatars);
//        	// indicate that there's more fragments
////        	if (i>=castIds.length && false) { // currently disabled 06/03/2014
////        		TextView castMore = (TextView) findViewById(R.id.cast_more);
////        		aq.id(castMore).text("+"+(i-castIds.length+1)).visible();
////        	}
//
//		} catch (JSONException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//      }else{
//          //ajax error
//      	Log.e(LOGTAG, "getStoryCastCb: null Json, cast not received for clipId=" + movie.getId());
//      }
//	}
	
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
