package com.example.helpimstuck;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class NotifFragment extends Fragment {

    private ListView smsListView;
    private ArrayAdapter<String> smsAdapter;
    private NotifViewModel notifViewModel;

    // Declare SIM options (SIM 1, SIM 2, etc.)
    private String[] simOptions = {"SIM 1", "SIM 2"};
    private int selectedSimSlot = 0; // Default to SIM 1

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notif, container, false);

        smsListView = view.findViewById(R.id.smsListView);

        // Initialize ViewModel
        notifViewModel = new ViewModelProvider(requireActivity()).get(NotifViewModel.class);

        // Initialize adapter
        smsAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1);
        smsListView.setAdapter(smsAdapter);

        // Load messages from ViewModel
        refreshSmsList();

        // Add functionality to clear notifications
        view.findViewById(R.id.clearNotificationsButton).setOnClickListener(v -> clearNotifications());

        // Add functionality to logout button
        view.findViewById(R.id.logoutButton).setOnClickListener(v -> logout());

        // Add click listener for list items
        smsListView.setOnItemClickListener((parent, view1, position, id) -> {
            SmsEntity selectedSms = notifViewModel.getSmsMessages().get(position);
            showResponseDialog(selectedSms.sender);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Load messages from the database
        AppDatabase db = AppDatabase.getInstance(requireContext());
        new Thread(() -> {
            List<SmsEntity> messages = db.smsDao().getAllMessages();
            requireActivity().runOnUiThread(() -> {
                notifViewModel.setSmsMessages(messages);
                refreshSmsList();
            });
        }).start();
    }

    private void refreshSmsList() {
        smsAdapter.clear();
        for (SmsEntity sms : notifViewModel.getSmsMessages()) {
            String contactName = ContactHelper.getContactName(requireContext(), sms.sender);
            smsAdapter.add("From: " + contactName + "\n" + sms.message);
        }
        smsAdapter.notifyDataSetChanged();
    }

    // Function to clear notifications
    public void clearNotifications() {
        notifViewModel.clearSmsMessages();
        smsAdapter.clear();
        smsAdapter.notifyDataSetChanged();

        AppDatabase db = AppDatabase.getInstance(requireContext());
        new Thread(() -> db.smsDao().deleteAllMessages()).start();

        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }

        Toast.makeText(requireContext(), "All notifications cleared.", Toast.LENGTH_SHORT).show();
    }

    // Logout function
    private void logout() {
        FirebaseAuth.getInstance().signOut(); // Sign out the user
        Toast.makeText(requireContext(), "Logged out successfully.", Toast.LENGTH_SHORT).show();

        // Navigate to the login screen
        Intent intent = new Intent(requireContext(), Login.class);
        startActivity(intent);
        requireActivity().finish(); // Close the current activity to prevent going back
    }

    private void showResponseDialog(String sender) {
        String contactName = ContactHelper.getContactName(requireContext(), sender);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Respond to SMS");
        builder.setMessage("Do you want to accept or decline the message from " + contactName + "?\n" +
                "Current selected SIM: " + simOptions[selectedSimSlot]);

        builder.setSingleChoiceItems(simOptions, selectedSimSlot, (dialog, which) -> selectedSimSlot = which);
        builder.setPositiveButton("Accept", (dialog, which) -> showSimSelectionDialog(sender, "SOS Accepted"));
        builder.setNegativeButton("Decline", (dialog, which) -> showSimSelectionDialog(sender, "SOS Declined"));
        builder.setNeutralButton("Cancel", null);

        builder.show();
    }

    private void showSimSelectionDialog(String sender, String response) {
        AlertDialog.Builder simDialogBuilder = new AlertDialog.Builder(requireContext());
        simDialogBuilder.setTitle("Select SIM to send response")
                .setItems(simOptions, (dialog, which) -> sendResponse(sender, response, which))
                .show();
    }

    private void sendResponse(String sender, String response, int simSlot) {
        String message = "Response: " + response;

        SubscriptionManager subscriptionManager = (SubscriptionManager) requireContext().getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        if (subscriptionManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                try {
                    SubscriptionInfo subscriptionInfo = subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(simSlot);
                    if (subscriptionInfo != null) {
                        SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionInfo.getSubscriptionId());
                        smsManager.sendTextMessage(sender, null, message, null, null);
                        Toast.makeText(requireContext(), "Response sent: " + response + " via " + simOptions[simSlot], Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "No active subscription found for SIM " + (simSlot + 1), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Error sending SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "Dual SIM is not supported on this device.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}