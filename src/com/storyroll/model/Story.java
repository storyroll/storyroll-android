package com.storyroll.model;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class Story implements Serializable {
	long id;
	int likes;
	boolean completed;
	boolean published;
	boolean userLikes = false;
	String[] cast = null;
	
	public Story(long id, int likes) {
		this.id = id;
		this.likes = likes;
	}
	
	public Story(JSONObject storyObj) throws JSONException {
		id = storyObj.getLong("id");
		likes = storyObj.getInt("likes");
		published = storyObj.getBoolean("published");
	}
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public int getLikes() {
		return likes;
	}
	public void setLikes(int likes) {
		this.likes = likes;
	}
	public boolean isCompleted() {
		return completed;
	}
	public void setCompleted(boolean completed) {
		this.completed = completed;
	}
	public boolean isPublished() {
		return published;
	}
	public void setPublished(boolean published) {
		this.published = published;
	}

	public boolean isUserLikes() {
		return userLikes;
	}

	public void setUserLikes(boolean userLikes) {
		this.userLikes = userLikes;
	}

	public String[] getCast() {
		return cast;
	}

	public void setCast(String[] cast) {
		this.cast = cast;
	}
	
	
	
}
