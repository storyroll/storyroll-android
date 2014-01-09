package ru.jecklandin.stickman.vp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import com.storyroll.util.AppUtility;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.Messenger;
import android.text.TextUtils;
import android.util.Log;

public class ProcessingServiceNew extends IntentService {
	private static final String TAG = "ProcessingService";

  private int result = Activity.RESULT_CANCELED;
  public static final String URL = "urlpath";
  public static final String FILENAME = "filename";
  public static final String FILEPATH = "filepath";
  public static final String RESULT = "result";
  public static final String NOTIFICATION = "ru.jecklandin.stickman.vp";

	/** Name of the pipe FFmpeg should write progress to */
	private String mPipename = "11";
	
	private String mName;
	
	/**
	 * Flag to be set to true if processing completed successfully
	 */
	private boolean processingOk = false;
	 
	/**
	 * Determines if this service should call the next one
	 */
	private boolean callFurther = true;
	
	static {                           
	    System.loadLibrary("ffmpeg");
	    System.loadLibrary("pipes");
	}  
	
	private native void callFfmpeg(int argc, String[] argv);
	private native void write2Fifo(String fifoName, int value);  
	
  public ProcessingServiceNew() {
    super("ProcessingService");
  }

  // will be called asynchronously by Android
  @Override
  protected void onHandleIntent(final Intent intent) {
	
	  
	  
	  callFurther = true;
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				process(intent);
			}
		}).start();
		
//    String urlPath = intent.getStringExtra(URL);
//    String fileName = intent.getStringExtra(FILENAME);
//    File output = new File(Environment.getExternalStorageDirectory(),
//        fileName);
//    if (output.exists()) {
//      output.delete();
//    }
//
//    InputStream stream = null;
//    FileOutputStream fos = null;
//    try {
//
//      URL url = new URL(urlPath);
//      stream = url.openConnection().getInputStream();
//      InputStreamReader reader = new InputStreamReader(stream);
//      fos = new FileOutputStream(output.getPath());
//      int next = -1;
//      while ((next = reader.read()) != -1) {
//        fos.write(next);
//      }
//      // successfully finished
//      result = Activity.RESULT_OK;
//
//    } catch (Exception e) {
//      e.printStackTrace();
//    } finally {
//      if (stream != null) {
//        try {
//          stream.close();
//        } catch (IOException e) {
//          e.printStackTrace();
//        }
//      }
//      if (fos != null) {
//        try {
//          fos.close();
//        } catch (IOException e) {
//          e.printStackTrace();
//        }
//      }
//    }
    publishResults(null, result);
  }

  private void publishResults(String outputPath, int result) {
    Intent intent = new Intent(NOTIFICATION);
//    intent.putExtra(FILEPATH, outputPath);
    intent.putExtra(RESULT, result);
    sendBroadcast(intent);
  }
  
	public String sendProgressIntent(String s) {
		int value = 1;
		if ("end".equals(s)) {
			value = 0;
		}
		Intent i = new Intent("vp");
		i.putExtra("name", mName);
		i.putExtra("value", value);
		sendBroadcast(i);
		return "";
	}
	
	private void processMedia(String params) {
		Log.d(TAG, "Processing: ffmpeg "+params);
		String[] argv = params.split(" ");
//		for (String s : argv) {
//			Log.d("!!!!", s+"|");
//		}
		callFfmpeg(argv.length, argv);
	}
	
	private void processShell(String params) {
		Log.d(TAG, "Shell: "+params);
		try {
			Runtime.getRuntime().exec(params);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Concats the given files 
	 * @param files array of filenames (the last one is output)
	 * @param fromAssets true if files are in assets, false if in FS
	 */
	private void multicat(String[] files, boolean fromAssets) throws IOException {
		FileOutputStream fos = new FileOutputStream(files[files.length-1]);
		byte[] buff = new byte[1000];
		
		InputStream is = null;
		for (int i=0; i<files.length-1; ++i) {
			if (TextUtils.isEmpty(files[i])) {
				continue;
			}
			
			if (fromAssets) {
				is = getAssets().open(files[i]);
			} else {
				is = new FileInputStream(files[i]);
			}
			
			int c = 0;
			while ((c=is.read(buff, 0, 1000)) != -1) {
				fos.write(buff,0,c);
			}
		}
		fos.close();
	}
	
//	@Override
//	public void onStart(final Intent intent, int startId) {
//		super.onStart(intent, startId);
//		
//		if (intent == null || intent.getAction() == null) {
//			return;
//		}
//		
//		if (intent.getAction().equals(STOP_ACTION)) {
//			System.exit(0);
//			return;
//		} else if (intent.getAction().equals(START_ACTION)) {
//			callFurther = true;
//			new Thread(new Runnable() {
//				
//				@Override
//				public void run() {
//					process(intent);
//				}
//			}).start();
//		} 
//	}
	
	
	public void process(Intent intent) {
		mName = intent.getStringExtra("name");
		int i = intent.getIntExtra("num", 0);
		
		// retrieve a chain of commands from the intent
		String[] commands = intent.getStringArrayExtra("commands");
		if (commands == null || i > commands.length-1) {
			return;
		}
		
		String command = commands[i];
		
		try {
			if (command.startsWith("s")) {
				processShell(command.replaceFirst("s ", ""));
			} else if (command.startsWith("cat")) {
				String args[] = command.replaceFirst("cat ", "").split(" ");
				multicat(args, false);
			} else if (command.startsWith("acat")) { // files from assets
				String args[] = command.replaceFirst("acat ", "").split(" ");
				multicat(args, true);
			} else { // ffmpeg call
				processMedia(command);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "Processing failed");
			System.exit(0);
		}
		
		processingOk = true;
		
		if (! callFurther) {
			Log.d(TAG, "Converting was cancelled");
			System.exit(0);
			return;
		}
		
		++i;
		if (i == commands.length) { //it was the last command, calling Activity
			//write2Fifo(mPipename, 0); // 0 means successful end
			
			sendProgressIntent("end");
			// :-( can do better
			File f = new File(AppUtility.getAppWorkingDir(), mName);
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// Choose one of the services to launch a new process. 
		// A Service class can't hold more than 1 process so we have 3 different classes
		// FFMPEG lib is buggy in such a wat that multiple instances/calls can't be run in the same process
		
		Class<?> cl = null;
		switch (i%3) {
		case 0:
			cl = ProcessingService.class;
			break;
		case 1:
			cl = ProcessingService1.class;
			break;
		case 2:
			cl = ProcessingService2.class;
			break;
		}
		intent.setClass(this, cl);
		intent.putExtra("num", i);
		intent.putExtra("name", mName);
		startService(intent);
		System.exit(0);
	}
} 