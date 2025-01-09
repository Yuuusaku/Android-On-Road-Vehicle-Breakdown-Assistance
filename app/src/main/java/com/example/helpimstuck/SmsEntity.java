package com.example.helpimstuck;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sms_messages")
public class SmsEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String sender;
    public String message;
    public long timestamp;

    public SmsEntity(String sender, String message, long timestamp) {
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
    }
}
