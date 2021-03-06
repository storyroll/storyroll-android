package co.storyroll.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import co.storyroll.R;
import co.storyroll.base.MenuActivity;
import co.storyroll.model.Profile;
import co.storyroll.util.AppUtility;
import co.storyroll.util.ImageUtility;
import co.storyroll.util.PrefUtility;
import co.storyroll.util.ServerUtility;
import com.androidquery.auth.FacebookHandle;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.util.AQUtility;
import com.bugsense.trace.BugSenseHandler;
import com.google.analytics.tracking.android.Fields;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;

public class ProfileActivity extends MenuActivity
{
	private final String LOGTAG = "PROFILE";
	private static final String SCREEN_NAME = "Profile";

	
//	private AQuery aq;
	
	private final int ACTIVITY_SELECT_AVATAR_REQUEST = 1001;
	
	private final static String STATE_USERNAME = "profile.state.username";
	private final static String STATE_EMAIL = "profile.state.email";
	private final static String STATE_PASSWORD = "profile.state.password";
	private final static String STATE_LOCATION = "profile.state.location";
	private final static String STATE_AUTH_METHOD = "profile.state.auth_method";
	private final static String STATE_AVATAR_URL = "profile.state.avatar";

	private final static String STATE_AVATAR_CHANGE_STARTED = "profile.state.avatarChangeStarted";
	private final static String STATE_AVATAR_CHANGE_COMPLETED = "profile.state.avatarChangeCompleted";


	private FacebookHandle handle;
	
	protected Profile profile = null;
	protected boolean registration;

	private boolean avatarChangeStarted = false;
	private boolean avatarChangeCompleted = false;
	private Bundle storedState = null;
	
	@Override
	protected void onCreate(Bundle state)
    {
		super.onCreate(state);
		
		Log.v(LOGTAG, "onCreate");
		setContentView(R.layout.activity_profile);
		// Fields set on a tracker persist for all hits, until they are
	    // overridden or cleared by assignment to null.
	    getGTracker().set(Fields.SCREEN_NAME, SCREEN_NAME);

    	Intent intent = getIntent();
    	if(intent != null){
    		registration = intent.getBooleanExtra("registration", false);
    		Log.v(LOGTAG, "onCreate, intent::registration: "+registration);
    	}
    	
		// state stored? keep it until later
		if (state!=null){
			storedState = state;
		}	
    	
    	if (registration) {
    		aq.id(R.id.profileMessage).text(R.string.msg_registration);
//    		profile = getPersistedProfile();
    		profile = (Profile)intent.getExtras().get("profile");
    		Log.v(LOGTAG, "profile from extras: "+profile.toString());
    		initForm();
    		restoreState();
    		initAvatar();
    		
    		// make username if it's empty
			if (TextUtils.isEmpty(profile.username)) {
				profile.username = profile.email.split("@")[0];
				aq.id(R.id.user_name).text(profile.username);
			}

    	}
    	else if (state!=null){
    		// restore state
    		profile = getPersistedProfile();
    		initForm();
    		restoreState();
    		initAvatar();
    	} else {
    		// edit profile
    		aq.auth(basicHandle).progress(R.id.progress).ajax(PrefUtility.getApiUrl(ServerUtility.API_PROFILE, "uuid=" + getUuid()),
    				JSONObject.class, this, "getProfileForEditCb");
    	}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
		Log.v(LOGTAG, "onSave, storing username: "+aq.id(R.id.user_name).getText().toString());
		outState.putBoolean(STATE_AVATAR_CHANGE_STARTED, avatarChangeStarted);
		outState.putBoolean(STATE_AVATAR_CHANGE_COMPLETED, avatarChangeCompleted);
		outState.putString(STATE_USERNAME, aq.id(R.id.user_name).getText().toString());
		outState.putString(STATE_PASSWORD, aq.id(R.id.password).getText().toString());
		outState.putString(STATE_LOCATION, aq.id(R.id.location).getText().toString());
		outState.putInt(STATE_AUTH_METHOD, profile!=null?profile.authMethod:Profile.AUTH_UNKNOWN);

	}

	
	private void initForm() {
		aq.id(R.id.done_button).enabled(true).clicked(this, "doneButtonClicked");
		aq.id(R.id.avatar).clicked(this, "avatarImageClicked");
		
		aq.id(R.id.user_name).text(profile.username);
		if (profile.location!=null && !"null".equals(profile.location)) {
			aq.id(R.id.location).text(profile.location);
		}
		
		if (profile.isAuthFacebook()) {
			Log.d(LOGTAG, "isFacebook");
			handle = AppUtility.makeHandle(this);
			// disable email and hide pass fields
			aq.id(R.id.email).text(profile.email);
			aq.id(R.id.email).enabled(false);
			aq.id(R.id.password).visibility(View.GONE);
		}
		else {
			aq.id(R.id.email).text(profile.email);
			aq.id(R.id.password).text(profile.password);
		}
		if (!registration) {
			aq.id(R.id.password).visibility(View.GONE);
		}
	}

