package co.storyroll.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import co.storyroll.PQuery;
import co.storyroll.R;
import co.storyroll.activity.ChannelActivity;
import co.storyroll.activity.VideoCaptureActivity;
import co.storyroll.enums.AutostartMode;
import co.storyroll.model.Movie;
import co.storyroll.ui.ControlledMovieView;
import co.storyroll.ui.MovieItemView;
import co.storyroll.ui.RoundedImageView;
import co.storyroll.util.*;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by martynas on 17/06/14.
 */

public class MovieAdapter extends ArrayAdapter<Movie> implements AbsListView.OnScrollListener, SwipeRefreshLayout.OnRefreshListener {

    private final int FIRST_VIDEO_ITEM_IDX = 1; // index of the first element in ListView, which is an actual video

    private final long mChanId;
    public String LOGTAG = "MovieAdapter";

    private static final boolean HIDE_AGE_AGO_POSTFIX = false;
    private final Activity context;
    private final ArrayList<Movie> movies;
    private final PQuery aq;
    private final String uuid;
    private int screenWidth, screenHeight, calculcatedVideoWidth;

    private int autoRangeTop;
    private int autoRangeBottom;
    private boolean isTrial;
    private ImageButton.OnClickListener onMoviehideClick;
    private AvatarClickListener avatarClickListener = new AvatarClickListener();
    private ToggleModeClickListener onToggleModeListener = new ToggleModeClickListener();

    static final Integer LIMIT_ITEMS = 40;

    // how many percents of width should a squared video take
    private static final int PERCENT_SQUARE = 95;

    // the range of display which acts as an "autostart" area for videos
    // when the center of VideoView enters this range, it is autostarted
    // TODO: must be calculated as a function of display w, h and PERCENT_SQUARE
    private static final float AUTO_RANGE_TOP = 0.33f;
    private static final float AUTO_RANGE_BOTTOM = 0.75f;

    private static ControlledMovieView currentlyPlayed = null;

    private Calendar c = Calendar.getInstance();

    public MovieAdapter(final Activity context, ArrayList<Movie> movies,
                            PQuery aq, String uuid, long chanId, boolean trial) {

        super(context, R.layout.movie_item, movies);

        this.context = context;
        this.movies = movies;
        this.aq = aq;
        this.uuid = uuid;
        this.isTrial = trial;
        this.mChanId = chanId;

//			screenWidth = context.getWindowManager().getDefaultDisplay()
//					.getWidth();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        calculcatedVideoWidth = (screenWidth*PERCENT_SQUARE)/100;

        // calculate active range
        autoRangeTop = Math.round(AUTO_RANGE_TOP*screenHeight);
        autoRangeBottom = Math.round(AUTO_RANGE_BOTTOM*screenHeight);
        onMoviehideClick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fireGAnalyticsEvent("ui_action", "touch", "action_roll_movie", null);
//                new HideMovieDialog().show(context.getSupportFragmentManager(), "HideMovieDialog");
            }
        };

