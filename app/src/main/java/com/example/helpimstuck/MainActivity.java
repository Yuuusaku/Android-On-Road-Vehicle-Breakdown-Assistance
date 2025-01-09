package com.example.helpimstuck;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.helpimstuck.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";  // For logging

    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check Firebase Auth state
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // If user is not signed in, navigate to the login screen
            navigateToLogin();
            return;
        }

        // User is signed in, proceed with loading the main screen
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        replaceFragment(new HomeMap());  // Replace with HomeMap fragment by default

        // Remove default background for BottomNavigationView
        binding.bottomNavigationView.setBackground(null);

        // Set listener for item selection in BottomNavigationView
        binding.bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.home) {
                replaceFragment(new HomeMap());
            } else if (item.getItemId() == R.id.sos) {
                replaceFragment(new SOSFragment());
            } else if (item.getItemId() == R.id.contacts) {
                replaceFragment(new ContactsFragment());
            } else if (item.getItemId() == R.id.notifs) {
                replaceFragment(new NotifFragment());
            }
            return true;
        });

        // Handle FAB click to add a contact number
        binding.floatingActionButton.setOnClickListener(view -> showAddContactDialog());

        // Start the SMS Receiver service
        Intent serviceIntent = new Intent(this, SmsReceiver.class);
        startService(serviceIntent);
    }

    @Override
    public void onBackPressed() {
        // Prevent going back to the login screen after logging in
        super.onBackPressed();
    }

    // Navigate to login activity if user is not authenticated
    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, Login.class);
        startActivity(intent);
        finish();  // Finish MainActivity so it is not in the back stack
    }

    // Method to replace the current fragment with a new one
    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }

    // Show dialog to manually add a contact number
    private void showAddContactDialog() {
        final EditText input = new EditText(this);
        input.setHint("Enter phone number");

        new AlertDialog.Builder(this)
                .setTitle("Add Contact Number")
                .setMessage("Please enter the contact number:")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String phoneNumber = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(phoneNumber)) {
                        addContactToPhonebook(phoneNumber);
                    } else {
                        Toast.makeText(MainActivity.this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Method to add a contact to the phonebook
    private void addContactToPhonebook(String phoneNumber) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            try {
                ContentValues values = new ContentValues();
                values.put(ContactsContract.RawContacts.ACCOUNT_TYPE, (String) null);
                values.put(ContactsContract.RawContacts.ACCOUNT_NAME, (String) null);

                Uri rawContactUri = getContentResolver().insert(ContactsContract.RawContacts.CONTENT_URI, values);
                if (rawContactUri == null) {
                    throw new Exception("Failed to insert RawContact.");
                }

                long rawContactId = ContentUris.parseId(rawContactUri);
                Log.d(TAG, "Raw Contact Inserted, ID: " + rawContactId);

                ContentValues phoneValues = new ContentValues();
                phoneValues.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
                phoneValues.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber);
                phoneValues.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
                phoneValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);

                Uri phoneUri = getContentResolver().insert(ContactsContract.Data.CONTENT_URI, phoneValues);
                Log.d(TAG, "Phone Number Inserted URI: " + phoneUri);

                if (phoneUri == null) {
                    throw new Exception("Failed to insert phone number.");
                }

                Toast.makeText(this, "Contact Added: " + phoneNumber, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error adding contact: " + e.getMessage(), e);
                Toast.makeText(this, "Error adding contact: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_CONTACTS}, 1);
        }
    }
}
