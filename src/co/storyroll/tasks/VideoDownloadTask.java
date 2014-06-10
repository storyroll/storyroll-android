package co.storyroll.tasks;

import android.content.Context;
import android.graphics.Interpolator.Result;
import android.os.AsyncTask;
import android.util.Log;
import co.storyroll.util.AppUtility;
import co.storyroll.util.DataUtility;
import com.bugsense.trace.BugSenseHandler;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class VideoDownloadTask extends AsyncTask<String, Void, Result> {
	
	public interface OnVideoTaskCompleted{
	    void onVideoTaskCompleted(String cachedFileName, boolean success, boolean wasCached, Exception e);
	}
	
	private static final String TAG = "VIDEO_DOWNLOAD";
	private static final int TIMEOUT_CONNECTION = 5000;//5sec
	private static final int TIMEOUT_SOCKET = 30000;//30sec
	private static final String LOGTAG = "VideoDownloadTask";
    
	private Context mContext;
	private String fileName;
	private File file;
	private OnVideoTaskCompleted listener;
	private boolean success = false;
	private boolean wasCached = true;
	private Exception e = null;
	
    public VideoDownloadTask(Context context, OnVideoTaskCompleted listener) {
    	Log.v(LOGTAG, "constructor");
        mContext = context;
        this.listener = listener;
    }

    @Override
    public void onPreExecute() {
    	
    }


    @Override
    protected Result doInBackground(String... params) {
        final String paramUrl = params[0];

        InputStream is = null;
        BufferedInputStream inStream = null;
        FileOutputStream outStream = null;

		try {
	        URL url = new URL(paramUrl);

//	        fileName = DataUtility.getBase64Filename(paramUrl); // filename too long for some handsets?
//	        fileName = Math.abs(paramUrl.hashCode())+"";
	        fileName = DataUtility.getCacheFileName(paramUrl)+".mp4";

	        Log.v(TAG, "filename: "+fileName);
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
		        is = ucon.getInputStream();
		        inStream = new BufferedInputStream(is, 1024 * 5);
		        outStream = new FileOutputStream(file);
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
		        
		        success = true;
		        wasCached = false;
		        
		        Log.i(TAG, "download completed in "
		                + ((System.currentTimeMillis() - startTime) / 1000)
		                + " sec");
//		        listener.onVideoTaskCompleted(fileName);
		        }
			// file found
			else {
				Log.i(TAG, "file found in cache: "+file.getAbsolutePath());
				success = true;
			}
		} catch (MalformedURLException e) {
			Log.e(LOGTAG, "Error opening file", e);
			BugSenseHandler.sendException(e);
		} catch (IOException e) {
			Log.e(LOGTAG, "Error writing file "+file.getAbsolutePath(), e);
			BugSenseHandler.sendException(e);

            // do your best trying to clean up but don't complain if it fails
            Log.i(LOGTAG, "Removing a corrupt download");
            try {
                if (outStream!=null) {
                    outStream.flush();
                    outStream.close();
                }
                if (inStream!=null) {inStream.close();}
                if (file.exists()) {
                    file.delete();
                }
            } catch (IOException e1) {}

		}

        return null;
    }
    
    @Override
    protected void onPostExecute(Result result){
        //your stuff
    	Log.i(TAG, "onPostExecute, success "+success+", wasCached: "+wasCached);
        listener.onVideoTaskCompleted(fileName, success, wasCached, e);
    }
}