package com.storyroll.activity;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import com.androidquery.auth.FacebookHandle;
import com.androidquery.callback.AjaxStatus;
import com.storyroll.R;
import com.storyroll.base.BaseActivity;
import com.storyroll.model.Profile;
import com.storyroll.util.AppUtility;

public class LoginActivity extends BaseActivity {
	private final static String LOGTAG = "LOGIN";
	private final static String facebookGraphUrl = "https://graph.facebook.com/me?fields=first_name,last_name,name,email,location";
	private final int ACTIVITY_SSO = 1000;

	private FacebookHandle handle;
	private Profile profile = null;
	private boolean connectedViaFacebook = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		handle = AppUtility.makeHandle(this);
		
		aq.id(R.id.facebook_button).clicked(this, "facebookButtonClicked");
		aq.id(R.id.done_button).clicked(this, "doneButtonClicked");

	}
    
	
	// - - - callbacks
	
	public void facebookButtonClicked(View button){
		authFacebookSSO();
    }
	
	public void authFacebookSSO(){
		Log.v(LOGTAG, "authFacebookSSO");
		handle.sso(ACTIVITY_SSO);
		aq.auth(handle).progress(R.id.progress).ajax(facebookGraphUrl, JSONObject.class, this, "facebookProfileCb");
	}
	
	public void doneButtonClicked(View button){
		// login via API
		profile = new Profile();
		profile.email = aq.id(R.id.email).getText().toString();
		String apiUrl = AppUtility.API_URL + "getProfile?uuid="+profile.email;
		aq.ajax(apiUrl, JSONObject.class, LoginActivity.this, "getSrProfileCb");
	}

	// - - - callbacks & helpers 
    public void facebookProfileCb(String url, JSONObject json, AjaxStatus status) {
		Log.v(LOGTAG, "facebookProfileCb");
            
            if(json != null){
				try {
	                    //successful ajax call
						Log.v(LOGTAG, "facebookProfileCb: Facebook connected");
	                    Toast.makeText(aq.getContext(), "Facebook connected", Toast.LENGTH_SHORT).show();
	                    connectedViaFacebook = true;
	                    
	                    populateProfileFromFbJson(json);
	                    
	                    // query API, user exists?
	    				String apiUrl = AppUtility.API_URL + "getProfile?uuid="+profile.email;
						aq.ajax(apiUrl, JSONObject.class, LoginActivity.this, "getSrProfileCb");
						
					} catch (JSONException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
            }else{
                    
                    //ajax error, show error code
                    Toast.makeText(aq.getContext(), "Error: "+status.getMessage()+" ("+ status.getCode()+")", Toast.LENGTH_LONG).show();
            }
    }
    
	public void getSrProfileCb(String url, JSONObject json, AjaxStatus status){
		Log.v(LOGTAG, "getSrProfileCb");
		if(json != null){
			
			// user exists, update profile and mark as signed in
			try {
				populateProfileFromSrJson(json);
				profile.loggedIn = true;
				persistProfile(profile);

				startActivity(new Intent(getApplicationContext(), RollFlipPlayActivity.class));

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	  
		
		}else{
		  // user not in db, add user to db first
		  aq.ajax(AppUtility.API_URL + "addUser?uuid="+profile.email, JSONObject.class, this, "addUserCb");
	  }
	}

	public void addUserCb(String url, JSONObject json, AjaxStatus status){
	        
        if(json != null){               
            //successful ajax call
        	Toast.makeText(aq.getContext(), "Registered successfully", Toast.LENGTH_LONG).show();
        	Log.v(LOGTAG, "addUserCb: "+json.toString());
        	try {
				populateProfileFromSrJson(json);
				profile.loggedIn = true;
				if (connectedViaFacebook) {
					profile.authMethod = Profile.AUTH_FACEBOOK;
				}
				else {
					profile.authMethod = Profile.AUTH_EMAIL;
				}
				persistProfile(profile);
				
				Intent intent =  new Intent(this, ProfileActivity.class);
				intent.putExtra("registration", true);
				startActivity(intent);
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }else{          
            //ajax error
        	Log.e(LOGTAG, "addUserCb: could not register user");
        	Toast.makeText(aq.getContext(), "There was an error registering user", Toast.LENGTH_LONG).show();
        }
	}
    
    // populate profile from facebook response
	public void populateProfileFromFbJson(JSONObject json) throws JSONException{
//		String tb = com.storyroll.util.ImageUtility.getFbProfileTb(handle);
		Log.i(LOGTAG, json.toString());
		if (profile==null) {
			profile = new Profile();
		}
		profile.email = json.getString("email");
		profile.username = json.getString("first_name");
		JSONObject location = json.getJSONObject("location");
		profile.location = location.getString("name");
	}
	
    // populate profile from StoryRoll API response
	public void populateProfileFromSrJson(JSONObject json) throws JSONException{
		if (profile==null) {
			profile = new Profile();
		}
		profile.email = json.getString("uuid");
		// in case no name set yet, make one from email
		if (profile.username==null || "".equals(profile.username.trim())) {
			profile.username = profile.email.trim().split("@")[0];
		}
		// set avatar id
		if (json.has("avatar") && !json.isNull("avatar")) {
			Log.v(LOGTAG, "has avatar");
			JSONObject avatarJson = json.getJSONObject("avatar");
			profile.avatar = avatarJson.getInt("id");
		}
		else
			Log.v(LOGTAG, "has NO avatar");
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v(LOGTAG, "onActivityResult");

	        switch(requestCode) {
	                
	        case ACTIVITY_SSO: {
	    		Log.v(LOGTAG, "ACTIVITY_SSO");

	                if(handle != null){
	                        handle.onActivityResult(requestCode, resultCode, data);   
	                }
	        		
	        		Log.v(LOGTAG, "facebook authenticated: "+handle.authenticated());
	        		if (handle.authenticated()) {
	        			aq.auth(handle).progress(R.id.progress).ajax(facebookGraphUrl, JSONObject.class, this, "facebookProfileCb");
	        		}

	                break;
	        }
	
	        }
	}


}
