package co.storyroll.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Channel {
	private String title = null;
	private long id = -1L;
	private long createdOn = -1L;
	private String thumbnailFile = null;
	
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
	
}
