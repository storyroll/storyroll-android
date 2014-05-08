package com.storyroll.util;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.storyroll.model.Channel;
import com.storyroll.model.Story;

public class ModelUtility {
	
	private static final String LOGTAG = "ModelUtility";

	public static List<Channel> channels(JSONArray jarr) {
		ArrayList<Channel> channels = new ArrayList<Channel>();
		for (int i = 0; i < jarr.length(); i++) {
			JSONObject chanObj;
			try {
				chanObj = jarr.getJSONObject(i);
				Channel channel = new Channel(chanObj);
				channels.add(channel);
			}
			catch (JSONException e) {
				// TODO Auto-generated catch block
				Log.e(LOGTAG, "Error parsing channels", e);
				e.printStackTrace();
			}
		}
		return channels;
	}
	
//	public List<Story> stories(JSONArray jarr) {
//		for (int i = 0; i < jarr.length(); i++) {
//			JSONObject storyObj = jarr.getJSONObject(i);
//			Story story = new Story(storyObj);
//			if (story.isPublished()) {
//				// manually set userLikes flag
////				story.setUserLikes(userLikes.contains(story.getId()+""));
////				story.setUnseen(unseenStories.contains(story.getId()+""));
//				stories.add(story);
//			}
//		}
//	}
}