	public void restoreState(){
		if (storedState!=null)
		{
			avatarChangeStarted = storedState.getBoolean(STATE_AVATAR_CHANGE_STARTED);
			avatarChangeCompleted = storedState.getBoolean(STATE_AVATAR_CHANGE_COMPLETED);
			Log.d(LOGTAG, "restoring state, avatarChanged: "+avatarChangeStarted+", "+avatarChangeCompleted);

			aq.id(R.id.email).text( storedState.getString(STATE_EMAIL) );
			aq.id(R.id.user_name).text( storedState.getString(STATE_USERNAME) );
			aq.id(R.id.password).text( storedState.getString(STATE_PASSWORD) );
			aq.id(R.id.location).text( storedState.getString(STATE_LOCATION) );
			profile.authMethod = storedState.getInt(STATE_AUTH_METHOD);
			profile.setAvatarUrl(storedState.getString(STATE_AVATAR_URL));

		}
	}
	
	public void initAvatar() {
		// see if we can get avatar from API
		if (!avatarChangeStarted && !avatarChangeCompleted) 
		{
			// do we have SR avatar?
			if (!TextUtils.isEmpty(profile.getAvatarUrl()))
            {
				Log.v(LOGTAG, "loading avatar from SR");
				aq.id(R.id.avatar).image(profile.getAvatarUrl(),
						false, false, 0, R.drawable.ic_avatar_default);

			}
			// otherwise load avatar from FB
			else if (profile.isAuthFacebook())
			{
                Log.v(LOGTAG, "loading avatar from Facebook");

                // TODO: refactor
                // hack here
//                AbstractAjaxCallback.setSSF(SSLSocketFactory.getSocketFactory());

				String tb = ImageUtility.getFbProfileTb(handle);
                // todo: clear cached image, which is sometimes picked up here
				aq.id(R.id.avatar).image(tb);

                // TODO: refactor
                // hack pt2: restore SSL Factory
//                AbstractAjaxCallback.setSSF(MainApplication.getSocketFactory());
			}
		}
		
		// but maybe avatar changed, yet not uploaded? show it from local dir
		if (avatarChangeCompleted)  {
			aq.id(R.id.avatar).getImageView().setImageURI(Uri.fromFile(new File(AppUtility.getAppWorkingDir(this)+File.separator+"avatar.jpg")));
		}
	}
	

	
	// - - - callbacks
	
	public void doneButtonClicked(View button)
    {
		Log.v(LOGTAG, "doneButtonClicked");
        aq.id(R.id.done_button).enabled(false);

        fireGAnalyticsEvent("ui_activity", "click", "doneButton", null);
		
		String formUsername = aq.id(R.id.user_name).getText().toString().trim();
		
		// TODO validate form
		if (TextUtils.isEmpty(formUsername) ) {
			Toast.makeText(aq.getContext(), R.string.msg_uname_required, Toast.LENGTH_SHORT).show();
            aq.id(R.id.done_button).enabled(true);
            return;
		}
		
		boolean unameChanged = !formUsername.equals(profile.username);
		Log.v(LOGTAG, "unameChanged: "+unameChanged);
		
		// TODO: store/send to server new/updated profile
		profile.email = aq.id(R.id.email).getText().toString().trim();
		profile.username = formUsername;
		profile.location = aq.id(R.id.location).getText().toString().trim();
		profile.password = aq.id(R.id.password).getText().toString().trim();
		
//		if (registration) {
//			if (profile.isAuthEmail() && (TextUtils.isEmpty(profile.password) || TextUtils.isEmpty(profile.email)) ) {
//				Toast.makeText(aq.getContext(), R.string.msg_password_email_required, Toast.LENGTH_SHORT).show();
//				return;
//			}
//			Log.d(LOGTAG, "profile: "+profile.toString()+", params: "+profile.toParamString(false, true));
//			
//			aq.progress(R.id.progress).ajax(AppUtility.API_URL+"addProfile?"+profile.toParamString(false, true), JSONObject.class, this, "createProfileCb");						
//		}
//		else {
			persistProfile(profile);
			profile = getPersistedProfile();
			aq.auth(basicHandle).progress(R.id.progress).ajax(PrefUtility.getApiUrl(
					ServerUtility.API_PROFILE_UPDATE, 
					profile.toParamString(unameChanged, false, false)
				), 
				JSONObject.class, this, "updateProfileCb");
//		}		
	}

	
	public void getProfileForEditCb(String url, JSONObject json, AjaxStatus status) throws JSONException
	{
		Log.v(LOGTAG, "getProfileForEditCb");
		if (isAjaxErrorThenReport(status)) return;
		
		if(json != null){ 
			profile = populateProfileFromSrJson(json, true, null);
		}
		else {
			apiError(LOGTAG, "Error getting profile.", status, true, Log.ERROR);
		}
		initForm();
		restoreState();
		initAvatar();
	}

