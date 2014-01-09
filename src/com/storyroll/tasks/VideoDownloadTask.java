package com.storyroll.tasks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import android.content.Context;
import android.graphics.Interpolator.Result;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.storyroll.util.AppUtility;
import com.storyroll.util.DataUtility;

public class VideoDownloadTask extends AsyncTask<String, Void, Result> {
	
	public interface OnVideoTaskCompleted{
	    void onVideoTaskCompleted(String cachedFileName);
	}
	
	private static final String TAG = "VIDEO_DOWNLOAD";
	private static final int TIMEOUT_CONNECTION = 5000;//5sec
	private static final int TIMEOUT_SOCKET = 30000;//30sec
    
	private Context mContext;
	private String fileName;
	private File file;
	private OnVideoTaskCompleted listener;
	
    public VideoDownloadTask(Context context, OnVideoTaskCompleted listener) {
        mContext = context;
        this.listener = listener;
    }

    @Override
    public void onPreExecute() {
    	
    }


    @Override
    protected Result doInBackground(String... params) {
        final String paramUrl = params[0];

		try {
	        URL url = new URL(paramUrl);

	        fileName = DataUtility.getBase64Filename(paramUrl);

			file = new File(AppUtility.getVideoCacheDir(mContext.getApplicationContext()), fileName);
			
			if (!file.exists()) {
	        
		        long startTime = System.currentTimeMillis();
		        Log.i(TAG, "download begins: "+paramUrl);
	
		        //Open a connection to that URL.
		        URLConnection ucon = url.openConnection();
	
		        //this timeout affects how long it takes for the app to realize there's a connection problem
		        ucon.setReadTimeout(TIMEOUT_CONNECTION);
		        ucon.setConnectTimeout(TIMEOUT_SOCKET);
	
	
		        //Define InputStreams to read from the URLConnection.
		        // uses 3KB download buffer
		        InputStream is = ucon.getInputStream();
		        BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);
		        FileOutputStream outStream = new FileOutputStream(file);
		        byte[] buff = new byte[5 * 1024];
	
		        //Read bytes (and store them) until there is nothing more to read(-1)
		        int len;
		        while ((len = inStream.read(buff)) != -1)
		        {
		            outStream.write(buff,0,len);
		        }
	
		        //clean up
		        outStream.flush();
		        outStream.close();
		        inStream.close();
	
		        Log.i(TAG, "download completed in "
		                + ((System.currentTimeMillis() - startTime) / 1000)
		                + " sec");
//		        listener.onVideoTaskCompleted(fileName);
		        }
			// file found
			else {
				Log.i(TAG, "file found in cache: "+file.getAbsolutePath());
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        

        return null;
    }
    
    @Override
    protected void onPostExecute(Result result){
        //your stuff
    	 Log.i(TAG, "onPostExecute");
        listener.onVideoTaskCompleted(fileName);
    }
}