/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.storyroll.gcm;

import com.bugsense.trace.BugSenseHandler;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.storyroll.R;
import com.storyroll.activity.GcmTestActivity;
import com.storyroll.util.AppUtility;
import com.storyroll.util.DataUtility;
import com.storyroll.util.PrefUtility;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class GcmIntentService extends IntentService {
	private static String NEW_MOVIE_IN_CHANNEL = "NEW_MOVIE_IN_CHANNEL";
	private static String NEW_REPLY_IN_CHANNEL = "NEW_REPLY_IN_CHANNEL";
	private static String STORY_PUBLISHED = "STORY_PUBLISHED";
	private static String REPLY_PUBLISHED = "STORY_PUBLISHED";
	
	
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }
    public static final String LOGTAG = "GCM Intent Service";

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM will be
             * extended in the future with new message types, just ignore any message types you're
             * not interested in, or that you don't recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
            	Log.e(LOGTAG, "Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
            	// TODO improve deleted messages event (full sync?)
            	Log.e(LOGTAG, "Deleted messages on server: " + extras.toString());
            // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // This loop represents the service doing some work.
                for (int i = 0; i < 1; i++) {
                    Log.v(LOGTAG, "Working... " + (i + 1)
                            + "/5 @ " + SystemClock.elapsedRealtime());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                    }
                }
                Log.v(LOGTAG, "Completed work @ " + SystemClock.elapsedRealtime());
                // Post notification of received message.
                Log.d(LOGTAG, "MESSAGE_TYPE_MESSAGE Received");
                processGcmMessage(extras);
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void processGcmMessage(Bundle extras) 
    {
    	Log.i(LOGTAG, "processGcmMessage: " + extras.toString());
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        // is this a notification for current user?
        String uuid = extras.getString("uuid");
        String currentUuid = PrefUtility.getUuid();
        if (uuid==null || !uuid.equals(currentUuid)) 
        {
        	Log.w(LOGTAG, "GCM message received for "+uuid+", not for current user: "+currentUuid);
        	// TODO: fire analytics or bugSense event
        	return;
        }
        

//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
//                new Intent(this, GcmTestActivity.class), 0);
        Intent notificationIntent = new Intent(this, AppUtility.ACTIVITY_HOME);
        notificationIntent.putExtra("NOTIFICATION", true);
        
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
        		notificationIntent, 0);
        
        String collapseKey = extras.getString("collapse_key");
        Log.v(LOGTAG, "collapse_key="+collapseKey);
        
        String msg = "";
        String contentTitle = "StoryRoll";
        String countNewStoriesStr = extras.getString("count", null);
        String channelName = extras.getString("channelName", "<UNKNOWN>");
        
        if (TextUtils.isEmpty(countNewStoriesStr)) 
        {
        	Log.w(LOGTAG, "No message count in GCM message payload");
//        	BugSenseHandler.sendException(new Exception("No message count GCM message payload"));
        	msg = extras.getString("message");
        }
        else if (STORY_PUBLISHED.equalsIgnoreCase(collapseKey)) {
        	contentTitle = "Your video is on!";
        	msg = "Published in "+channelName.toUpperCase()+"!";
        	
        }
        else if (REPLY_PUBLISHED.equalsIgnoreCase(collapseKey)) {
        	contentTitle = "You got response!";
        	msg = "Someone replied to you in "+channelName.toUpperCase()+"!";
        }
    	else if (NEW_MOVIE_IN_CHANNEL.equalsIgnoreCase(collapseKey)) {
    		contentTitle = "New video";
        	msg = "New video in "+channelName.toUpperCase()+"!";
    	}
    	else if (NEW_REPLY_IN_CHANNEL.equalsIgnoreCase(collapseKey)) {
    		contentTitle = "New response";
    		msg = "Someone posted a response in "+channelName.toUpperCase()+"!";
    	}
        int countNewStories = Integer.valueOf(countNewStoriesStr);
		if (countNewStories>1) {
	    	msg=msg+" You have "+countNewStories+" unchecked video(s).";
	    }
        
        String newStoriesStr = extras.getString("stories");
        notificationIntent.putExtra("stories", DataUtility.stringToIntArray(newStoriesStr));
        notificationIntent.putExtra("lastUpdatedMovie", extras.getString("lastUpdatedMovie"));
        notificationIntent.putExtra("channelId", extras.getString("channel"));
        
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.ic_notif_newstory)
        .setContentTitle(contentTitle)
        .setStyle(new NotificationCompat.BigTextStyle()
        .bigText(msg))
        .setContentText(msg)
        .setAutoCancel(true);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}