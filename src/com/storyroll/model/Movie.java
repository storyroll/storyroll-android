package com.storyroll.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Movie extends Clip {
	long lastClipId = -1L;
	String lastUserUuid = null;
	String[] cast = null;
	long publishedOn = 0;
	
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

	public Movie(JSONObject obj) throws JSONException {
		super(obj);
		lastClipId = obj.getLong("lastClipId");
		lastUserUuid = obj.getString("lastUserUuid");
		publishedOn = obj.getLong("publishedOn");
	}
	
	@Override
	public int compareTo(Clip another) {
		 return new Long( ((Movie)another).getPublishedOn() ).compareTo(new Long(getPublishedOn()));
	}

}
