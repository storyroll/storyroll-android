package com.storyroll.ffmpeg;

//import info.guardianproject.mrapp.AppConstants;

import java.io.File;
import java.io.IOException;

import org.ffmpeg.android.FfmpegController;
import org.ffmpeg.android.MediaDesc;
import org.ffmpeg.android.ShellUtils.ShellCallback;

import com.storyroll.util.AppUtility;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class MediaCropper implements Runnable
{
	private final static String LOGTAG = "MediaCropper";
	
//	private Handler mHandler;
	private MediaDesc mMediaIn;
	private MediaDesc mMediaOut;
	private Context mContext;
	private File mFileExternDir;
	private ShellCallback mShellCallback;
	private MediaManager mMediaManager;
	
    private int current, total;

	public MediaCropper (Context context, Handler handler, MediaDesc mediaIn, MediaDesc mediaOut, File fileExternDir, ShellCallback shellCallback)
	{
//		mHandler = handler;
		mMediaIn = mediaIn;
		mMediaOut = mediaOut;
		mContext = context;
		mFileExternDir = fileExternDir;
		mShellCallback = shellCallback;
	}
	
	public void run ()
	{
    	try
    	{
    		 Log.v(LOGTAG, "Rendering media in the background");
	        
    		if (mMediaIn.mimeType.startsWith("video"))
    			prerenderVideo(mMediaIn, mMediaOut);
    		else {
    			Log.e(LOGTAG, "Can't crop media of mimeType: "+mMediaIn.mimeType);
    		}
	    		
    		File fileMediaOut = new File(mMediaOut.path);
    		
	         if (!fileMediaOut.exists() || (fileMediaOut.length() == 0))
	         {
	    		 Log.e(LOGTAG,"Error occured with media pre-render");
			 }
	         else
	         {
	    		 Log.v(LOGTAG,"Success - media rendered!");
	         }
    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    	}
	}
	
    private void prerenderVideo (MediaDesc mediaIn, MediaDesc mMediaOut) throws Exception
    {
    	

    	FfmpegController ffmpegc = new FfmpegController (mContext, new File(AppUtility.getAppWorkingDir()));

//    	MediaDesc mediaOut = ffmpegc.convertToMP4Stream(mediaIn, fileOutPath.getAbsolutePath(), preconvertMP4, mShellCallback);
    
//    	MediaDesc mediaOut = ffmpegc.convertToMP4Stream(mediaIn, "0", "3000", fileOutPath.getAbsolutePath(), mShellCallback);
    	
    	ffmpegc.processVideo(mediaIn, mMediaOut, false, mShellCallback);		
    
   }

    private File createOutputFile (String fileext) throws IOException
    {
		File saveFile = File.createTempFile("output", '.' + fileext, mFileExternDir);	
		return saveFile;
    }
}