package com.example.helpimstuck;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SOSFragment extends Fragment {

    private static final int CONTACTS_PERMISSION_REQUEST_CODE = 1;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2;
    private static final int PHONE_STATE_PERMISSION_REQUEST_CODE = 3;

    private ListView contactListView;
    private Button sendSOSButton;
    private ArrayList<String> contactsList;
    private ArrayAdapter<String> adapter;
    private Map<Integer, Boolean> checkBoxMap;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_s_o_s, container, false);

        contactListView = view.findViewById(R.id.contactListView);
        sendSOSButton = view.findViewById(R.id.sendSOSButton);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());

        checkAndRequestContactsPermission();

        sendSOSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndRequestLocationPermission();
            }
        });

        contactListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedContact = contactsList.get(position);
                checkBoxMap.put(position, !checkBoxMap.getOrDefault(position, false));
                adapter.notifyDataSetChanged();
            }
        });

        return view;
    }

    private void checkAndRequestContactsPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_CONTACTS}, CONTACTS_PERMISSION_REQUEST_CODE);
        } else {
            getContactList();
        }
    }

    private void checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLocationAndSendSOSMessage();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CONTACTS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getContactList();
            } else {
                Toast.makeText(getContext(), "Contacts permission is required for this app to function properly", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndSendSOSMessage();
            } else {
                Toast.makeText(getContext(), "Location permission is required to send your current location", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getContactList() {
        contactsList = new ArrayList<>();
        Cursor cursor = getContext().getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        );

        if (cursor == null) {
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

        checkBoxMap = new HashMap<>();
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_multiple_choice, contactsList);
        contactListView.setAdapter(adapter);
        contactListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    }

    private void getLocationAndSendSOSMessage() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        showEmergencyTypeDialog(latitude, longitude);
                    } else {
                        Toast.makeText(getContext(), "Unable to retrieve current location", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            Toast.makeText(getContext(), "Location permission not granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEmergencyTypeDialog(double latitude, double longitude) {
        String[] emergencyTypes = {"Vehicle Breakdown", "Health Emergency", "Flat Tire", "Battery Dead"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Emergency Type")
                .setItems(emergencyTypes, (dialog, which) -> {
                    String sosMessage = generateSOSMessage(which, latitude, longitude);
                    sendSOSMessageToSelectedContacts(sosMessage);
                })
                .show();
    }

    private String generateSOSMessage(int emergencyType, double latitude, double longitude) {
        String sosMessage = "Emergency Assistance Request SOS: I need immediate help! My situation is as follows:\n" +
                "Emergency Type: ";

        // Detailed descriptions for each emergency type
        switch (emergencyType) {
            case 0: // Vehicle Breakdown
                sosMessage += "Vehicle Breakdown! I am stranded on the road. ";
                break;
            case 1: // Health Emergency
                sosMessage += "Health Emergency! I need medical assistance immediately. ";
                break;
            case 2: // Flat Tire
                sosMessage += "Flat Tire! I am unable to drive due to a flat tire. ";
                break;
            case 3: // Battery Dead
                sosMessage += "Battery Dead! My vehicle won't start due to a dead battery. ";
                break;
        }

        // Adding detailed location information
        sosMessage += "My current location is:\n" +
                "Latitude: " + latitude + "\n" +
                "Longitude: " + longitude + "\n" +
                "Please assist me as soon as possible!\n\n" +
                "Paste my coordinates in your map app(" + latitude + "," + longitude + ")";

        return sosMessage;
    }

    private void sendSOSMessageToSelectedContacts(String sosMessage) {
        String[] simOptions = {"SIM 1", "SIM 2"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select SIM to send SMS")
                .setItems(simOptions, (dialog, which) -> {
                    for (Map.Entry<Integer, Boolean> entry : checkBoxMap.entrySet()) {
                        int position = entry.getKey();
                        boolean isChecked = entry.getValue();

                        if (isChecked) {
                            String selectedContact = contactsList.get(position);
                            String phoneNumber = getPhoneNumber(selectedContact);
                            sendSMS(phoneNumber, sosMessage, which);
                        }
                    }
                })
                .show();
    }

    private String getPhoneNumber(String contact) {
        int lastHyphenIndex = contact.lastIndexOf("-");
        if (lastHyphenIndex != -1 && lastHyphenIndex < contact.length() - 2) {
            return contact.substring(lastHyphenIndex + 2).trim();
        } else {
            return "";
        }
    }

    private void sendSMS(String phoneNumber, String message, int simSlot) {
        try {
            SubscriptionManager subscriptionManager = SubscriptionManager.from(getContext());
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_PHONE_STATE}, PHONE_STATE_PERMISSION_REQUEST_CODE);
                return;
            }

            List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
            if (subscriptionInfoList != null && subscriptionInfoList.size() > simSlot) {
                int subscriptionId = subscriptionInfoList.get(simSlot).getSubscriptionId();
                SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId);

                if (message.length() > 160) {
                    ArrayList<String> parts = smsManager.divideMessage(message);
                    smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
                } else {
                    smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                }
            }
        } catch (Exception e) {
            Log.e("SOSFragment", "Failed to send SMS", e);
            Toast.makeText(getContext(), "Failed to send SOS message", Toast.LENGTH_SHORT).show();
        }
    }
}
