package com.storyroll.util;

import com.androidquery.auth.FacebookHandle;
import com.storyroll.model.Profile;

public class ImageUtility {

	public static String getFbProfileTb(FacebookHandle handle){
		
		if(!handle.authenticated()) return null;
		
		String pic = "https://graph.facebook.com/me/picture?width="+Profile.AVATAR_WIDTH+"&height="+Profile.AVATAR_WIDTH;
		pic = handle.getNetworkUrl(pic);
		return pic;
	}
	
	public static String getSrProfileTb(FacebookHandle handle, String uuid){
		
		if(!handle.authenticated()) return null;
		
		String pic = AppUtility.API_URL+"avatar?uuid="+uuid;
		pic = handle.getNetworkUrl(pic);
		return pic;
	}

}
