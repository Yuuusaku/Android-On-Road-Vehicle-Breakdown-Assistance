package com.example.helpimstuck;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SmsDao {

    @Query("SELECT * FROM sms_messages ORDER BY timestamp DESC")
    List<SmsEntity> getAllMessages();

    @Insert
    void insert(SmsEntity smsEntity);

    // Method to delete all messages from the table
    @Query("DELETE FROM sms_messages")
    void deleteAllMessages();  // This deletes all rows from the sms_messages table
}
