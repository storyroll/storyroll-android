package com.storyroll.model;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class Clip implements Serializable, Comparable<Clip> {
	private long id;
	private long createdOn = 0;
	
	private int likes = 0;
	private boolean userLikes = false;
	private boolean unseen = true;
	private String thumbUrl;
	String url;

	public Clip(long id) {
		this.id = id;
	}
	
	public Clip(JSONObject videoObj) throws JSONException {
		id = videoObj.getLong("id");
		createdOn = videoObj.getLong("createdOn");
		thumbUrl = videoObj.getString("thumbUrl");
		url = videoObj.getString("url");
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

	
	public boolean isUnseen() {
		return unseen;
	}

	public void setUnseen(boolean unseen) {
		this.unseen = unseen;
	}
	
	public int getLikes() {
		return likes;
	}
	public void setLikes(int likes) {
		this.likes = likes;
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

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	
	
}
