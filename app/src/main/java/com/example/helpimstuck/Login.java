package com.example.helpimstuck;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import android.Manifest;

public class Login extends AppCompatActivity {

    Button log;
    TextView create, forgotPassword;
    EditText email, password;
    FirebaseAuth mAuth;

    private static final String TAG = "LoginActivity";

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int CONTACTS_PERMISSION_REQUEST_CODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        mAuth = FirebaseAuth.getInstance();

        log = findViewById(R.id.btnLog);
        create = findViewById(R.id.txtCreate);
        forgotPassword = findViewById(R.id.txtForgot);  // Forgot Password TextView
        email = findViewById(R.id.logNumber);
        password = findViewById(R.id.logPass);

        requestPermissions();

        log.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String emailAddress = email.getText().toString().trim();
                String pass = password.getText().toString().trim();

                // Validate input
                if (TextUtils.isEmpty(emailAddress)) {
                    Toast.makeText(Login.this, "Enter Email Address", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(pass)) {
                    Toast.makeText(Login.this, "Enter your Password", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Attempt to sign in the user directly
                signIn(emailAddress, pass);
            }
        });

        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Login.this, Register.class);
                startActivity(intent);
            }
        });

        // Forgot password functionality
        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String emailAddress = email.getText().toString().trim();

                if (TextUtils.isEmpty(emailAddress)) {
                    Toast.makeText(Login.this, "Please enter your email address", Toast.LENGTH_SHORT).show();
                } else {
                    forgotPassword(emailAddress);
                }
            }
        });
    }

    private void signIn(String email, String password) {
        Log.d(TAG, "Attempting to sign in with email: " + email);

        // Try to sign in the user directly
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Sign-in successful");

                            // Navigate to the main activity
                            Toast.makeText(Login.this, "Login Successful", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Log.d(TAG, "Sign-in failed: " + task.getException());
                            Toast.makeText(Login.this, "Authentication failed. Please check your credentials.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Forgot password functionality
    private void forgotPassword(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(Login.this, "Password reset email sent. Please check your inbox.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(Login.this, "Error: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Method to check and request permissions
    private void requestPermissions() {
        // Check for Location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Check for Contacts permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, CONTACTS_PERMISSION_REQUEST_CODE);
        }
    }

    // Handle permission request results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted for location
                    Toast.makeText(this, "Location Permission Granted", Toast.LENGTH_SHORT).show();
                } else {
                    // Permission denied for location
                    Toast.makeText(this, "Location Permission Denied", Toast.LENGTH_SHORT).show();
                }
                break;

            case CONTACTS_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted for contacts
                    Toast.makeText(this, "Contacts Permission Granted", Toast.LENGTH_SHORT).show();
                } else {
                    // Permission denied for contacts
                    Toast.makeText(this, "Contacts Permission Denied", Toast.LENGTH_SHORT).show();
                }
                break;

            default:
                break;
        }
    }
}
