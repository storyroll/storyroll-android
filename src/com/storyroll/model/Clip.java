package com.storyroll.model;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class Clip implements Serializable, Comparable<Clip> {
	private long id;
	private long createdOn = 0;
	
	public Clip(long id) {
		this.id = id;
	}
	
	public Clip(JSONObject blinkObj) throws JSONException {
		id = blinkObj.getLong("id");
		createdOn = blinkObj.getLong("createdOn");
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
	
}