//			Log.d(LOGTAG, "display metrix: "+screenWidth+" x "+screenHeight+", autoRange: "+autoRangeTop+" - "+autoRangeBottom);

    }

    // The following two methods overriden to trick Adapter into thinking we have +1 item (which is an interactive control)
    @Override
    public int getCount() {
        return super.getCount()+1;
    }

    @Override
    public Movie getItem(int position) {
        Log.v(LOGTAG, "getItem: "+position);
        return super.getItem(position-1);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
			Log.v(LOGTAG, "getView: "+position);

        // 1. Create inflater
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // 2. Get rowView from inflater
        if (position==0) {
            View movieItemNew = inflater.inflate(R.layout.movie_item_new, parent, false);
            ViewUtility.setViewWidth(movieItemNew, calculcatedVideoWidth);
            aq.id(movieItemNew).clicked(this, "onNewPressed");
            return movieItemNew;
        }
        else {
            position = position++;
        }

        MovieItemView rowView = (MovieItemView)inflater.inflate(R.layout.movie_item, parent, false);

        // 3. Get the views from the rowView
//        ImageView videoThumb = (ImageView) rowView.findViewById(R.id.videoThumb);
        Movie movie = getItem(position);
        Log.v(LOGTAG, "movie has clips: "+movie.getClips().size());

        // set sizes of video table cells
        for (int cid:MovieItemView.holderIds) {
            ViewUtility.setViewSquare(rowView.findViewById(cid), calculcatedVideoWidth/2);
        }

        for (int i=0; i<movie.getClips().size() && i<MovieItemView.MAX_SHOWN_CLIPS; i++)
        {
            String thumbUrl = movie.getClips().get(i).getThumbUrl();

            ImageView thumb = (ImageView)rowView.findViewById(MovieItemView.thumbIds[i]);
            aq.id(thumb).image(thumbUrl, true, true);
            if (i<MovieItemView.MAX_SHOWN_CLIPS)
            {
                ControlledMovieView videoView = (ControlledMovieView)rowView.findViewById(MovieItemView.vidPlayIds[i]);
                ProgressBar pb = (ProgressBar)rowView.findViewById(MovieItemView.progressIds[i]);
                videoView.adapterInit(this, thumb, calculcatedVideoWidth/2, position,
                        movie.getClips().get(i), uuid, pb, null, null);
                thumb.setOnClickListener(new ThumbClickListener(videoView));
            }
        }
        // make last clip holder hold a rec button
        if (movie.getClips().size()<MovieItemView.MAX_SHOWN_CLIPS)
        {
            int pos = movie.getClips().size();
            ImageView thumb = (ImageView)rowView.findViewById(MovieItemView.thumbIds[pos]);
            thumb.setImageResource(R.drawable.btn_camera);
            thumb.setOnClickListener(new ReplyClickListener(movie));
        }
        ViewUtility.setViewSquare(rowView.findViewById(R.id.thumbTable), calculcatedVideoWidth);

//        aq.id(videoThumb).image(movie.getThumbUrl()); // first things first, this takes time to load

        ImageView playControl = (ImageView) rowView.findViewById(R.id.playControl);
//        ImageView removeControl = (ImageView) rowView.findViewById(R.id.removeControl);
//        removeControl.setOnClickListener(onMoviehideClick);

        TextView likesNum = (TextView) rowView.findViewById(R.id.numLikes);
        ImageView likeControl = (ImageView) rowView.findViewById(R.id.likeImage);
        ControlledMovieView videoView = (ControlledMovieView)rowView.findViewById(R.id.videoPlayerView);
        ProgressBar progressBar = (ProgressBar) rowView.findViewById(R.id.progress);
//        View unseenIndicator = rowView.findViewById(R.id.unseenIndicator);
//        ImageButton cameraButton = (ImageButton)rowView.findViewById(R.id.cameraButton);
        ImageView arrowHolder = (ImageView)rowView.findViewById(R.id.arrowHolder);

        // 4. set data & callbacks
        if (movie.getClipCount()<MovieItemView.MAX_SHOWN_CLIPS) {
            int stateNum = movie.getClipCount();
//            cameraButton.setImageResource(btnCameraResIds[stateNum]);
            arrowHolder.setImageResource(MovieItemView.arrowResIds[stateNum]);
            ViewUtility.setViewSquare(arrowHolder, calculcatedVideoWidth);
            ((TextView)rowView.findViewById(R.id.agePrefix)).setText("");
        }
        else {
            ((TextView)rowView.findViewById(R.id.agePrefix)).setText("Completed ");
            ViewUtility.setViewSquare(playControl, calculcatedVideoWidth);
            playControl.setVisibility(View.VISIBLE);
            playControl.setOnClickListener(new ThumbClickListener(videoView));
        }

//        if (!movie.isSeen()) {
//            playControl.setImageResource(R.drawable.ic_play_roll_new);
//        }
//        ViewUtility.setViewSquare(cameraButton, calculcatedVideoWidth);
        ViewUtility.setViewSquare(rowView.findViewById(R.id.bgHolder), calculcatedVideoWidth);
        ViewUtility.setViewSquare(rowView.findViewById(R.id.maskHolder), calculcatedVideoWidth);

//        rowView.initAndLoadCast(movie, aq);

//        ViewUtility.setViewSquare(videoThumb, calculcatedVideoWidth);
        ViewUtility.setViewSquare(playControl, calculcatedVideoWidth);

        if (movie.getClipCount()>=MovieItemView.MAX_SHOWN_CLIPS)
        {
            videoView.adapterInit(this, playControl, calculcatedVideoWidth, position,
                    movie, uuid, progressBar, null, playControl);
//            // todo: optimize to not create a listener for each, but reuse one listener
        }
//        cameraButton.setOnClickListener(new ReplyClickListener(movie));
        ImageView shareImage = (ImageView)rowView.findViewById(R.id.shareImage);
        if (movie.getPlayerUrl()!=null)
        {
            shareImage.setTag(movie.getPlayerUrl());
//            shareImage.setAlpha(1);
            shareImage.setEnabled(true);
        }
        aq.id(shareImage).clicked(mOnShareClicked);

        ImageView shareInsta = (ImageView)rowView.findViewById(R.id.shareInsta);
        if (movie.getPlayerUrl()!=null)
        {
            shareInsta.setTag(movie.getFileUrl());
            shareInsta.setEnabled(true);
        }
        aq.id(shareInsta).clicked(mOnShareInstaClicked);

        ImageView displayModeControl = (ImageView)rowView.findViewById(R.id.modeControl);
        if (movie.getClipCount()>=MovieItemView.MAX_SHOWN_CLIPS) {
            displayModeControl.setTag(videoView);
            displayModeControl.setOnClickListener(onToggleModeListener);
        }
        else {
            displayModeControl.setVisibility(View.GONE);
        }

        ImageView soundControl = (ImageView)rowView.findViewById(R.id.soundControl);


        String ageText = DateUtils.getRelativeTimeSpanString(
                movie.getPublishedOn(), c.getTimeInMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString();
        if (HIDE_AGE_AGO_POSTFIX) {
            ageText = ageText.replace(" ago", "");
        }
        aq.id(rowView.findViewById(R.id.ageText)).text(ageText);
        likesNum.setText(shortLikesString(movie.getLikeCount()));

        int clipsNum = 0;
        if (movie.getCast()!=null) clipsNum = movie.getCast().size();

        for (int i=0; i< MovieItemView.MAX_AVAIL_POSITIONS_IN_DESIGN; i++)
        {
            RoundedImageView castImage = (RoundedImageView) rowView.findViewById(MovieItemView.castIds[i]);
            if (i<clipsNum) {
                if (!TextUtils.isEmpty(movie.getCast().get(i).getAvatarUrl())) {
                    aq.id(castImage).image(movie.getCast().get(i).getAvatarUrl(), true, true, 0, R.drawable.ic_avatar_default);
                    // set username as tag so that it can be picked up by click listener
                    castImage.setTag(movie.getCast().get(i).getUsername());
                } else {
                    castImage.setImageResource(R.drawable.ic_avatar_default);
                }
                aq.id(castImage).visible().clicked(avatarClickListener);
            }
            if (i>=MovieItemView.MAX_SHOWN_CLIPS) {
                castImage.setVisibility(View.GONE);
            }
        }


        // set current user-video like state
//        movie.setUserLikes( userLikes.contains(movie.getId()+"") );
        if (movie.isUserLikes()) {
            likeControl.setImageResource(R.drawable.ic_star_on);
        }
        else {
            likeControl.setImageResource(R.drawable.ic_star_off);
        }

        // disable liking in trial
//			if (!isTrial) {
        likeControl.setOnClickListener(new LikeClickListener(likeControl,
                likesNum, uuid, movie, isTrial));
//			}


        // 5. return rowView
        return rowView;
    }

    @Override
    public void onRefresh() {
        ((ChannelActivity)context).onRefresh();
    }


    class AvatarClickListener implements  ImageView.OnClickListener {
        @Override
        public void onClick(View view) {
            if (view.getTag()!=null) {
                fireGAnalyticsEvent("ui_action", "touch", "avatar", null);
                Toast toast = Toast.makeText(context, view.getTag().toString(), Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER_VERTICAL| Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();
            }
            else {
                fireGAnalyticsEvent("ui_action", "touch", "avatar_placeholder", null);
            }
        }
    }

    class ToggleModeClickListener implements  ImageView.OnClickListener {
        @Override
        public void onClick(View view) {
            if (view.getTag()!=null)
            {
                ControlledMovieView cmv = (ControlledMovieView)view.getTag();
                fireGAnalyticsEvent("ui_action", "touch", "toggle_video_mode_"+(cmv.isQuadPlay()?"quad":"seq"), null);
                if (cmv.isQuadPlay()) {
                    ((ImageView)view).setImageResource(R.drawable.ico_mode_quad);
                }
                else {
                    ((ImageView)view).setImageResource(R.drawable.ico_mode_seq);
                }
                cmv.toggleQuadPlay();
            }
            else {
                fireGAnalyticsEvent("ui_action", "touch", "toggle_video_mode_undefined", null);
            }
        }
    }


    // play/stop click listener
    class ThumbClickListener implements ImageView.OnClickListener {
        ControlledMovieView pv;
        public ThumbClickListener(ControlledMovieView v){
            this.pv = v;
        }

        @Override
        public void onClick(View v) {
            fireGAnalyticsEvent("ui_action", "touch", "movieThumb", null);
            pv.startVideoPreloading(true, false);
        }
    }

    // reply listener
    class ReplyClickListener implements ImageButton.OnClickListener {
        Movie movie;

        public ReplyClickListener(Movie movie){
            this.movie = movie;
        }

        @Override
        public void onClick(View v) {
            if (isTrial) {
                fireGAnalyticsEvent("ui_action", "touch", "replyButton_trial", null);
                Toast.makeText(context, "Please sign in to reply to this video. You can sign in from app menu.", Toast.LENGTH_SHORT).show();
            }
            else {
                fireGAnalyticsEvent("ui_action", "touch", "replyButton", null);
                Intent intent = new Intent(context, VideoCaptureActivity.class);
                intent.putExtra(VideoCaptureActivity.RESPOND_TO_CLIP, movie.getLastClipId());
                intent.putExtra(VideoCaptureActivity.RESPOND_TO_CLIP_URL, movie.getLastClipUrl());
                intent.putExtra(VideoCaptureActivity.CURRENT_CHANNEL, mChanId);
                intent.putExtra(VideoCaptureActivity.MOVIE, movie.getId());
                intent.putExtra(VideoCaptureActivity.LAST_USER_UUID, movie.getLastUserId());
                intent.putExtra(VideoCaptureActivity.LAST_USER_AVATAR, movie.getLastUserAvatar());
                ((ChannelActivity)context).
                        startActivityForResult(intent, ChannelActivity.VIDEOCAPTURE_REQUEST);
//                    startActivity(intent);
            }
        }
    }

    // like click listener
    class LikeClickListener implements ImageView.OnClickListener {
        String uuid;
        Movie movie;
        ImageView view;
        TextView likesNum;
        boolean trial;
        int count;

        public LikeClickListener(ImageView view, TextView likesNum,
                                 String uuid, Movie movie, boolean trial)
        {
            this.uuid = uuid;
            this.movie = movie;
            this.view = view;
            this.likesNum = likesNum;
            this.trial = trial;
            this.count=0;
        }

        @Override
        public void onClick(View v) {
            fireGAnalyticsEvent("ui_action", "touch", "likeButton"+(isTrial?"_trial":""), null);

            if (trial) {
                count++;
                if (count==1){
                    Toast.makeText(context, "You can \"like\" the video after signing in via app menu.", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(context, "Sign in already! ;)", Toast.LENGTH_SHORT).show();
                    count=0;
                }
                return;
            }
            // first, invert the likes
            movie.setUserLikes(!movie.isUserLikes());

            if (movie.isUserLikes()) {
                movie.setLikeCount(movie.getLikeCount() + 1);
                view.setImageResource(R.drawable.ic_star_on);
                likesNum.setText(shortLikesString(movie.getLikeCount()));
//                userLikes.add(movie.getId()+"");
            } else {
                movie.setLikeCount(movie.getLikeCount() - 1);
                view.setImageResource(R.drawable.ic_star_off);
                likesNum.setText(shortLikesString(movie.getLikeCount()));
//                userLikes.remove(movie.getId()+"");
            }

            if (!trial)
            {
                String url = PrefUtility.getApiUrl(ServerUtility.API_STORY_LIKE, "uuid=" + this.uuid + "&story=" + movie.getId());

                if (!movie.isUserLikes())
                {
                    url = PrefUtility.getApiUrl(ServerUtility.API_STORY_DISLIKE, "uuid=" + this.uuid + "&story=" + movie.getId());
                }


                aq.auth(PrefUtility.getBasicHandle()).ajax(url, JSONObject.class, new AjaxCallback<JSONObject>() {
                    @Override
                    public void callback(String url, JSONObject json,
                                         AjaxStatus status) {
                        if (json != null) {
                            // successful ajax call
                            Log.v(LOGTAG, "likeCb success: " + json.toString());
                            // TODO: update like icon, and increase the count.
                            // TODO: collateral?
                        } else {
                            // ajax error, o change
                            ErrorUtility.apiError(LOGTAG,
                                    "likeCb: Json null ", status, context, false, Log.ERROR);
                        }
                    }
                });
            }
        }

    }

    // --------- HELPERS & CALLBACKS

    public void onNewPressed() {
        // call the same method from parent Activity
        ((ChannelActivity)context).onNewPressed(mChanId);
    }

    public void onCastClickedCb()
    {
        fireGAnalyticsEvent("ui_action", "touch", "cast", null);
    }

    private ImageView.OnClickListener mOnShareClicked = new ImageView.OnClickListener() {

        @Override
        public void onClick(View view) {
            fireGAnalyticsEvent("ui_action", "touch", "share", null);
//        new ShareDialog().show( ((ChannelActivity)context).getSupportFragmentManager(), "ShareDialog");
            if (view.getTag()==null) return;

            String link = view.getTag().toString();

            String message = "A StoryRoll clip" + ":\n\n" + link;

            IntentUtility.sendShare((Activity)context, "Check this StoryRoll clip", message);
        }
    };

    private ImageView.OnClickListener mOnShareInstaClicked = new ImageView.OnClickListener() {

        @Override
        public void onClick(View view) {
            fireGAnalyticsEvent("ui_action", "touch", "share_insta", null);
            if (view.getTag()==null) return;
            String link = view.getTag().toString();
            IntentUtility.sendShareInstagram((Activity)context, link);
        }
    };

    // TODO: improve to show like 1.5m
    private String shortLikesString(Integer num) {
        if (num == null || num < 1)
            return "0";
        if (num >= 1000000)
            return num / 1000000 + "m";
        if (num >= 1000)
            return num / 1000 + "k";
        return num + "";
    }

    private int lastTrackedPos=-1;

    public void resetAutoplayTracker() {
        lastTrackedPos=-1;
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
                         int visibleItemCount, int totalItemCount) {
        Log.v(LOGTAG, "onScroll:  " +firstVisibleItem);

        int topRowVerticalPosition =
                (view == null || view.getChildCount() == 0) ?
                        0 : view.getChildAt(0).getTop();
        ((ChannelActivity)context).setEnabledSwipeContainer(topRowVerticalPosition >= 0);

        boolean stopCurrent = false;
        if (currentlyPlayed!=null) {
            // stop previously played roll when it is scrolled out of active range
            int[] location = new int[2];
            currentlyPlayed.getLocationOnScreen(location);
            int viewCenterY = location[1] + calculcatedVideoWidth/2;
            if (viewCenterY<=autoRangeTop || viewCenterY>=autoRangeBottom)
            {
//					Log.v(LOGTAG, "onScroll: current video exits active range");
                // gotcha! stop it
                currentlyPlayed.queueStopVideo();
                currentlyPlayed = null;
            }

        }

        // only autostart if the tab was freshly switched, or shown for the first time

        if (lastTrackedPos==-1 && visibleItemCount>0) {
            // TODO: if it's currently selected fragment, autoplay, if not, schedule

            MovieItemView pv = (MovieItemView) ((ViewGroup)view).getChildAt(FIRST_VIDEO_ITEM_IDX); // normally 0, but we have the top item as "new roll" controll
            if (pv!=null)
            {
					Log.v(LOGTAG, "onScroll: starting freshly shown channel's first video");

                lastTrackedPos = firstVisibleItem;
                if (currentlyPlayed!=null) {
                    currentlyPlayed.queueStopVideo();
                    currentlyPlayed = null;
                }
                ControlledMovieView videoView = (ControlledMovieView) pv.findViewById(R.id.videoPlayerView);
                handleAutostart(pv, videoView);
            }
        }
    }



    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        Log.v(LOGTAG, "onScrollStateChanged:  " +scrollState);

        // autostart here - when the scoll event is over (state idle)
        if (AbsListView.OnScrollListener.SCROLL_STATE_IDLE == scrollState)
        {
            int first = view.getFirstVisiblePosition();
            int last = view.getLastVisiblePosition();
				Log.v(LOGTAG, "onScrollStateChanged: first " +first+" last "+last + " lastTrackedPos "+lastTrackedPos);

            // find the first VideoView that falls in the "active" range
            boolean found = false;
            for (int current=first, i=0; current<=last && !found; current++, i++)
            {
                View v = view.getChildAt(i);
                if (!(v instanceof MovieItemView))
                    continue;
                MovieItemView pv = (MovieItemView)v;
                ControlledMovieView videoView = (ControlledMovieView) pv.findViewById(R.id.videoPlayerView);
                int[] location = new int[2];
                videoView.getLocationOnScreen(location);
                int viewCenterY = location[1] + calculcatedVideoWidth/2;
					Log.d(LOGTAG, "onScrollStateChanged: "+current+" videoView's center y location: "+viewCenterY);

                if (viewCenterY>autoRangeTop && viewCenterY<autoRangeBottom) {
                    // that's the one
                    found = true;
                    if (lastTrackedPos!=current) {
							Log.d(LOGTAG, "onScrollStateChanged: new roll in active range: "+current);
                        lastTrackedPos = current;
                        handleAutostart(pv, videoView);
                    }
                }


            }
        }
    }

    // autostart first visible video, video/network settings permitting
    private void handleAutostart(MovieItemView pv, ControlledMovieView videoView) {


			Log.v(LOGTAG, "handleAutostart: first visible item's position in list: "+videoView.getItemPosition());
        AutostartMode am = PrefUtility.getAutostartMode();
			Log.v(LOGTAG, "autostartMode = "+am.toString());

        boolean autoStart = false;
        switch (am) {
            case ALWAYS:
                autoStart = true;
                break;
            case WIFI:
                autoStart = NetworkUtility.isWifiConnected(context.getApplicationContext());
//				Log.v(LOGTAG, "wificonnected: "+autoStart);
                break;
            default:
                break;
        }
        if (autoStart) {
            videoView.startVideoPreloading(true, false);
        }

        fireGAnalyticsEvent("handle_autostart", "mode", am.toString(), null);
        fireGAnalyticsEvent("handle_autostart", "autoStart", autoStart+"", null);

    }

    protected void fireGAnalyticsEvent(String category, String action, String label, Long value) {
        EasyTracker.getInstance(context).send(MapBuilder
                        .createEvent(category, action, label, value)
                        .build()
        );
    }

    // stop currently played video and start new one, setting it as currently played
    // needed to avoid playing two or more videos at once
    // using this as a central point for play event counter
    public void switchCurrentlyPlayedMovie(ControlledMovieView v)
    {
        if (currentlyPlayed!=null && currentlyPlayed.isPlaying())
        {
//            currentlyPlayed.stopPlayback();
            currentlyPlayed.pauseVideo();

            currentlyPlayed.indicatePlayable(true);
        }
        currentlyPlayed = v;

        // hide "newness" indicator
        v.indicateSeenPlayable();

        // fire an event about new video start
        // don't track in trial, but track on Google Analytics
        if (isTrial) {
            fireGAnalyticsEvent(v.getVideoType(), "view", "anonymous", null);
        }
        else {
            fireGAnalyticsEvent(v.getVideoType(), "view", v.getUuid(), null);
            if (v.getVideo() instanceof Movie) {
                String apiUrl = PrefUtility.getApiUrl(ServerUtility.API_VIEW_ADD, "story=" + v.getMovieId() + "&uuid=" + v.getUuid());
                aq.ajax(apiUrl, JSONObject.class, this, "addViewCb");
            }
        }
    }

}
