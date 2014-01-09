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

import com.androidquery.AQuery;
import com.androidquery.auth.FacebookHandle;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.storyroll.base.BaseActivity;
import com.storyroll.base.Constants;
import com.storyroll.base.LeftActionActivity;
import com.storyroll.base.MenuActivity;
import com.storyroll.model.Profile;
import com.storyroll.util.AppUtility;
import com.storyroll.util.ImageUtility;
import com.storyroll.R;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class ProfileActivity extends MenuActivity {
	private final String LOGTAG = "PROFILE";
	
//	private AQuery aq;
	
	private final int ACTIVITY_SELECT_AVATAR_REQUEST = 1001;

	private FacebookHandle handle;
	
	private Profile profile = null;
	
	private boolean registration;
	private boolean avatarChanged = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		Log.v(LOGTAG, "onCreate");
		
		setContentView(R.layout.activity_profile);

    	Intent intent = getIntent();
    	if(intent != null){
    		registration = intent.getBooleanExtra("registration", false);
    	}
    	
		profile = getPersistedProfile();

		aq.id(R.id.done_button).clicked(this, "doneButtonClicked");
		aq.id(R.id.avatar).clicked(this, "avatarImageClicked");
		aq.id(R.id.user_name).text(profile.username);
		aq.id(R.id.location).text(profile.location);
		
		if (profile.authMethod==Profile.AUTH_FACEBOOK) {
			handle = AppUtility.makeHandle(this);
			// disable email and hide pass fields
			aq.id(R.id.email).text(profile.email);
			aq.id(R.id.email).enabled(false);
			aq.id(R.id.password).visibility(View.GONE);
		}
		else {
			aq.id(R.id.email).text(profile.email);
		}
		// see if we can get avatar from API
		if (profile.avatar!=null) {
			Log.v(LOGTAG, "loading avatar");
			aq.id(R.id.avatar).image(AppUtility.API_URL+"avatar?uuid="+profile.email, false, false, 0, R.drawable.ic_avatar_default);
		}
		else if (profile.isAuthFacebook())
		// avatar from FB
		{
			String tb = com.storyroll.util.ImageUtility.getFbProfileTb(handle);
			aq.id(R.id.avatar).image(tb);
		}
		

	}
    
	// - - - callbacks
	
	
	public void doneButtonClicked(View button){
		Log.v(LOGTAG, "doneButtonClicked");
		// upload avatar if it was changed
		persistProfile(
				aq.id(R.id.email).getText().toString(),
				aq.id(R.id.user_name).getText().toString(),
				profile.avatar,
				profile.authMethod,
				profile.loggedIn
				);
		
		// TODO: store/send to server new/updated profile
		if (avatarChanged || (profile.isAuthFacebook() && registration)) {
			// upload avatar
			File file = new File(AppUtility.getAppWorkingDir()+File.separator+"avatar.jpg");
			
			Map params = new HashMap();
	        params.put("file", file);
	        params.put("uuid", profile.email);
	        aq.progress(R.id.progress).ajax(AppUtility.API_URL+"setAvatar", params, JSONObject.class, ProfileActivity.this, "setAvatarCb");
	        
		}
		else {
			if (profile.avatar!=null) {
				startActivity(new Intent(getApplicationContext(), RollFlipPlayActivity.class));
			}
			else {
	        	Toast.makeText(aq.getContext(), "Avatar must be set.", Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	public void setAvatarCb(String url, JSONObject json, AjaxStatus status) throws JSONException{
        
        if(json != null){               
            //successful ajax call
        	Log.v(LOGTAG, "setAvatarCb success: "+json.toString());
        	profile.avatar = json.getJSONObject("avatar").getInt("id");
        	Log.v(LOGTAG, "set avatar id to "+profile.avatar);
			persistProfile(profile);

			
        }else{          
            //ajax error
        	Log.e(LOGTAG, "Could not store avatar");
        	Toast.makeText(aq.getContext(), "Could not store avatar.", Toast.LENGTH_SHORT).show();
        }
		startActivity(new Intent(getApplicationContext(), RollFlipPlayActivity.class));
        
	}
	
	private static Uri outputFileUri;
	
	public void avatarImageClicked(View ImageView){
		// Determine Uri of camera image to save.
		final File appRootDir = new File(AppUtility.getAppWorkingDir());
		appRootDir.mkdirs();
		final File sdImageMainDirectory = new File(appRootDir, "avatar.jpg");
		outputFileUri = Uri.fromFile(sdImageMainDirectory);

		    // Camera.
		    final List<Intent> cameraIntents = new ArrayList<Intent>();
		    final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		    final PackageManager packageManager = getPackageManager();
		    final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
		    for(ResolveInfo res : listCam) {
		        final String packageName = res.activityInfo.packageName;
		        final Intent intent = new Intent(captureIntent);
		        intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
		        intent.setPackage(packageName);
		        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
		        cameraIntents.add(intent);
		    }

		    // Filesystem.
		    final Intent galleryIntent = new Intent();
		    galleryIntent.setType("image/*");
		    galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

		    // Chooser of filesystem options.
		    final Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Source");

		    // Add the camera options.
		    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));

		    startActivityForResult(chooserIntent, ACTIVITY_SELECT_AVATAR_REQUEST);
		}

	// - - - callbacks & helpers 

	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v(LOGTAG, "onActivityResult");

	        switch(requestCode) {
	        
	        case ACTIVITY_SELECT_AVATAR_REQUEST:
	        {
	        	if (resultCode == RESULT_OK){
		            final boolean isCamera;
		            if(data == null)
		            {
		                isCamera = true;
		            }
		            else
		            {
		                isCamera = MediaStore.ACTION_IMAGE_CAPTURE.equals(data.getAction());
		            }
	
		            Uri selectedImageUri = null;
		            if(isCamera)
		            {
		                selectedImageUri = outputFileUri;
		            }
		            else
		            {
		            	// gallery
		                selectedImageUri = data == null ? null : data.getData();
		                
		            }
		            Log.v(LOGTAG, "selectedImageUri: "+selectedImageUri);
		            if (selectedImageUri!=null){
	                	
						try {
							InputStream is = getContentResolver().openInputStream(selectedImageUri);
		                	Bitmap bitmap = BitmapFactory.decodeStream(is);
		                	is.close();
		                	Bitmap bt=Bitmap.createScaledBitmap(bitmap, 200, 200, false);  
		                	aq.id(R.id.avatar).getImageView().setImageBitmap(bt);
		                	
		                	// store resized avatar
		                	FileOutputStream out = new FileOutputStream(AppUtility.getAppWorkingDir()+File.separator+"avatar.jpg");
		                    bt.compress(Bitmap.CompressFormat.JPEG, 85, out);
		                    out.flush();
		                    out.close();
		                    
		                    avatarChanged = true;

						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

	                }
	        	}
	        }
	        
	        }
	}


}
