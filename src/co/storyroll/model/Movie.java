package co.storyroll.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Movie extends Clip {
	private static final String LOGTAG = "Movie";
	long lastClipId = -1L;
    String lastClipUrl = null;
	String lastUserUuid = null;
	String[] cast = null;
	long publishedOn = 0;
	boolean seen = false;
	
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

    public Movie(JSONObject obj) throws JSONException {
		super(obj);
		lastClipId = obj.has("lastClipId")?obj.getLong("lastClipId"):-1L;
		lastUserUuid = obj.getString("lastUserUuid");
        lastClipUrl = obj.getString("lastClipUrl");
//		Log.v(LOGTAG, "lastUserUid: "+lastUserUuid);
		publishedOn = obj.getLong("publishedOn");
		seen = obj.getBoolean("seen");
	}
	
	@Override
	public int compareTo(Clip another) {
		 return new Long( ((Movie)another).getPublishedOn() ).compareTo(new Long(getPublishedOn()));
	}

}
