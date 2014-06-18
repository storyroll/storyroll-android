package co.storyroll.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Channel {
	private String title = null;
	private long id = -1L;
	private String thumbnailFile = null;
    private boolean publicChannel = false;
    private String thumbUrl = null;
    private long createdOn = 0;
    private long lastActivityTime = 0;
    private int userCount = 0;
    private List<String> userAvatars = new ArrayList<String>();
	
	public Channel() {
	}
	
	public Channel(long id, String name) {
		this.id = id;
		this.title = name;		
	}

	public Channel(JSONObject chanObj) throws JSONException {
		id = chanObj.getLong("id");
		createdOn = chanObj.getLong("createdOn");
        lastActivityTime = chanObj.getLong("lastActivityTime");
        title = chanObj.getString("title");
		thumbnailFile = chanObj.getString("thumbnailFile");
        publicChannel = chanObj.getBoolean("publicChannel");
        thumbUrl = chanObj.getString("thumbUrl");
        userCount = chanObj.getInt("userCount");

        JSONArray users = chanObj.getJSONArray("5Users");
        if (users!=null) {
            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);
                userAvatars.add(user.getString("avatarUrl"));
            }
        }
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

    public long getLastActivityTime() {
        return lastActivityTime;
    }

    public void setLastActivityTime(long lastActivityTime) {
        this.lastActivityTime = lastActivityTime;
    }

    public int getUserCount() {
        return userCount;
    }

    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }

    public List<String> getUserAvatars() {
        return userAvatars;
    }

    public void setUserAvatars(List<String> userAvatars) {
        this.userAvatars = userAvatars;
    }
}