	public void createProfileCb(String url, JSONObject json, AjaxStatus status)
	{
		Log.v(LOGTAG, "createProfileCb");
		// profile register successfull or fail?
		fireGAnalyticsEvent("profile", "create", json != null?"success":"fail", null);
		
		updateProfileGeneral(url, json, status);
	}
	
	public void updateProfileCb(String url, JSONObject json, AjaxStatus status)
	{
		Log.v(LOGTAG, "updateProfileCb");
		fireGAnalyticsEvent("profile", "update", json != null?"success":"fail", null);
		updateProfileGeneral(url, json, status);
	}
	
	protected boolean updateProfileGeneral(String url, JSONObject json, AjaxStatus status)
	{		
		if (status.getCode()==500) 
		{
			if (status.getError().contains("already present")) {
				Log.w(LOGTAG, "Username already present");
				Toast.makeText(this, R.string.msg_uname_not_unique, Toast.LENGTH_SHORT).show();
                aq.id(R.id.done_button).enabled(true);
                return false;
			}
		}
		if (isAjaxErrorThenReport(status)) {
            aq.id(R.id.done_button).enabled(false);
            return false;
		}
		
        if(json != null)
        {
            //successful ajax call
        	Log.v(LOGTAG, "updateProfileGeneral success");
        	// persist profile
        	profile.loggedIn = true;
        	persistProfile(profile);
        	
        	// if avatar wasn't changed before registering facebook user, download facebook avatar
        	if (!avatarChangeCompleted && profile.isAuthFacebook() && registration ) 
        	{
        		// store resized avatar
        		String tb = ImageUtility.getFbProfileTb(handle);
        		File file = aq.makeSharedFile(tb, "avatar.jpg");
                if (file!=null)
                {
                    File localAvatarFile = new File(AppUtility.getAppWorkingDir(this) + File.separator + "avatar.jpg");
                    if (localAvatarFile.exists()) {
                        localAvatarFile.delete();
                    }
                    file.renameTo(localAvatarFile);
                    avatarChangeCompleted = true;
                }
        	}
        	
        	// upload avatar
    		if (avatarChangeCompleted) 
    		{
                // delay to avoid race conditions, see https://github.com/storyroll/storyroll-android/issues/209
                Log.v(LOGTAG, "postDelayed:AvatarUpdater");
                AQUtility.postDelayed(new AvatarUpdater(getApplicationContext()), 3000);
    		}
    		else {
    			nextActivity();
    		}
        }else{          
        	apiError(LOGTAG, "Could not update profile", status, true, Log.ERROR);
            aq.id(R.id.done_button).enabled(false);
            return false;
        }
        return true;
	}

    private class AvatarUpdater implements Runnable {
        Context ctx;
        public AvatarUpdater(Context c) {
            ctx = c;
        }
        @Override
        public void run() {
            doUploadAvatar(ctx, profile.email);
        }
    }

