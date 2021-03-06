package co.storyroll.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.util.Log;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.Toast;
import co.storyroll.PQuery;
import co.storyroll.R;
import co.storyroll.base.Constants;
import co.storyroll.enums.AutofocusMode;
import co.storyroll.enums.AutostartMode;
import co.storyroll.enums.ServerPreference;
import co.storyroll.tasks.ClearCacheTask;
import co.storyroll.util.*;
import com.androidquery.auth.BasicHandle;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.util.AQUtility;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import org.json.JSONObject;

import java.io.File;

//import com.androidquery.simplefeed.util.AppUtility;
//import com.androidquery.simplefeed.util.DialogUtility;
//import com.androidquery.simplefeed.util.IntentUtility;
//import com.androidquery.simplefeed.util.ParseUtility;
//import com.androidquery.simplefeed.util.PrefUtility;



public class SettingsActivity extends PreferenceActivity implements OnPreferenceClickListener, OnPreferenceChangeListener, ClearCacheTask.OnClearCacheCompleted {

	private static final String LOGTAG = "SETTINGS";
	private static final String SCREEN_NAME = "Settings";
	
//	protected FacebookHandle handle;
	protected PQuery aq;
    protected BasicHandle basicHandle = null;
	
//	http://stackoverflow.com/questions/2240326/remove-hide-a-preference-from-the-screen
//	@Override
//    protected void onCreate(final Bundle savedInstanceState)
//    {
//        super.onCreate(savedInstanceState);
//        EasyTracker.getInstance(this).set(Fields.SCREEN_NAME, SCREEN_NAME);
//        
//        ActionBarUtility.initCustomActionBar(this);
//		ActionBarUtility.adjustActionBarLogoCentering(this);
//        
//		aq = new PQuery(this);
//		
//        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
//        
//        initView();
//    }
//
//    public static class MyPreferenceFragment extends PreferenceFragment
//    {
//        @Override
//        public void onCreate(final Bundle savedInstanceState)
//        {
//            super.onCreate(savedInstanceState);
//            addPreferencesFromResource(R.xml.settings);
//        }
//    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
    	
    	super.onCreate(savedInstanceState);
    	
		// Fields set on a tracker persist for all hits, until they are
	    // overridden or cleared by assignment to null.
	    EasyTracker.getInstance(this).set(Fields.SCREEN_NAME, SCREEN_NAME);
	    
    	ActionBarUtility.initCustomActionBar(this, true);
//		ActionBarUtility.adjustActionBarLogoCentering(this);

        addPreferencesFromResource(R.xml.settings);
        
        
        aq = new PQuery(this);
//        handle = AppUtility.makeHandle(this);
        basicHandle = AppUtility.makeBasicHandle(this);
        
