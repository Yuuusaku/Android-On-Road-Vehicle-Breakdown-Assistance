package com.example.helpimstuck;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;

public class ContactsFragment extends Fragment {

    private ListView contactsListView;
    private ArrayList<String> contactsList;
    private ArrayAdapter<String> adapter;
    private FirebaseFirestore firestore;

    private static final int CONTACTS_PERMISSION_REQUEST_CODE = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        contactsListView = view.findViewById(R.id.contactsView);
        contactsList = new ArrayList<>();
        firestore = FirebaseFirestore.getInstance();

        // Check and request permissions
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.READ_CONTACTS},
                    CONTACTS_PERMISSION_REQUEST_CODE);
        } else {
            getContacts(); // Fetch contacts if permission granted
        }

        // Set up the adapter for the ListView
        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, contactsList);
        contactsListView.setAdapter(adapter);

        // Set an OnItemClickListener to open the dialer with the selected contact's number
        contactsListView.setOnItemClickListener((parent, view1, position, id) -> {
            String contact = contactsList.get(position);
            // Extract the phone number by splitting the string at the " - "
            String phoneNumber = contact.split(" - ")[1].trim();

            // Open the dialer with the full phone number
            dialPhoneNumber(phoneNumber);
        });

        return view;
    }

    // Method to fetch contacts and populate the list
    private void getContacts() {
        Cursor cursor = getActivity().getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        );

        if (cursor == null) {
            Toast.makeText(getActivity(), "Failed to retrieve contacts", Toast.LENGTH_SHORT).show();
            return;
        }

        HashSet<String> phoneNumbers = new HashSet<>();
        int nameColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        int phoneNumberColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

        // Add constant emergency contacts first
        contactsList.add("Bureau of Fire Protection - (02) 8426-0219");
        contactsList.add("Philippine National Police - (02) 8722-0650");
        contactsList.add("Philippine Coast Guard - (02) 8527-3877");
        contactsList.add("MMDA - (02) 882 4151");

        // Add contacts from phone book
        while (cursor.moveToNext()) {
            String name = cursor.getString(nameColumnIndex);
            String phoneNumber = cursor.getString(phoneNumberColumnIndex).replaceAll("\\s", "");
            phoneNumbers.add(phoneNumber);
            contactsList.add(name + " - " + phoneNumber); // Default display
        }

        cursor.close();

        // Query Firebase to check which numbers have the app
        checkContactsInFirestore(phoneNumbers);
    }

    // Method to check contacts in Firestore
    private void checkContactsInFirestore(HashSet<String> phoneNumbers) {
        firestore.collection("users")
                .whereIn("phoneNumber", new ArrayList<>(phoneNumbers))
                .get()
                .addOnSuccessListener(querySnapshot -> updateContactsList(querySnapshot))
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), "Error fetching data from Firestore.", Toast.LENGTH_SHORT).show();
                });
    }

    // Method to update the contacts list based on Firestore data
    private void updateContactsList(QuerySnapshot querySnapshot) {
        HashSet<String> appUsers = new HashSet<>();
        for (com.google.firebase.firestore.DocumentSnapshot document : querySnapshot.getDocuments()) {
            String phoneNumber = document.getString("phoneNumber");
            if (phoneNumber != null) {
                appUsers.add(phoneNumber);
            }
        }

        for (int i = 0; i < contactsList.size(); i++) {
            String contact = contactsList.get(i);
            String phoneNumber = contact.split(" - ")[1].trim();
            if (appUsers.contains(phoneNumber)) {
                contactsList.set(i, contact + " (Has App)");
            }
        }

        // Notify the adapter of data changes
        adapter.notifyDataSetChanged();
    }

    // Method to dial the selected phone number
    private void dialPhoneNumber(String phoneNumber) {
        Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
        startActivity(dialIntent);
    }

    // Handle permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CONTACTS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getContacts(); // Fetch contacts if permission is granted
            } else {
                Toast.makeText(getActivity(), "Permission denied, cannot access contacts.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
