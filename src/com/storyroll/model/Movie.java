package com.storyroll.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Movie extends Clip {
	long lastClipId = -1L;
	String lastUserUuid = null;
	String[] cast = null;
	
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
	
	public Movie(JSONObject obj) throws JSONException {
		super(obj);
		lastClipId = obj.getLong("lastClipId");
		lastUserUuid = obj.getString("lastUserUuid");
	}
}
