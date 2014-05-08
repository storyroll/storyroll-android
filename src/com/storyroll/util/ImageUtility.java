package com.storyroll.util;

import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import com.androidquery.auth.FacebookHandle;
import com.storyroll.model.Profile;

public class ImageUtility {

	private static final String LOGTAG = "ImageUtility";

	public static String getFbProfileTb(FacebookHandle handle){
		
		if(!handle.authenticated()) return null;
		
		String pic = "https://graph.facebook.com/me/picture?width="+Profile.AVATAR_WIDTH+"&height="+Profile.AVATAR_WIDTH;
		pic = handle.getNetworkUrl(pic);
		return pic;
	}
	
	public static String getSrProfileTb(FacebookHandle handle, String uuid){
		
		if(!handle.authenticated()) return null;
		
		String pic = PrefUtility.getApiUrl(ServerUtility.API_AVATAR, "uuid="+uuid);
		pic = handle.getNetworkUrl(pic);
		return pic;
	}

	public static void sliderAnimateRightToLeft(ImageView v) 
	{
		Log.v(LOGTAG, "slider animation starts");
		v.setVisibility(View.VISIBLE);
		TranslateAnimation animation = new TranslateAnimation(0.0f, -v.getWidth()*4, 0.0f, 0.0f);
		animation.setDuration(1000);
		animation.setRepeatCount(1);
		animation.setRepeatMode(Animation.RESTART);
		animation.setFillAfter(false);
		v.startAnimation(animation);
		v.setVisibility(View.GONE);
	}
}
