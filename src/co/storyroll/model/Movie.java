package co.storyroll.model;

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
	private String[] cast = null;
    private long publishedOn = 0;
    private boolean seen = false;
    private String lastUserAvatar = null;
    private int clipCount = 0;
    private List<Clip> clips = new ArrayList<Clip>();
    private int likeCount = 0;


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

	public String[] getCast() {
		return cast;
	}

	public void setCast(String[] cast) {
		this.cast = cast;
	}
	
	public long getPublishedOn() {
		return publishedOn;
	}

	public void setPublishedOn(long publishedOn) {
		this.publishedOn = publishedOn;
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

        if (obj.has("clips")) {
            JSONArray clipsJsonArr = obj.getJSONArray("clips");
            for (int i=0;i<clipsJsonArr.length();i++) {
                clips.add(new Clip(clipsJsonArr.getJSONObject(i)));
            }
        }
	}
	
	@Override
	public int compareTo(Clip another) {
		 return new Long( ((Movie)another).getPublishedOn() ).compareTo(new Long(getPublishedOn()));
	}

}
