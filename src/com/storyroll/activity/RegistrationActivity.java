package com.storyroll.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.androidquery.auth.FacebookHandle;
import com.androidquery.callback.AjaxStatus;
import com.bugsense.trace.BugSenseHandler;
import com.google.analytics.tracking.android.Fields;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.storyroll.R;
import com.storyroll.base.Constants;
import com.storyroll.base.MenuActivity;
import com.storyroll.model.Profile;
import com.storyroll.util.AppUtility;
import com.storyroll.util.PrefUtility;
import com.storyroll.util.ServerUtility;

public class RegistrationActivity extends ProfileActivity {
	private final String LOGTAG = "REGISTER";
	private static final String SCREEN_NAME = "Registration";
	
	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		
		// Fields set on a tracker persist for all hits, until they are
	    // overridden or cleared by assignment to null.
	    getGTracker().set(Fields.SCREEN_NAME, SCREEN_NAME);
	}

	
	// - - - callbacks
	
	@Override
	public void doneButtonClicked(View button){
		Log.v(LOGTAG, "doneButtonClicked");
		
		fireGAnalyticsEvent("ui_activity", "click", "doneButton", null);
		
		String formUsername = aq.id(R.id.user_name).getText().toString().trim();
		boolean unameChanged = !formUsername.equals(profile.username);
		Log.v(LOGTAG, "unameChanged: "+unameChanged);
		
		// TODO: store/send to server new/updated profile
		profile.email = aq.id(R.id.email).getText().toString().trim();
		profile.username = formUsername;
		profile.location = aq.id(R.id.location).getText().toString().trim();
		profile.password = aq.id(R.id.password).getText().toString().trim();
		
		if (registration) 
		{
			if (profile.isAuthEmail() && (TextUtils.isEmpty(profile.password) || TextUtils.isEmpty(profile.email)) ) 
			{
				Toast.makeText(aq.getContext(), R.string.msg_password_email_required, Toast.LENGTH_SHORT).show();
				return;
			}
			Log.d(LOGTAG, "profile: "+profile.toString()+", params: "+profile.toParamString(false, true));	
			aq.progress(R.id.progress).ajax(PrefUtility.getApiUrl(
					ServerUtility.API_PROFILE_ADD, profile.toParamString(false, true)), 
					JSONObject.class, this, "createProfileCb");						
		}
		else 
		{
			persistProfile(profile);
			profile = getPersistedProfile();
			aq.progress(R.id.progress).ajax(PrefUtility.getApiUrl(
					ServerUtility.API_PROFILE_UPDATE, profile.toParamString(unameChanged, false)), 
					JSONObject.class, this, "updateProfileCb");
		}		
	}
	
    
	@Override
	public void createProfileCb(String url, JSONObject json, AjaxStatus status)
	{
		Log.v(LOGTAG, "createProfileCb");
		// profile register successfull or fail?
		
		
		fireGAnalyticsEvent("profile", "create", json != null?"success":"fail", null);
		
		if (updateProfileGeneral(url, json, status)) 
		{
			
			// registered successfull, now register with GCM and update required field
			Log.v(LOGTAG, "register success, now register with GCM");
			
	        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
	        if (checkPlayServices()) {
	            gcm = GoogleCloudMessaging.getInstance(this);
	//            regid = getRegistrationId(context);
	//
	//            if (regid.isEmpty()) {
	                gcmRegisterInBackground();
	//            }
	        } else {
	            Log.w(LOGTAG, "No valid Google Play Services APK found.");
	        }
	        
		}
	}
	

}
