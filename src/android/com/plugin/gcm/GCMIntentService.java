package com.plugin.gcm;

import java.util.List;

import com.google.android.gcm.GCMBaseIntentService;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.IOException;
import com.plugin.gcm.PushHandlerActivity;

@SuppressLint("NewApi")
public class GCMIntentService extends GCMBaseIntentService {

	public static final int NOTIFICATION_ID = 237;
	private static final String TAG = "GCMIntentService";

	public GCMIntentService() {
		super("GCMIntentService");
	}

	@Override
	public void onRegistered(Context context, String regId) {

		Log.v(TAG, "onRegistered: "+ regId);

		JSONObject json;

		try
		{
			json = new JSONObject().put("event", "registered");
			json.put("regid", regId);

			Log.v(TAG, "onRegistered: " + json.toString());

			// Send this JSON data to the JavaScript application above EVENT should be set to the msg type
			// In this case this is the registration ID
			PushPlugin.sendJavascript( json );

		}
		catch( JSONException e)
		{
			// No message to the user is sent, JSON failed
			Log.e(TAG, "onRegistered: JSON exception");
		}
	}

	@Override
	public void onUnregistered(Context context, String regId) {
		Log.d(TAG, "onUnregistered - regId: " + regId);
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		Log.d(TAG, "onMessage - context: " + context);

		// Extract the payload from the message
		Bundle extras = intent.getExtras();
		if (extras != null)
		{
			boolean	foreground = this.isInForeground();

			extras.putBoolean("foreground", foreground);

			/* if (foreground) { */
			/* 	PushPlugin.sendExtras(extras); */
      /* } */

      /* if the app is on we add notification */
		  createNotification(context, extras);
		}
	}

	public void createNotification(Context context, Bundle extras)
   {
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		String appName = getAppName(this);
		Intent notificationIntent = new Intent(this, PushHandlerActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("pushBundle", extras);

		String title = extras.getString("title");
    String message = extras.getString("message");
    String user = extras.getString("user");
		String msgcnt = extras.getString("msgcnt");
		String userImage = extras.getString("userImage");

    Bitmap bitmap = getBitmapFromURL(userImage);
    if (msgcnt != null) {
      /* PushHandlerActivity.msgNum keeps track of the number of times
       * user was notified before opening the app
       */
      PushHandlerActivity.msgNum += Integer.parseInt(msgcnt);
		}

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		PendingIntent deleteIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, NotificationDeleteReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);

    long[] pattern = {
        0, 500, 100, 500, 100
    };

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);

    mBuilder.setSmallIcon(context.getApplicationInfo().icon)
    .setLargeIcon(bitmap)
    .setWhen(System.currentTimeMillis())
    .setTicker(appName)
    .setAutoCancel(true)
    .setLights(0xFFE7E1B2,300,3000)
    .setVibrate(pattern)
    .setDeleteIntent(deleteIntent)
    .setContentTitle(title)
    .setContentText(message)
    .setNumber(PushHandlerActivity.msgNum)
    .setContentIntent(contentIntent)
    .setStyle(new NotificationCompat.BigTextStyle().bigText(message).setSummaryText(getAppName(context)+" @"+user));

    /* notify the user ie create notification card
     * @param NOTIFICATION_ID: unique identifier for message in our case always same
     * @param mBuilder.build(): build the notification
     */
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
		tryPlayRingtone();
	}

	private void tryPlayRingtone()
	{
		try {
			Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
			r.play();
		}
		catch (Exception e) {
			Log.e(TAG, "failed to play notification ringtone");
		}
	}

	public static void cancelNotification(Context context)
	{
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel((String)getAppName(context), NOTIFICATION_ID);
	}

  public Bitmap getBitmapFromURL(String strURL) {
    try {
      URL url = new URL(strURL);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setDoInput(true);
      connection.connect();
      InputStream input = connection.getInputStream();
      Bitmap myBitmap = BitmapFactory.decodeStream(input);
      return myBitmap;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

	private static String getAppName(Context context)
	{
		CharSequence appName =
				context
					.getPackageManager()
					.getApplicationLabel(context.getApplicationInfo());

		return (String)appName;
	}

	public boolean isInForeground()
	{
		ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> services = activityManager
				.getRunningTasks(Integer.MAX_VALUE);

		if (services.get(0).topActivity.getPackageName().toString().equalsIgnoreCase(getApplicationContext().getPackageName().toString()))
			return true;

		return false;
	}

	@Override
	public void onError(Context context, String errorId) {
		Log.e(TAG, "onError - errorId: " + errorId);
	}

}
