/**
    This file is part of Video Plugin for Animating Touch for Android.

    Video Plugin is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Video Plugin is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Video Plugin.  If not, see <http://www.gnu.org/licenses/>.
*/

package ru.jecklandin.stickman.vp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

/**
 * This class interacts with FFmpeg. It takes a chain of commands that
 * are passed within an Intent and process them in order.
 * @author jeck_landin
 */
public class ProcessingService extends Service {

	private static final String TAG = "ProcessingService";

	public static final String USE_NOTIFICATION = "ru.jecklandin.stickman.USE_NOTIFICATION";
	public static final String PROCESSING_FAILED = "ru.jecklandin.stickman.PROCESSING_FAILED";
	public static final String PROCESSING_FINISHED = "ru.jecklandin.stickman.PROCESSING_FINISHED";
	public static final String STOP_ACTION = "ru.jecklandin.stickman.STOP_ACTION";
	public static final String START_ACTION = "ru.jecklandin.stickman.START_ACTION";
	  public static final String NOTIFICATION = "com.vogella.android.service.receiver";

	
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
	
	public String sendProgressIntent(String s) {
		int value = 1;
		if ("end".equals(s)) {
			value = 0;
		}
//		Intent i = new Intent("vp");
		Intent i = new Intent(NOTIFICATION);
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
	
	@Override
	public void onStart(final Intent intent, int startId) {
		super.onStart(intent, startId);
		
		if (intent == null || intent.getAction() == null) {
			return;
		}
		
		if (intent.getAction().equals(STOP_ACTION)) {
			System.exit(0);
			return;
		} else if (intent.getAction().equals(START_ACTION)) {
			callFurther = true;
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					process(intent);
				}
			}).start();
		} 
	}
	
	
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
//			File f = new File(Environment.getExternalStorageDirectory() + "/storyroll", mName);
//			try {
//				f.createNewFile();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
		}
		
		//Choose one of the services to launch a new process. 
		//A Service class can't hold more than 1 process so we have 3 different classes
		
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
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
