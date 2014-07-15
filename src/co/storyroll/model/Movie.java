package co.storyroll.model;

import android.util.Log;
import co.storyroll.ui.MovieItemView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Movie extends Clip {
	private static final String LOGTAG = "Movie";
    private long lastClipId = -1L;
    private String lastClipUrl = null;
    private String lastUserUuid = null;
	private List<Cast> cast = new ArrayList<Cast>(MovieItemView.MAX_SHOWN_CLIPS);
    private boolean seen = false;
    private String lastUserAvatar = null;
    private int clipCount = 0;
    private List<Clip> clips = new ArrayList<Clip>(MovieItemView.MAX_SHOWN_CLIPS);
    private int likeCount = 0;
    private boolean finished = true;
    private String playerUrl = null;
    private String quadPlayerUrl = null;
    private String quadFileUrl = null;


    public Movie(long id){
		super(id);
	}

	public long getLastClipId() {
		return lastClipId;
	}

	public void setLastClipId(long lastClipId) {
		this.lastClipId = lastClipId;
	}
	
	public String getLastUserId() {
		return lastUserUuid;
	}

	public void setLastUserId(String lastUserId) {
		this.lastUserUuid = lastUserId;
	}

	public List<Cast> getCast() {
		return cast;
	}

	public void setCast(List<Cast> cast) {
		this.cast = cast;
	}
	
	public boolean isSeen() {
		return seen;
	}

	public void setSeen(boolean seen) {
		this.seen = seen;
	}

    public String getLastClipUrl() {
        return lastClipUrl;
    }

    public void setLastClipUrl(String lastClipUrl) {
        this.lastClipUrl = lastClipUrl;
    }

    public String getLastUserAvatar() {
        return lastUserAvatar;
    }

    public void setLastUserAvatar(String lastUserAvatar) {
        this.lastUserAvatar = lastUserAvatar;
    }

    public int getClipCount() {
        return clipCount;
    }

    public void setClipCount(int clipCount) {
        this.clipCount = clipCount;
    }

    public int getLikeCount() {
        return likeCount;
    }
    public void setLikeCount(int likes) {
        this.likeCount = likes;
    }

    public List<Clip> getClips() {
        return clips;
    }

    public void setClips(List<Clip> clips) {
        this.clips = clips;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public String getPlayerUrl() {
        return playerUrl;
    }

    public void setPlayerUrl(String playerUrl) {
        this.playerUrl = playerUrl;
    }

    public String getQuadPlayerUrl() {
        return quadPlayerUrl;
    }

    public void setQuadPlayerUrl(String quadPlayerUrl) {
        this.quadPlayerUrl = quadPlayerUrl;
    }

    public String getQuadFileUrl() {
        return quadFileUrl;
    }

    public void setQuadFileUrl(String quadFileUrl) {
        this.quadFileUrl = quadFileUrl;
    }

    public Movie(JSONObject obj) throws JSONException {
        super();
        id = obj.getLong("id");
        createdOn = obj.getLong("createdOn");
        thumbUrl = obj.getString("thumbUrl");
        fileUrl = obj.getString("url"); // todo make same as in Clip
		lastClipId = obj.has("lastClipId")?obj.getLong("lastClipId"):-1L;
		lastUserUuid = obj.getString("lastUserUuid");
        lastClipUrl = obj.getString("lastClipUrl");
//		Log.v(LOGTAG, "lastUserUid: "+lastUserUuid);
		publishedOn = obj.getLong("publishedOn");
		seen = obj.getBoolean("seen");
        // todo: replace with real field
        lastUserAvatar = obj.getString("lastUserAvatarUrl");
        clipCount = obj.getInt("clipCount");
        likeCount = obj.getInt("likeCount");
        if (obj.has("finished")) {
            finished = obj.getBoolean("finished");
        }
        if (obj.has("clips")) {
            JSONArray clipsJsonArr = obj.getJSONArray("clips");
            for (int i=0;i<clipsJsonArr.length() && i<MovieItemView.MAX_SHOWN_CLIPS;i++) {
                JSONObject clipObj = clipsJsonArr.getJSONObject(i);
                Clip clip = new Clip(clipObj);
                clips.add(clip);
                JSONObject owner = clipObj.getJSONObject("owner");
                cast.add(new Cast(owner));
            }
        }
        if (clipCount >= MovieItemView.MAX_SHOWN_CLIPS) // todo: remove this clause?
        {
            if (obj.has("movieClip") && obj.get("movieClip")!=JSONObject.NULL) {
                JSONObject movieClip = obj.getJSONObject("movieClip");
                playerUrl = movieClip.getString("playerUrl");
            }
            if (obj.has("quadClip") && obj.get("quadClip")!=JSONObject.NULL) {
                Log.v(LOGTAG, "quadclip: "+obj.get("quadClip"));
                JSONObject movieClip = obj.getJSONObject("quadClip");
                quadPlayerUrl = movieClip.getString("playerUrl");
                quadFileUrl = movieClip.getString("mp4Url");
            }
        }

	}
	
	@Override
	public int compareTo(Clip another) {
		 return new Long( ((Movie)another).getPublishedOn() ).compareTo(new Long(getPublishedOn()));
	}

}
