package co.storyroll.tasks;

import android.content.Context;
import android.graphics.Interpolator.Result;
import android.os.AsyncTask;
import android.util.Log;
import co.storyroll.util.AppUtility;

import java.io.File;

public class ClearCacheTask extends AsyncTask<String, Void, Result> {

	public interface OnClearCacheCompleted{
	    void onClearCacheCompleted(boolean success, Exception e);
	}

	private static final String TAG = "VIDEO_DOWNLOAD";
	private static final int TIMEOUT_CONNECTION = 5000;//5sec
	private static final int TIMEOUT_SOCKET = 30000;//30sec
	private static final String LOGTAG = "VideoDownloadTask";

	private Context mContext;
	private String fileName;
	private File file;
	private OnClearCacheCompleted listener;
	private boolean success = false;
	private Exception e = null;

    public ClearCacheTask(Context context, OnClearCacheCompleted listener) {
    	Log.v(LOGTAG, "constructor");
        mContext = context;
        this.listener = listener;
    }

    @Override
    public void onPreExecute() {
    	
    }


    @Override
    protected Result doInBackground(String... params)
    {
        try {
            String cacheDir = AppUtility.getVideoCacheDir(mContext.getApplicationContext());
            File dir = new File(cacheDir);
            if (dir != null && dir.isDirectory()) {
                File[] children = dir.listFiles();
                if (children == null) {
                    // Either dir does not exist or is not a directory
                    Log.e(LOGTAG, "Cache dir doesn't exist or is not a directory");
                } else {
                    File temp;
                    for (int i = 0; i < children.length; i++) {
                        temp = children[i];
                        Log.v(LOGTAG, "deleting: "+temp.getName());
                        temp.delete();
                    }
                }
            }
            success = true;
        }
        catch (Exception e) {
            Log.e(LOGTAG, "Error clearing cache", e);
        }

        return null;
    }
    
    @Override
    protected void onPostExecute(Result result){
        //your stuff
    	Log.i(TAG, "onPostExecute, success "+success);
        if (listener!=null) {
            listener.onClearCacheCompleted(success, e);
        }
    }
}