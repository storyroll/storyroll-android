package com.storyroll;

import java.io.File;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.BitmapAjaxCallback;

import com.androidquery.util.AQUtility;
import com.bugsense.trace.BugSenseHandler;
import com.storyroll.base.Constants;
import com.storyroll.util.AppUtility;
import com.storyroll.util.ErrorReporter;
import com.storyroll.util.PrefUtility;

public class MainApplication extends Application implements Thread.UncaughtExceptionHandler{

	
	public static final String MOBILE_AGENT = "Mozilla/5.0 (Linux; U; Android 2.2) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533";		
	
	
	@Override
    public void onCreate() {     
        
		AQUtility.setContext(this);
        
		boolean test = PrefUtility.isTestDevice();
		System.err.println("test:" + test);
		
        if(test){
        	AQUtility.setDebug(true);
        }
        
        // MSA: don't track just yet
//        bugTracking();
        
        ErrorReporter.installReporter(AQUtility.getContext());
        
        AQUtility.setExceptionHandler(this);
        
        AQUtility.setCacheDir(null);
        
        AjaxCallback.setNetworkLimit(8);
        //AjaxCallback.setAgent(MOBILE_AGENT);
        
        BitmapAjaxCallback.setIconCacheLimit(200);
        BitmapAjaxCallback.setCacheLimit(80);
        BitmapAjaxCallback.setPixelLimit(400 * 400);
        BitmapAjaxCallback.setMaxPixelLimit(2000000);
        
        File workingDir = new File(AppUtility.getAppWorkingDir());
        workingDir.mkdirs();
        workingDir = new File(AppUtility.getVideoCacheDir(getApplicationContext()));
        workingDir.mkdirs();
        
        
        super.onCreate();
    }
	
//	private static final String API_KEY = "81009b75";
	private static final String API_KEY = "marsav";
	private void bugTracking(){
		
		try{
			AQUtility.debug("tracking!");
			BugSenseHandler.setup(this, API_KEY);	
		}catch(Exception e){
			AQUtility.debug(e);
		}
	}
	
	
	
	@Override
	public void onLowMemory(){	
    	BitmapAjaxCallback.clearCache();
    }
	
	public static Context getContext(){
		return AQUtility.getContext();
	}
	public static String get(int id){
		return getContext().getString(id);
	}
	

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		ErrorReporter.report(ex, true);
	}
	
}
