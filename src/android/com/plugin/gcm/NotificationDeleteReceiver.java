package com.plugin.gcm;

import com.plugin.gcm.PushHandlerActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationDeleteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        PushHandlerActivity.msgNum = 0;
    }
}
