package com.storyroll.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Channel {
	private String name;
	private long id;
	private long createdOn;
	
	public Channel() {
		name = null;
		id = -1;
		createdOn = -1;
	}
	
	public Channel(long id, String name) {
		this.id = id;
		this.name = name;
	}

	public Channel(JSONObject chanObj) throws JSONException {
		id = chanObj.getLong("id");
		createdOn = chanObj.getLong("createdOn");
		name = chanObj.getString("name");
	}
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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
