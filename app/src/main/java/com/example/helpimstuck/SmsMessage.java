package com.example.helpimstuck;

public class SmsMessage {
    private final String senderPhoneNumber;
    private final String message;

    public SmsMessage(String senderPhoneNumber, String message) {
        this.senderPhoneNumber = senderPhoneNumber;
        this.message = message;
    }

    public String getSenderPhoneNumber() {
        return senderPhoneNumber;
    }

    public String getMessage() {
        return message;
    }
}