    private void doUploadAvatar(Context ctx, String email){
        File file = new File(AppUtility.getAppWorkingDir(ctx)+File.separator+"avatar.jpg");

        HttpEntity reqEntity = MultipartEntityBuilder.create()
                .addBinaryBody("file", file, APPLICATION_OCTET_STREAM, "avatar.jpg")
                .addTextBody("uuid", email, ContentType.TEXT_PLAIN).build();

        aq.auth(basicHandle).progress(R.id.progress).post(PrefUtility.getApiUrl(ServerUtility.API_AVATAR_SET),
                reqEntity, JSONObject.class, new AjaxCallback<JSONObject>() {

                    @Override
                    public void callback(String url, JSONObject json, AjaxStatus status) {
                        Log.v(LOGTAG, "API_AVATAR_SET callback: json=" + (json == null ? "null" : json.toString()));
                        if (json!=null)
                        {
                            setAvatarCb(url, json, status);
                        }
                        else {
                            apiError(LOGTAG, "Error uploading avatar image on registration: "+registration, status, false, Log.ERROR);
                        }
                    }
                });
    }
	
	public void setAvatarCb(String url, JSONObject json, AjaxStatus status)
	{
		fireGAnalyticsEvent("profile_avatar", "setAvatar", json != null?"success":"fail", null);

		if (isAjaxErrorThenReport(status)) return;
		
        if(json != null){               
            //successful ajax call
        	Log.v(LOGTAG, "setAvatarCb success: "+json.toString());
        	// reset flag
        	avatarChangeCompleted = false;
        	// set avatar flag in profile
//        	profile.avatar = json.getJSONObject("avatar").getInt("id");
        	Log.v(LOGTAG, "set avatarUrl to "+profile.getAvatarUrl());
			persistProfile(profile);			
        }else{          
        	apiError(LOGTAG, "Could not store avatar", status, true, Log.ERROR);
        }
		nextActivity();
	}
	
	private void nextActivity() {
        aq.id(R.id.done_button).enabled(false);
        Intent intent = new Intent(getApplicationContext(), AppUtility.ACTIVITY_HOME);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}
	
	private static Uri outputFileUri;
	
	public void avatarImageClicked(View ImageView)
	{
		fireGAnalyticsEvent("ui_action", "touch", "avatarImage", null);
		
		avatarChangeStarted = true;
		// Determine Uri of camera image to save.
		final File appRootDir = new File(AppUtility.getAppWorkingDir(this));
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
	        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
	        cameraIntents.add(intent);
	    }

	    // Filesystem.
	    final Intent galleryIntent = new Intent();
	    galleryIntent.setType("image/*");
	    galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
	    // don't return to this activity if they relaunch your application from the Launcher
	    galleryIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

	    // Chooser of filesystem options.
	    final Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Source");

	    // Add the camera options.
	    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));
	    // don't return to this activity if they relaunch your application from the Launcher
	    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

	    startActivityForResult(chooserIntent, ACTIVITY_SELECT_AVATAR_REQUEST);
	}

	// - - - callbacks & helpers 

	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v(LOGTAG, "onActivityResult");

	        switch(requestCode) {
	        
	        case ACTIVITY_SELECT_AVATAR_REQUEST:
	        {
	        	Log.v(LOGTAG, "onActivityResult: ACTIVITY_SELECT_AVATAR_REQUEST");
	    		fireGAnalyticsEvent("profile_avatar", "selectAvatar", resultCode == RESULT_OK?"RESULT_OK":"RESULT_"+resultCode, null);

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
		                	Bitmap bt=Bitmap.createScaledBitmap(bitmap, Profile.AVATAR_WIDTH, Profile.AVATAR_HEIGHT, false);
		                	Log.v(LOGTAG, "setting avatar");
		                	aq.id(R.id.avatar).getImageView().setImageBitmap(bt);
		                	
		                	// store resized avatar
		                	FileOutputStream out = new FileOutputStream(AppUtility.getAppWorkingDir(this)+File.separator+"avatar.jpg");
		                    bt.compress(Bitmap.CompressFormat.JPEG, 85, out);
		                    out.flush();
		                    out.close();
		                    
		                    avatarChangeCompleted = true;

						} catch (FileNotFoundException e) {
							BugSenseHandler.sendException(e);
							Log.e(LOGTAG, "File not found saving avatar", e);
							e.printStackTrace();
						} catch (IOException e) {
							BugSenseHandler.sendException(e);
							Log.e(LOGTAG, "I/O error saving avatar", e);
							e.printStackTrace();
						}

	                }
	        	}
                avatarChangeStarted = false;
                initAvatar();
	        }
	        
	        }
	}


}
