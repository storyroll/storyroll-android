package co.storyroll.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class Clip implements Serializable, Comparable<Clip> {
	protected long id;
    protected long createdOn = 0;
	
	private boolean userLikes = false;
	private boolean unseen = true;
    protected String thumbUrl;
    protected String fileUrl;

    public Clip() {
    }

	public Clip(long id) {
		this.id = id;
	}
	
	public Clip(JSONObject videoObj) throws JSONException {
		id = videoObj.getLong("id");
		createdOn = videoObj.getLong("createdOn");
		thumbUrl = videoObj.getString("thumbUrl");
        fileUrl = videoObj.getString("fileUrl");
	}
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}

	@Override
	public int compareTo(Clip another) {
		 return new Long(another.getId()).compareTo(new Long(getId()));
	}

	public boolean isUnseen_() {
		return unseen;
	}

	public void setUnseen_(boolean unseen) {
		this.unseen = unseen;
	}

	public boolean isUserLikes() {
		return userLikes;
	}

	public void setUserLikes(boolean userLikes) {
		this.userLikes = userLikes;
	}

	public String getThumbUrl() {
		return thumbUrl;
	}

	public void setThumbUrl(String thumbUrl) {
		this.thumbUrl = thumbUrl;
	}

	public String getFileUrl() {
		return fileUrl;
	}

	public void setFileUrl(String url) {
		this.fileUrl = url;
	}

	
	
}
