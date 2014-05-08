package com.storyroll.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Movie extends Clip {
	long lastClipId = -1L;
	
	public Movie(long id){
		super(id);
	}

	public long getLastClipId() {
		return lastClipId;
	}

	public void setLastClipId(long lastClipId) {
		this.lastClipId = lastClipId;
	}
	
	public Movie(JSONObject obj) throws JSONException {
		super(obj);
		lastClipId = obj.getLong("lastClipId");
	}
}
