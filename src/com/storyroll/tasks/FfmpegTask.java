package com.storyroll.tasks;

import ru.jecklandin.stickman.vp.ProcessingService;
import android.content.Intent;
import android.graphics.Interpolator.Result;
import android.os.AsyncTask;
import android.util.Log;

public class FfmpegTask extends AsyncTask<String, Void, Result> {
	
	public interface OnFfmpegTaskCompleted{
	    void onFfmpegTaskCompleted(boolean success);
	}
	
	private static final String TAG = "FFMPEG_TASK";
    
	private OnFfmpegTaskCompleted listener;
	private boolean mSuccess;
	private ProcessingService ps;
	
    public FfmpegTask(OnFfmpegTaskCompleted listener, ProcessingService ps) {
        this.listener = listener;
        mSuccess = true;
        this.ps = ps;
    }

    @Override
    public void onPreExecute() {
    	
    }


    @Override
    protected Result doInBackground(String... params) {
        final String paramArgs = params[0];
        final String paramArgs1 = params[1];

//        ps.process(paramArgs);
//        Intent intent = new Intent(ProcessingService.START_ACTION);
//        intent.setClass(, ProcessingService.class);
//		intent.putExtra("num", i);
//		intent.putExtra("name", mName);
//		startService(intent);
        return null;
    }
    
    @Override
    protected void onPostExecute(Result result){
        //your stuff
    	 Log.i(TAG, "onPostExecute");
        listener.onFfmpegTaskCompleted(mSuccess);
    }
}