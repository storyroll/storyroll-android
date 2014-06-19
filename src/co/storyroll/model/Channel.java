package co.storyroll.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Channel {
	private String title = null;
	private long id = -1L;
	private String thumbnailFile = null;
    private boolean publicChannel = false;
    private String thumbUrl = null;
    private long createdOn = 0;
    private int userCount = 0;

	public Channel() {
	}
	
	public Channel(long id, String name) {
		this.id = id;
		this.title = name;		
	}

	public Channel(JSONObject chanObj) throws JSONException {
		id = chanObj.getLong("id");
		createdOn = chanObj.getLong("createdOn");
        title = chanObj.getString("title");
		thumbnailFile = chanObj.getString("thumbnailFile");
        publicChannel = chanObj.getBoolean("publicChannel");
        thumbUrl = chanObj.getString("thumbUrl");
        userCount = chanObj.getInt("userCount");

	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String name) {
		this.title = name;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(long createdOn) {
		this.createdOn = createdOn;
	}

    public boolean isPublic() {
        return publicChannel;
    }

    public void setPublic(boolean publicChannel) {
        this.publicChannel = publicChannel;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public void setThumbUrl(String thumbUrl) {
        this.thumbUrl = thumbUrl;
    }

    public int getUserCount() {
        return userCount;
    }

    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }

}
