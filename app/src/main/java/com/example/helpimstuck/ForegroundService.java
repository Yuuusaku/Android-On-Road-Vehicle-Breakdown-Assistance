package com.example.helpimstuck;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class ForegroundService extends android.app.Service {

    private static final String CHANNEL_ID = "sos_notifications";
    private NotificationManager notificationManager;
    private BroadcastReceiver smsReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();  // Create notification channel for notifications

        // Register BroadcastReceiver to receive SMS messages
        smsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null && intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
                    Object[] pdus = (Object[]) intent.getExtras().get("pdus");
                    if (pdus == null) return;

                    SmsMessage[] messages = new SmsMessage[pdus.length];
                    StringBuilder smsMessage = new StringBuilder();
                    String senderPhoneNumber = null;

                    for (int i = 0; i < pdus.length; i++) {
                        messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        smsMessage.append(messages[i].getMessageBody());

                        if (i == 0) {
                            senderPhoneNumber = messages[i].getOriginatingAddress();
                        }
                    }

                    Log.d("SMS_RECEIVER", "Received SMS from: " + senderPhoneNumber + " Message: " + smsMessage.toString());

                    // Check for SOS message
                    if (smsMessage.toString().contains("SOS")) {
                        showNotification(senderPhoneNumber, smsMessage.toString());
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(smsReceiver, filter);

        // Start service as foreground service
        startForeground(1, createNotification("Listening for SMS..."));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (smsReceiver != null) {
            unregisterReceiver(smsReceiver);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "SOS Notifications";
            String description = "Notifications for incoming SOS messages";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SOS Service")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .build();
    }

    private void showNotification(String senderPhoneNumber, String message) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("New SOS Message")
                .setContentText("From: " + senderPhoneNumber + " - " + message)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(1, notification);  // Show notification
    }
}
