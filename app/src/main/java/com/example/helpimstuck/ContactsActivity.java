package com.example.helpimstuck;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ContactsActivity extends AppCompatActivity {

    private static final int CONTACTS_PERMISSION_REQUEST_CODE = 1;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2;

    private ListView contactListView;
    private Button sendSOSButton, listplace;
    private ArrayList<String> contactsList;
    private ArrayAdapter<String> adapter;
    private Map<Integer, Boolean> checkBoxMap;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        contactListView = findViewById(R.id.contactListView);
        sendSOSButton = findViewById(R.id.sendSOSButton);


        // Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        checkAndRequestContactsPermission();

        sendSOSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndRequestLocationPermission(); // Request location permission when SOS is pressed
            }
        });

        contactListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedContact = contactsList.get(position);
                checkBoxMap.put(position, !checkBoxMap.getOrDefault(position, false));
                // Update the adapter to reflect the changes
                adapter.notifyDataSetChanged();
            }
        });
    }

    // Check and request contacts permission at runtime
    private void checkAndRequestContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, CONTACTS_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, proceed to get contact list
            getContactList();
        }
    }

    // Check and request location permission at runtime
    private void checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, proceed to get location and send SOS message
            getLocationAndSendSOSMessage();
        }
    }

    // Handle the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CONTACTS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Contacts permission granted, proceed to get contact list
                getContactList();
            } else {
                Toast.makeText(this, "Contacts permission is required for this app to function properly", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted, proceed to get location and send SOS message
                getLocationAndSendSOSMessage();
            } else {
                Toast.makeText(this, "Location permission is required to send your current location", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getContactList() {
        contactsList = new ArrayList<>();
        Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        );

        if (cursor == null) {
            // Handle the case where the cursor is null
            return;
        }

        int nameColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        int phoneNumberColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

        while (cursor.moveToNext()) {
            String name = cursor.getString(nameColumnIndex);
            String phoneNumber = cursor.getString(phoneNumberColumnIndex);
            contactsList.add(name + " - " + phoneNumber);
        }

        cursor.close();

        // Set up the adapter with the updated contactsList and checkBoxMap
        checkBoxMap = new HashMap<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, contactsList);
        contactListView.setAdapter(adapter);
        contactListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    }

    private void getLocationAndSendSOSMessage() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        // Format the coordinates as a Google Maps URL
                        String googleMapsUrl = "https://www.google.com/maps?q=" + latitude + "," + longitude;

                        // Create the SOS message with the Google Maps link
                        String sosMessage = "Assistance Request: I need help, my vehicle broke down/something emergency happened. Please enter my coordinates in Google Maps.\n" +
                                "Here is my current location: " + googleMapsUrl;

                        // Now send the SOS message to selected contacts
                        sendSOSMessageToSelectedContacts(sosMessage);
                    } else {
                        Toast.makeText(ContactsActivity.this, "Unable to retrieve current location", Toast.LENGTH_SHORT).show();
                        Log.d("LocationCheck", "Location is null");
                    }
                }
            });
        } else {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendSOSMessageToSelectedContacts(String sosMessage) {
        StringBuilder selectedContactsBuilder = new StringBuilder();

        for (Map.Entry<Integer, Boolean> entry : checkBoxMap.entrySet()) {
            int position = entry.getKey();
            boolean isChecked = entry.getValue();

            if (isChecked) {
                String selectedContact = contactsList.get(position);
                selectedContactsBuilder.append(selectedContact).append("\n");

                // Get the phone number and send the SOS message
                String phoneNumber = getPhoneNumber(selectedContact);
                sendSMS(phoneNumber, sosMessage);
            }
        }

        if (selectedContactsBuilder.length() > 0) {
            String selectedContactsMessage = "SOS message sent to:\n" + selectedContactsBuilder.toString();
            Toast.makeText(this, selectedContactsMessage, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No contacts selected", Toast.LENGTH_SHORT).show();
        }
    }

    private String getPhoneNumber(String contact) {
        // Extract and return the phone number from the contact string
        int lastHyphenIndex = contact.lastIndexOf("-");
        if (lastHyphenIndex != -1 && lastHyphenIndex < contact.length() - 2) {
            return contact.substring(lastHyphenIndex + 2).trim();
        } else {
            return "";
        }
    }

    private void sendSMS(String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            // Check if the message exceeds the SMS character limit
            if (message.length() > 160) {
                // Split the message into multiple parts if it's too long
                ArrayList<String> parts = smsManager.divideMessage(message);
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            }
            Log.d("SMS", "SMS sent to " + phoneNumber);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void onList(View view) {
        // Create an Intent to start LocationListActivity
        Intent intent = new Intent(this, LocationListActivity.class);
        startActivity(intent);
    }
}
