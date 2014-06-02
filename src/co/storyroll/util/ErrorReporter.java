/*******************************************************************************
 * Copyright 2014 StoryRoll (martynas@ore.lt)
 * 
 ******************************************************************************/
package co.storyroll.util;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Looper;
import android.widget.Toast;
import com.androidquery.util.AQUtility;
import com.bugsense.trace.BugSenseHandler;

import java.util.Date;

public class ErrorReporter implements Thread.UncaughtExceptionHandler{

	private static Context context;
	
	public static void installReporter(Context appContext){
		
		try{
			
			Thread.setDefaultUncaughtExceptionHandler(new ErrorReporter());
			context = appContext;
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void report(final String message){
		
		AQUtility.debug("report", message);
		
		IllegalStateException ex = new IllegalStateException(message);
		report(ex, true);
		
	}
	
	
	private static long last;
	private static long MIN_GAP = 2000;
	
	public static void report(final Throwable ex, boolean async){
		
		
		
		if(async){
			AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

				@Override
				protected Void doInBackground(Void... params) {
					reportBlock(ex);
					return null;
				}
			};
			
			task.execute();
		}else{
			
			reportBlock(ex);
			
		}
		
		
		
	}
	
	private static void reportBlock(Throwable ex){
		
	    if(PrefUtility.isTestDevice()) return;
	    
	    try{
	    
	    	if(ex instanceof java.lang.reflect.InvocationTargetException){
	    		ex = ((java.lang.reflect.InvocationTargetException) ex).getTargetException();
	    	}
	    	
	    	
	    	long now = System.currentTimeMillis();
	    	if(now - last < MIN_GAP){
	    		return;
	    	}
	    	
	    	last = now;
	    	
	    	Date localDate = new Date();
//		    int i = 0;
//		    StringWriter localStringWriter = new StringWriter();
//		    PrintWriter localPrintWriter = new PrintWriter(localStringWriter);
//		    ex.printStackTrace(localPrintWriter);	    	
//		    BugSense.submitError(i, localDate, localStringWriter.toString());
//		    BugSenseHandler.sendExceptionMessage("date", localDate.toString(), (Exception)ex);
	    	BugSenseHandler.sendException((Exception)ex);
		    
		    AQUtility.debug("submit error");
	    }catch(Exception e){
	    	e.printStackTrace();
	    }
		
		
	}
	
	
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {

		AQUtility.debug("Error reported. We will fix it soon!");
        
		ex.printStackTrace();
        AppUtility.reportRemote(ex);
		
		report(ex, false);
		
		wait(ex);
		
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
		
	}
	
	private void wait(Throwable ex){
		
		synchronized(ex){
			try {
				ex.wait(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	
	private void showToast(final String message){
	
		
		new Thread() {
	
	        @Override
	        public void run() {
	            Looper.prepare();
	            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	            Looper.loop();
	        }
	
	    }.start();
	
	}
	
	
}
