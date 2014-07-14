/*******************************************************************************
 * Copyright 2012 AndroidQuery (tinyeeliu@gmail.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Additional Note:
 * 1. You cannot use AndroidQuery's Facebook app account in your own apps.
 * 2. You cannot republish the app as is with advertisements.
 ******************************************************************************/
package co.storyroll.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import co.storyroll.R;
import co.storyroll.base.Constants;
import co.storyroll.tasks.VideoDownloadTask;
import com.androidquery.util.AQUtility;

import java.io.File;
import java.util.List;
import java.util.Locale;

//import co.storyroll.ImageActivity;

public class IntentUtility {
    private static final String LOGTAG = "IntentUtility";

	/*
    public static boolean openMarket(Activity act) {
    	try{
	    	Intent intent = new Intent(Intent.ACTION_VIEW);
	    	intent.setData(Uri.parse(AppStoreUtility.getMarketMobileUrl()));
	    	act.startActivity(intent);
	    	return true;
    	}catch(Exception e){
    		AQUtility.report(e);
    		return false;
    	}
    }
    */
	
	public static boolean openMarket(Activity act){
		
		return openBrowser(act, getMarketUrl(act));
		
	}
	
	
    public static String getMarketUrl(Activity act){
    	return "market://details?id=" + getAppId(act);    	
    }
    
    
    public static String getWebMarketUrl(Activity act){
    	
    	Locale locale = Locale.getDefault();    
    	String lang = locale.getLanguage();    
    	
    	if("zh".equals(lang)){
    		lang = "zh_tw";
    	}
    	
    	return "https://market.android.com/details?id=" + getAppId(act) + "&hl=" + lang;    	 	
    
    }
	
    
	private static String getAppId(Activity act){
		return act.getApplicationInfo().packageName;
	}
    
    public static boolean openBrowser(Activity act, String url) {
    
    	
    	try{
   
	    	if(url == null) return false;
	    	
	    	Uri uri = Uri.parse(url);
	    	Intent intent = new Intent(Intent.ACTION_VIEW, uri);	    	
	    	act.startActivity(intent);
    	
	    	return true;
    	}catch(Exception e){
    		AQUtility.report(e);
    		return false;
    	}
    }
    
    /*
    public static void sendEmail(Activity act){
    	
    	Intent i = new Intent(Intent.ACTION_SEND);
    	
    	i.setType("text/plain");
    	i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"support@vikispot.com"});
    	i.putExtra(Intent.EXTRA_SUBJECT, "Feedback from User");
    	
    	try {
    	    act.startActivity(Intent.createChooser(i, "Send feedback email with..."));
    	} catch (android.content.ActivityNotFoundException ex) {
    	    //Toast.makeText(MyActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
    	}
    }
    */
    
//    public static void launchActivity(Activity act, FeedItem item){
//    	
//    	if(item == null) return;
//    	
//    	String url = item.getLink();
//    	if(url == null) return;
//    	
//    	AQUtility.debug("clicked", url);
//    	
//    	if(ParseUtility.isYT(url)){
//    		IntentUtility.openBrowser(act, url);
//    	}else if("photo".equals(item.getType())){
//    		String tb = item.getContentTb();    		
//    		if(tb != null){
//    			tb = tb.replaceAll("_s.", "_n.");
//    			tb = tb.replaceAll("type=album", "type=normal");
//    			//IntentUtility.openBrowser(this, tb);
//    			Intent intent = new Intent(act, ImageActivity.class);
//    			intent.putExtra("url", tb);
//    			intent.putExtra("item", item);
//    	    	act.startActivity(intent);
//    		}
//    	}else if("video".equals(item.getType())){
//    		String source = item.getSource();
//    		if(source == null) source = url;
//    		IntentUtility.openBrowser(act, source);
//    	}else if(!url.contains("facebook.com")){
//    		IntentUtility.openBrowser(act, url);
//    	}
//    	
//    }
    
    
    public static void sendEmail(Activity act, String title, String text){
    	
    	Intent i = new Intent(Intent.ACTION_SEND);
    	
    	i.setType("text/plain");
    	i.putExtra(Intent.EXTRA_EMAIL  , new String[]{Constants.EMAIL_FEEDBACK});
    	i.putExtra(Intent.EXTRA_SUBJECT, title);
    	i.putExtra(Intent.EXTRA_TEXT, text);
    	
    	String select = act.getString(R.string.send_email);
    	
    	try {
    	    act.startActivity(Intent.createChooser(i, select));
    	}catch(Exception ex) {
    	    //Toast.makeText(MyActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
    	}
    }
    
