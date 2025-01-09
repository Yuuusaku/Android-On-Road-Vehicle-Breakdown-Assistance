package com.example.helpimstuck;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import androidx.core.app.NotificationCompat;

public class SmsReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "sms_channel_id"; // Unique channel ID
    private static final String CHANNEL_NAME = "SMS Notifications"; // Channel name for UI
    private static final int NOTIFICATION_ID = 1; // Notification ID for updates

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            if (bundle == null) return;

            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus == null) return;

            StringBuilder smsMessage = new StringBuilder();
            String senderPhoneNumber = null;

            for (Object pdu : pdus) {
                SmsMessage message = SmsMessage.createFromPdu((byte[]) pdu);
                smsMessage.append(message.getMessageBody());

                if (senderPhoneNumber == null) {
                    senderPhoneNumber = message.getOriginatingAddress();
                }
            }

            // Save SMS to the database
            AppDatabase db = AppDatabase.getInstance(context);
            String finalSenderPhoneNumber = senderPhoneNumber;
            new Thread(() -> db.smsDao().insert(new SmsEntity(finalSenderPhoneNumber, smsMessage.toString(), System.currentTimeMillis()))).start();

            // Show notification
            showNotification(context, senderPhoneNumber, smsMessage.toString());
        }
    }

    private void showNotification(Context context, String sender, String message) {
        // Create notification channel (required for Android 8.0 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Intent to open the app when the notification is clicked
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Replace with your app's icon
                .setContentTitle("New SMS from: " + sender)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message)) // Expandable text
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true) // Remove notification after tapping
                .setContentIntent(pendingIntent); // Open the app

        // Display the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }
}
