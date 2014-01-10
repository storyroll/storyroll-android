package com.storyroll.activity;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
		profile.email = aq.id(R.id.email).getText().toString().trim();
		profile.password = aq.id(R.id.password).getText().toString().trim();
		profile.authMethod = Profile.AUTH_EMAIL;
		
		if (TextUtils.isEmpty(profile.password) || TextUtils.isEmpty(profile.email) ) {
            Toast.makeText(aq.getContext(), R.string.msg_password_email_required, Toast.LENGTH_SHORT).show();
			return;
		}
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
	    				String apiUrl = AppUtility.API_URL + "hasUser?uuid="+profile.email;
						aq.progress(R.id.progress).ajax(apiUrl, JSONObject.class, LoginActivity.this, "hasUserCb");
						
					} catch (JSONException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
            }else{
            	apiError(LOGTAG, "Error: "+status.getMessage()+" ("+ status.getCode()+")");
            }
    }
    
	public void hasUserCb(String url, JSONObject json, AjaxStatus status) throws JSONException{
		Log.v(LOGTAG, "hasUserCb");
		boolean userExists = false;
		if(json!=null){
			userExists = json.getBoolean("result");
		}
		Log.i(LOGTAG, "checkUserExistsBooleanCb user exists: "+ userExists);

		if (userExists) {
			// ...
			profile.loggedIn = true;
			persistProfile(profile);
			startActivity(new Intent(getApplicationContext(), RollFlipPlayActivity.class));
		}
		else {
			// user not in db, go to registration
			nextActionRegister();
		}
    }
	
	public void getSrProfileCb(String url, JSONObject json, AjaxStatus status) throws JSONException{
		Log.v(LOGTAG, "getSrProfileCb");
		if(json != null){
			
			// user exists, check password
			String srPassword = json.getString("password");
			if (!srPassword.equals(aq.id(R.id.password).getText().toString().trim())) {
	        	Toast.makeText(aq.getContext(), "Password incorrect, change it and try again.", Toast.LENGTH_LONG).show();
	        	return;
			}
			
			// update profile and mark as signed in
			profile = populateProfileFromSrJson(json, true);
			profile.loggedIn = true;
			persistProfile(profile);

			startActivity(new Intent(getApplicationContext(), RollFlipPlayActivity.class));
		
		}else{
			// user not in db, go to registration
			nextActionRegister();
	  }
	}
	
	private void nextActionRegister() {
		if (connectedViaFacebook) {
			profile.authMethod = Profile.AUTH_FACEBOOK;
		}
		else {
			profile.authMethod = Profile.AUTH_EMAIL;
		}
		Intent intent =  new Intent(this, ProfileActivity.class);
		intent.putExtra("registration", true);
		intent.putExtra("profile", profile);
		startActivity(intent);
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