    public static void sendShare(Activity act, String title, String text){
    	
    	
    	Intent i = new Intent(Intent.ACTION_SEND);
    	
    	i.setType("text/plain");
    	i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	
    	i.putExtra(Intent.EXTRA_SUBJECT, title);
    	i.putExtra(Intent.EXTRA_TEXT, text);
    	
    	String select = act.getString(R.string.share);
    	
    	try {
    		AQUtility.debug("share act start");
    	    act.startActivity(Intent.createChooser(i, select));
    	}catch(Exception ex) {
    		AQUtility.report(ex);
    	    //Toast.makeText(MyActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
    	}
    }


    public static void sendShareInstagram(Activity context, String link)
    {
        VideoDownloadTask task = new VideoDownloadTask(context.getApplicationContext(), new VideoDownloadCallback(context));
        task.execute(link);
    }

    static class VideoDownloadCallback implements VideoDownloadTask.OnVideoTaskCompleted {

        Context mContext;

        public VideoDownloadCallback(Context context) {
            mContext = context;
        }

        @Override
        public void onVideoTaskCompleted(String cachedFileName, boolean success, boolean wasCached, Exception e) {
            if (success) {
                Log.v(LOGTAG, "Sharing file: "+cachedFileName);
                shareVideo(mContext, cachedFileName);
            }
            else {
                Toast.makeText(mContext, "Error downloading video", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void shareVideo(final Context context, final String fileName){
        new Handler().post(new Runnable() {

            @Override
            public void run() {
                Uri videoURI = Uri.fromFile( new File(AppUtility.getVideoCacheDir(context.getApplicationContext()), fileName) );
//                Uri videoURI = Uri.parse("content://" + CachedFileProvider.AUTHORITY + "/"
//                        + fileName);
//                Uri photoUri = FileProvider.getUriForFile(context, "com.myapp.fileprovider", new File(fileName));

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Title");
                intent.setType("video/mp4");
                intent.putExtra(Intent.EXTRA_STREAM, videoURI);
                intent.setPackage("com.instagram.android");


                // Grant permissions to all apps that can handle this intent
                // thanks to this answer http://stackoverflow.com/a/18332000
                List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    context.grantUriPermission(packageName, videoURI,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                try {
                    context.startActivity(Intent.createChooser(intent, "Upload video via:"));
                } catch (android.content.ActivityNotFoundException ex) {
                    Log.e(LOGTAG, "ActivityNotFoundException", ex);
                }
            }
        });
    };

    public static String getVideoContentUriFromFilePath(Context ctx, String filePath) {

        ContentResolver contentResolver = ctx.getContentResolver();
        String videoUriStr = null;
        long videoId = -1;
        Log.d("first log","Loading file " + filePath);

        // This returns us content://media/external/videos/media (or something like that)
        // I pass in "external" because that's the MediaStore's name for the external
        // storage on my device (the other possibility is "internal")
        Uri videosUri = MediaStore.Video.Media.getContentUri("external");

        Log.d("second log","videosUri = " + videosUri.toString());

        String[] projection = {MediaStore.Video.VideoColumns._ID};

        // TODO This will break if we have no matching item in the MediaStore.
        Cursor cursor = contentResolver.query(videosUri, projection, MediaStore.Video.VideoColumns.DATA + " LIKE ?", new String[] { filePath }, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(projection[0]);
        videoId = cursor.getLong(columnIndex);

        Log.d("third log","Video ID is " + videoId);
        cursor.close();
        if (videoId != -1 ) videoUriStr = videosUri.toString() + "/" + videoId;
        return videoUriStr;
    }
}
