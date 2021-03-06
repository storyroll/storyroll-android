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

package co.storyroll.gcm;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import co.storyroll.R;
import co.storyroll.activity.ChannelActivity;
import co.storyroll.util.DataUtility;
import co.storyroll.util.PrefUtility;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.util.Date;

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class GcmIntentService extends IntentService {

	private static final String DEFAULT_UNKNOWN_CHAN_NAME = "<UNKNOWN>";
	private static final String DEFAULT_LAST_USERNAME = "Someone";

	private static final String KEY_GCM_CHANNEL_ID_STR = "channel";
	public static final String EXTRA_LAST_USERNAME = "lastUsername";
    public static final String EXTRA_INVITER_NAME = "inviter_name";

    private static final String KEY_GCM_COLLAPSE_KEY = "collapse_key";
	private static final String KEY_GCM_UNSEEN_COUNT = "count";
    public static final String EXTRA_NOTIFICATION = "NOTIFICATION";
    public static final String EXTRA_CHANNEL_NAME = "channelName";
    public static final String EXTRA_CHANNEL_ID_STR = "channelId";
    public static final String EXTRA_LAST_UPDATED_MOVIE = "lastUpdatedMovie";
    public static final String EXTRA_STORIES = "stories";
    public static final String EXTRA_CHANNEL_TITLE = "channelTitle";
    public static final String GCM_EVENT_CLIP_POSTED = "gcm-video-posted";
    private static String NEW_MOVIE_IN_CHANNEL = "NEW_MOVIE_IN_CHANNEL";
	private static String NEW_REPLY_IN_CHANNEL = "NEW_REPLY_IN_CHANNEL";
	private static String STORY_PUBLISHED = "STORY_PUBLISHED";
	private static String REPLY_PUBLISHED = "REPLY_PUBLISHED";
    private static String CHANNEL_INVITE = "CHANNEL_INVITE";



    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }
    public static final String LOGTAG = "GCM_SERVICE";

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle gcmExtras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!gcmExtras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM will be
             * extended in the future with new message types, just ignore any message types you're
             * not interested in, or that you don't recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
            	Log.e(LOGTAG, "Send error: " + gcmExtras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
            	// TODO improve deleted messages event (full sync?)
            	Log.e(LOGTAG, "Deleted messages on server: " + gcmExtras.toString());
            // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType))
            {
                // This loop represents the service doing some work.
                for (int i = 0; i < 1; i++) {
                    Log.v(LOGTAG, "Working... " + (i + 1)
                            + "/5 @ " + SystemClock.elapsedRealtime());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
                Log.v(LOGTAG, "Completed work @ " + SystemClock.elapsedRealtime());
                // Post notification of received message.
                Log.d(LOGTAG, "MESSAGE_TYPE_MESSAGE Received");
                processGcmMessage(gcmExtras);
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void processGcmMessage(Bundle gcmExtras)
    {
    	Log.i(LOGTAG, "processGcmMessage: " + gcmExtras.toString());
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        // is this a notification for current user?
        String uuid = gcmExtras.getString("uuid");
        String currentUuid = PrefUtility.getUuid();
        if (uuid==null || !uuid.equals(currentUuid)) 
        {
            String errstr =  "GCM message received for "+uuid+", not for current user: "+currentUuid;
        	Log.w(LOGTAG, errstr);
            // TODO: fire analytics or bugSense event
        	return;
        }

//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
//                new Intent(this, GcmTestActivity.class), 0);
        Intent notificationIntent = new Intent(this, ChannelActivity.class);
        notificationIntent.putExtra(EXTRA_NOTIFICATION, true);

        String collapseKey = gcmExtras.getString(KEY_GCM_COLLAPSE_KEY);
        Log.v(LOGTAG, "collapse_key="+collapseKey);
        
        String msg = "";
        String contentTitle = "StoryRoll";
        String countNewStoriesStr = gcmExtras.getString(KEY_GCM_UNSEEN_COUNT, null);
        String channelName = gcmExtras.getString(EXTRA_CHANNEL_NAME, DEFAULT_UNKNOWN_CHAN_NAME);
        String lastUserame = gcmExtras.getString(EXTRA_LAST_USERNAME, DEFAULT_LAST_USERNAME);
        String inviterName = gcmExtras.getString(EXTRA_INVITER_NAME, DEFAULT_LAST_USERNAME);
        String chanIdStr = gcmExtras.getString(KEY_GCM_CHANNEL_ID_STR);
        String lastUpdatedMovie = gcmExtras.getString(EXTRA_LAST_UPDATED_MOVIE);

        if (STORY_PUBLISHED.equalsIgnoreCase(collapseKey)) {
            Log.v(LOGTAG, "Got message: Published in "+channelName.toUpperCase()+"!");
            sendVideoPostedMessage(chanIdStr, lastUpdatedMovie);
            return;
        }

        boolean addStoryNum = true;
        if (TextUtils.isEmpty(countNewStoriesStr)) 
        {
        	Log.w(LOGTAG, "No message count in GCM message payload");
//        	BugSenseHandler.sendException(new Exception("No message count GCM message payload"));
        	msg = gcmExtras.getString("message");
        }
        else if (REPLY_PUBLISHED.equalsIgnoreCase(collapseKey)) {
        	contentTitle = getResources().getString(R.string.notif_you_got_response);
        	msg = lastUserame+" replied in "+channelName.toUpperCase()+"!";
        }
    	else if (NEW_MOVIE_IN_CHANNEL.equalsIgnoreCase(collapseKey)) {
    		contentTitle = getResources().getString(R.string.notif_new_video);
        	msg = lastUserame+" posted new video in "+channelName.toUpperCase()+"!";
    	}
    	else if (NEW_REPLY_IN_CHANNEL.equalsIgnoreCase(collapseKey)) {
    		contentTitle = getResources().getString(R.string.notif_response_channel);
    		msg = lastUserame+" posted a response in "+channelName.toUpperCase()+"!";
    	}
        else if (CHANNEL_INVITE.equalsIgnoreCase(collapseKey)) {
            contentTitle = getResources().getString(R.string.notif_channel_invite);
            msg = inviterName+" invited you to "+channelName.toUpperCase();
            addStoryNum = false;
        }
        int countNewStories = 0;
        if (!TextUtils.isEmpty(countNewStoriesStr)) {
            countNewStories = Integer.valueOf(countNewStoriesStr);
        }
		if (countNewStories>1 && addStoryNum) {
	    	msg=msg+" You have "+countNewStories+" unchecked video(s).";
	    }

        String newStoriesStr = gcmExtras.getString(EXTRA_STORIES);
        notificationIntent.putExtra(EXTRA_STORIES, DataUtility.stringToIntArray(newStoriesStr));
        notificationIntent.putExtra(EXTRA_LAST_UPDATED_MOVIE, lastUpdatedMovie);
        Log.v(LOGTAG, "gcm chanId: "+chanIdStr);
        notificationIntent.putExtra(EXTRA_CHANNEL_ID_STR, chanIdStr);
        notificationIntent.putExtra(EXTRA_CHANNEL_TITLE, channelName);

        // see http://stackoverflow.com/questions/3127957/why-the-pendingintent-doesnt-send-back-my-custom-extras-setup-for-the-intent
        notificationIntent.setAction(new Date().toString());

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.ic_notif_newstory)
        .setContentTitle(contentTitle)
        .setStyle(new NotificationCompat.BigTextStyle()
        .bigText(msg))
        .setContentText(msg)
        .setAutoCancel(true);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private void sendVideoPostedMessage(String chanIdStr, String lastUpdatedMovie) {
        Log.d("sender", "Broadcasting message");
        Intent intent = new Intent(GCM_EVENT_CLIP_POSTED);
        // You can also include some extra data.
        intent.putExtra("message", "This is my message!");
        intent.putExtra(EXTRA_CHANNEL_ID_STR, chanIdStr);
        intent.putExtra(EXTRA_LAST_UPDATED_MOVIE, lastUpdatedMovie);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
