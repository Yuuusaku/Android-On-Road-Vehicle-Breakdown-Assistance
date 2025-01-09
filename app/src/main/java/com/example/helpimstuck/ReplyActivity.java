package com.example.helpimstuck;

import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ReplyActivity extends AppCompatActivity {

    private TextView messageTextView;
    private Button yesButton;
    private Button noButton;
    private String senderPhoneNumber;
    private String originalMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reply);

        messageTextView = findViewById(R.id.messageTextView);
        yesButton = findViewById(R.id.yesButton);
        noButton = findViewById(R.id.noButton);

        // Get the intent extras
        Intent intent = getIntent();
        senderPhoneNumber = intent.getStringExtra("sender_phone");
        originalMessage = intent.getStringExtra("message");

        Log.d("ReplyActivity", "Received SMS: " + originalMessage + " from: " + senderPhoneNumber);

        if (senderPhoneNumber != null && originalMessage != null) {
            // Set the received message into the UI component
            messageTextView.setText(originalMessage);

            // Set up the buttons
            yesButton.setOnClickListener(v -> sendSms("Yes"));
            noButton.setOnClickListener(v -> sendSms("No"));
        }
    }

    private void sendSms(String response) {
        if (senderPhoneNumber != null) {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(senderPhoneNumber, null, response, null, null);
            Log.d("ReplyActivity", "Sent SMS: " + response);
        }
    }
}
