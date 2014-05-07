package com.storyroll.model;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class Story implements Serializable, Comparable<Story> {
	private long id;
	private int likes;
	private long publishedOn = 0;
	private boolean completed;
	private boolean published;
	private boolean userLikes = false;
	private boolean unseen = true;
	
	private String[] cast = null;
	
	public Story(long id, int likes) {
		this.id = id;
		this.likes = likes;
	}
	
	public Story(JSONObject storyObj) throws JSONException {
		id = storyObj.getLong("id");
		likes = storyObj.getInt("likes");
		published = storyObj.getBoolean("published");
		publishedOn = storyObj.getLong("publishedOn");
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

	public boolean isUnseen() {
		return unseen;
	}

	public void setUnseen(boolean unseen) {
		this.unseen = unseen;
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

	@Override
	public int compareTo(Story another) {
		 return new Long(another.getId()).compareTo(new Long(getId()));
	}
	
	
	
}
