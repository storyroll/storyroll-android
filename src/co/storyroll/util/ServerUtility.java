package co.storyroll.util;

import android.content.Context;
import android.util.Log;
import co.storyroll.base.Constants;
import com.androidquery.callback.AjaxStatus;
import org.json.JSONException;
import org.json.JSONObject;

public class ServerUtility {
	private static final String LOGTAG = "ServerUtility";

	public static final String API_AVATAR_SET = "setAvatar";

    public static final String API_CHANNEL_CREATE = "channel/create";
    public static final String API_CHANNELS = "channels";
    public static final String API_CHAN_MOVIES = "videosInChannel";
    public static final String API_CHANNEL_LEAVE = "channel/{channelId}/leave/{uuid}/"; // has to end with /

    @Deprecated
    public static final String API_CLIPS_AVAILABLE = "available"; //
	public static final String API_CLIP_ADD = "addFragment";
    @Deprecated
	public static final String API_CLIP_FILE = "fragmentFile"; //
    @Deprecated
	public static final String API_CLIP_THUMB = "clipThumb"; //

    public static final String API_INVITES_ADD = "invites/add";
    public static final String API_INVITES_IN_CHANNEL = "channels/invites"; // params: c=channelId
    public static final String API_INVITES_PENDING = "invites/pending"; // params: uuid
    public static final String API_INVITES_ACCEPT="invites/accept";
    public static final String API_INVITES_DECLINE="invites/decline";

//    public static final String API_LOGIN_VALID = "loginValid";

	public static final String API_MOVIE_THUMB = "movieThumb";

	public static final String API_PROFILE = "getProfile";
	public static final String API_PROFILE_ADD = "addProfile";
	public static final String API_PROFILE_UPDATE = "updateProfile";

	public static final String API_SERVER_PROPERTIES = "getServerProperties";
	
	public static final String API_STORIES_LIKED_BY_USER = "userLikes"; //
	public static final String API_STORIES_BEST = "getHotStories"; //
	public static final String API_STORIES_PUBLISHED_BY_USER = "getUserPublishedStories"; //
	public static final String API_STORIES_LATEST_BY_USER = "getLatestPublishedStories"; //

	public static final String API_CAST = "getStoryCast"; // get some kind of cast
	public static final String API_STORY_LIKE = "like";
	public static final String API_STORY_DISLIKE = "dislike";

	public static final String API_UNSEEN_CLIPS = "unseenStories"; //
	public static final String API_UNSEEN_STORIES = "unseenStories"; //
	public static final String API_UNSEEN_MOVIES = "unseenStories"; 

	public static final String API_USER_EXISTS = "hasUser";

	public static final String API_VIEW_ADD = "addView";

	// obsolete
	public static final String API_STORY_FILE = "storyFile"; //
	public static final String API_STORY_THUMB = "storyThumb"; //
//	public static final String API_MOVIE_FILE = "storyFile";


	public static void getServerPropertiesCb(String url, JSONObject json, AjaxStatus status, Context ctx)
	{
		if(json != null)
        {               
            //successful ajax call
        	Log.i(LOGTAG, "getServerPropertiesCb success: "+json.toString());
        	// update property
        	String ls="";
        	try {
				ls = json.getString(Constants.SERV_PREF_VIDEO_DURATION);
				int l = Integer.valueOf(ls);
				if (l<100) { l=l*1000; };
				CameraUtility.VIDEO_LENGTH = l;
				Log.v(LOGTAG,  "server property "+Constants.SERV_PREF_VIDEO_DURATION+": "+CameraUtility.VIDEO_LENGTH);
			} catch (JSONException e) {
				ErrorUtility.apiError(LOGTAG, "server property "+Constants.SERV_PREF_VIDEO_DURATION+" JSONException, "+e.getMessage(), status, ctx, false, Log.WARN);
			} catch (NumberFormatException e) {
				ErrorUtility.apiError(LOGTAG, "server property "+Constants.SERV_PREF_VIDEO_DURATION+" incorrect: "+ls, status, ctx, false, Log.WARN);
			}
        }
		else
        {          
            //ajax error
			ErrorUtility.apiError(LOGTAG, "API call getServerProperties failed.", status, ctx, false, Log.WARN);
        }
	}
}
