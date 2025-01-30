package com.example.helpimstuck;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class NotifViewModel extends ViewModel {
    private final List<SmsEntity> smsMessages = new ArrayList<>();

    public List<SmsEntity> getSmsMessages() {
        return smsMessages;
    }

    public void setSmsMessages(List<SmsEntity> messages) {
        smsMessages.clear();
        smsMessages.addAll(messages);
    }

    // Function to clear SMS messages
    public void clearSmsMessages() {
        smsMessages.clear();
    }
}