        initView();
    }
   
 
    
    private void initView(){
    	Preference p;
        PreferenceCategory prefCategory = (PreferenceCategory) findPreference("category_settings");
        PreferenceCategory prefCategory1 = (PreferenceCategory) findPreference("category_settings1");

    	if (AppUtility.isLoggedIn()) {
    		p = findPreference("logout");
            p.setOnPreferenceClickListener(this);
            
            // remove login
            p = findPreference("login");
            prefCategory.removePreference(p);
    	}
    	else {
    		p = findPreference("login");
            p.setOnPreferenceClickListener(this);
            // remove logout
            p = findPreference("logout");
            prefCategory.removePreference(p);
    	}

        
//        p = findPreference("share");
//        p.setOnPreferenceClickListener(this);
//        p.setTitle(p.getTitle() + " @Facebook");
//        
        p = findPreference("share_others");
        p.setOnPreferenceClickListener(this);
//        
//        
//        p = findPreference("review");
//        p.setOnPreferenceClickListener(this);
        
        p = findPreference("report");
        p.setOnPreferenceClickListener(this);
        
        p = findPreference("version");
        String version = getVersion();
        p.setSummary(version);
        p.setOnPreferenceClickListener(this);
        
        // todo feedback not yet implemented?

//        p = findPreference("feedback");
//        p.setOnPreferenceClickListener(this);
        
        p = findPreference("co.storyroll.enums.AutostartMode");
        p.setOnPreferenceChangeListener(this);

        p = findPreference("co.storyroll.enums.AutofocusMode");
        p.setOnPreferenceChangeListener(this);

        p = findPreference("co.storyroll.enums.ServerPreference");
        if (PrefUtility.isTestDevice())
        {
            p.setOnPreferenceChangeListener(this);
        }
        else {
            Log.v(LOGTAG, "hiding pref");
            prefCategory1.removePreference(p);
        }

        p = findPreference("cache");
        p.setOnPreferenceClickListener(this);
    }


    public boolean onPreferenceClick(Preference preference){
    	
    	String name = preference.getKey();
    	
    	AQUtility.debug("pref", name);
    	
    	try{
    		fireGAnalyticsEvent("ui_activity", "pref", name, null);

	    	if("logout".equals(name)){
	    		loginLogout(true);
	    	}else if("login".equals(name)){
	    		loginLogout(false);
	    	}else if("share".equals(name)){
	    		share();
	    	}else if("share_others".equals(name)){
	    		share2();
	    	}else if("review".equals(name)){
	    		review();
	    	}else if("feedback".equals(name)){
	    		feedback();
	    	}else if("report".equals(name)){
	    		report();
	    	} else if ("cache".equals(name)){
                clear_cache();
            } else if ("version".equals(name)){
                versionClick();
            }
    	}
    	catch(Exception e){
    		AQUtility.report(e);
    	}
    	
    	return false;
    	
    }

    int versionClicks = 0;
    private void versionClick() {
        Log.v(LOGTAG, "versionClicks: "+versionClicks);
        if (++versionClicks>5) {
            PrefUtility.setTestDevice(true);
            Toast.makeText(this, "Test mode enabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void clear_cache() {
        Log.v(LOGTAG, "clearing cache");
        ClearCacheTask task = new ClearCacheTask(this.getApplicationContext(), this);
        task.execute();
    }

    private void share2(){
    	
    	String title = getString(R.string.share_app_message);
    	String link = IntentUtility.getWebMarketUrl(this);
    	
    	String message = title + ":\n\n" + link;
    	
    	
    	IntentUtility.sendShare(this, title, message);
    }
    
    
    private void report(){
    	String title = getString(R.string.report_problem);
    	String text = "- - -\n\nPlease report your problem above. Your device details below will help investigating.\n\nDevice: "+
    		getDeviceName() + 
    		"\nFirmware: "+
    		Build.VERSION.RELEASE+
    		"\nKernel: "+
    		System.getProperty("os.version");
    	IntentUtility.sendEmail(this, title, text);
    }
    
    public String getDeviceName() {
    	  String manufacturer = Build.MANUFACTURER;
    	  String model = Build.MODEL;
    	  if (model.startsWith(manufacturer)) {
    	    return capitalize(model);
    	  } else {
    	    return capitalize(manufacturer) + " " + model;
    	  }
    	}


	private String capitalize(String s) {
	  if (s == null || s.length() == 0) {
	    return "";
	  }
	  char first = s.charAt(0);
	  if (Character.isUpperCase(first)) {
	    return s;
	  } else {
	    return Character.toUpperCase(first) + s.substring(1);
	  }
	} 
    
    ///PROFILE_ID/links 	Publish a link on the given profile 	link, message, picture, name, caption, description
    
    private void share(){
    	
    	String message = getString(R.string.share_app_message);
    	String title = getString(R.string.share) + " @Facebook";
    	
    	DialogUtility.askYesNo(this, title, message, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                AlertDialog d = (AlertDialog) dialog;
                EditText edit = (EditText) d.findViewById(R.id.input);

                String message = edit.getEditableText().toString().trim();

                AQUtility.debug("send message", message);

                if (message.length() > 0) {
                    shareSend(message);
                }
            }
        });
    	
    	
    }
    
	private void shareSend(String message){
		
//		String url = "https://graph.facebook.com/me/links";
//		
//		Map<String, Object> params = new HashMap<String, Object>();
//		
//		String link = IntentUtility.getWebMarketUrl(this);
//		//String message = getString(R.string.share_app_message);
//		//String picture = "http://androidquery.appspot.com/z/images/simplefb/share.png";
//		
//		params.put("message", message);
//		params.put("link", link);
//		//params.put("picture", picture);
//		
//		AQUtility.debug("params", params);
//		
//		progress2(true, getString(R.string.sharing));
//		
//		aq.auth(handle).ajax(url, params, String.class, new AjaxCallback<String>(){
//			
//			@Override
//			public void callback(String url, String object, AjaxStatus status) {
//				
//				progress2(false, null);
//				
//				showToast(getString(R.string.shared));
//				
//			}
//			
//		});
//		
//		
		
	}
    
    protected void showToast(String message) {
      	
    	Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
    	toast.setGravity(Gravity.CENTER, 0, 0);
    	toast.show();
    	
    }
	
    private void review(){
    	
    	IntentUtility.openMarket(this);
    	
    }
    
	private String getVersion(){		
		return getPackageInfo().versionName;		
	}
	
	private static PackageInfo pi;
	private PackageInfo getPackageInfo(){
		
		if(pi == null){
			try {
				pi = getPackageManager().getPackageInfo(getAppId(), 0);
				
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
		}
		return pi;
	}
    
	private String getAppId(){
		return getApplicationInfo().packageName;
	}
	
    private void loginLogout(boolean isLogout)
    {
    	if (isLogout){
    		AppUtility.logout(this); // todo: this call - only for Facebook-registered
        	Log.w(LOGTAG, "logout");
        	
    		// first of all, remove associated GCM reg id from the db record
    		String apiRegUrl = PrefUtility.getApiUrl(ServerUtility.API_PROFILE_UPDATE, "uuid=" + getUuid() + "&registrationId= ");
    		aq.auth(basicHandle).progress(R.id.progress).ajax(apiRegUrl, JSONObject.class, this, "removeProfileGcmRegCb");
    		
        	AppUtility.purgeProfile(this);
    		
        	// delete avatar picture
    		File file = new File(AppUtility.getAppWorkingDir(this)+File.separator+"avatar.jpg");
    		if (file.exists()) {
    			file.delete();
    		}
        	
    		// TODO: empty cache
    	}
    	
		// go to Home activity
    	Intent intent = new Intent(this, AppUtility.ACTIVITY_HOME);
    	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	intent.putExtra("logout", true);
    	startActivity(intent);
    }
    
	public void removeProfileGcmRegCb(String url, JSONObject json, AjaxStatus status){
		Log.v(LOGTAG, "removeProfileGcmRegCb");
		fireGAnalyticsEvent("profile", "gcm_remove", json != null?"success":"fail", null);
		
        if(json != null){
            //successful ajax call
        	Log.v(LOGTAG, "GcmRegCb remove success");
        }else{          
        	ErrorUtility.apiError(LOGTAG, "Could not remove GCM registration id from profile", status, this, false, Log.ERROR);
        }
	}
    

    private void feedback(){
    	
//    	Entity source = new Entity();
//    	source.setId("304050252938418");
//    	source.setName(getString(R.string.app_name));
//    	source.setTb(ParseUtility.profileTb(source.getId()));
//    	
//    	FeedActivity.start(this, source);
    	
    }
    

    
	@Override
	public boolean onPreferenceChange(Preference pref, Object newValue) 
	{
		Log.v(LOGTAG, "onPreferenceChange: "+newValue);
        fireGAnalyticsEvent("settings", pref.getKey(), newValue.toString(), null);

        if (pref.getKey().equals("co.storyroll.enums.ServerPreference")) {
			PrefUtility.putEnum(ServerPreference.valueOf(newValue.toString()));
			// restart
	    	Intent intent = new Intent(this, LaunchActivity.class);
	    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
	    	startActivity(intent);
		}
		else if (pref.getKey().equals("co.storyroll.enums.AutostartMode"))
		{
			PrefUtility.putEnum(AutostartMode.valueOf(newValue.toString()));
		}
        else if (pref.getKey().equals("co.storyroll.enums.AutofocusMode"))
        {
            Log.d(LOGTAG, "new value: "+newValue.toString());
            PrefUtility.putEnum(AutofocusMode.valueOf(newValue.toString()));
        }
		
		return true;
	}

    // ------------ low level and helper methods

    private void progress2(boolean show, String message){

        if(show){

            ProgressDialog dia = getProgressDialog();
            dia.setMessage(message);

            aq.show(dia);
        }else{

            aq.dismiss();
        }

    }

    private ProgressDialog getProgressDialog(){

        ProgressDialog progress = null;

        if(progress == null){
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setIndeterminate(true);
            dialog.setCancelable(true);
            dialog.setInverseBackgroundForced(false);
            dialog.setCanceledOnTouchOutside(true);
            progress = dialog;
        }
        return progress;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        aq.dismiss();
    }

    protected void fireGAnalyticsEvent(String category, String action, String label, Long value) {
        EasyTracker.getInstance(this).send(MapBuilder
                        .createEvent(category, action, label, value)
                        .build()
        );
    }

	@Override
    public void onResume(){
    	super.onResume();
    }

	protected String getUuid() {
		SharedPreferences settings = getSharedPreferences(Constants.PREF_PROFILE_FILE, 0);
		String uuid = settings.getString(Constants.PREF_EMAIL, null);
		String username = settings.getString(Constants.PREF_USERNAME, null);
		Log.i(LOGTAG, "uuid: " + uuid + ", username: " + username);
		return uuid;
	}

    @Override
    public void onClearCacheCompleted(boolean success, Exception e) {
        if (success) {
            Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show();
        }
    }
}
