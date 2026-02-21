package com.example.dietplanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    EditText etUser, etPass;
    Button btnLogin, btnFingerprint;
    TextView btnGoToRegister;
    // URL from your database export
    final String FIREBASE_URL = "https://dietplannertest-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize Views
        etUser = findViewById(R.id.etUsername);
        etPass = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnFingerprint = findViewById(R.id.btnFingerprint);
        btnGoToRegister = findViewById(R.id.btnGoToRegister);

        // 2. Setup Biometric Engine
        setupBiometrics();

        // 3. Check for "Remembered" User to trigger Fingerprint
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String savedUser = prefs.getString("saved_username", "");

        // Auto-trigger fingerprint if we have a saved username
        if (!savedUser.isEmpty()) {
            biometricPrompt.authenticate(promptInfo);
        }

        // 4. Navigation to Register
        btnGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
        });

        // 5. Standard Login
        btnLogin.setOnClickListener(v -> performLogin());

        // 6. Manual Fingerprint Button
        btnFingerprint.setOnClickListener(v -> {
            if (!savedUser.isEmpty()) {
                biometricPrompt.authenticate(promptInfo);
            } else {
                Toast.makeText(this, "Please log in with password once first", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupBiometrics() {
        Executor executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(MainActivity.this, "Biometric Login Success!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LandingActivity.class));
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(MainActivity.this, "Security Error: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(MainActivity.this, "Fingerprint not recognized", Toast.LENGTH_SHORT).show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Secure Biometric Login")
                .setSubtitle("Confirm your identity to access your diet plan")
                .setNegativeButtonText("Use Password")
                .build();
    }

    private void performLogin() {
        String user = etUser.getText().toString().trim();
        String pass = etPass.getText().toString().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = FIREBASE_URL + "users/" + user + ".json";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String storedEncryptedPass = response.getString("password");
                        String encryptedInput = EncryptionUtils.encrypt(pass);

                        if (encryptedInput.equals(storedEncryptedPass)) {
                            // 1. Extract values from the JSON response
                            // Defaults are provided in case the fields are missing for a specific user
                            int activityIndex = response.optInt("activityIndex", 1);
                            int targetIndex = response.optInt("targetIndex", 3);
                            int dailyGoal = response.optInt("dailyGoal", 2000);

                            // 2. Save all values to SharedPreferences (Session)
                            SharedPreferences.Editor editor = getSharedPreferences("UserPrefs", MODE_PRIVATE).edit();
                            editor.putString("saved_username", user);

                            // UPDATED: Save indexes so LandingActivity can map them to text
                            editor.putInt("activity_index", activityIndex);
                            editor.putInt("target_index", targetIndex);
                            editor.putInt("daily_goal", dailyGoal);

                            editor.apply();

                            Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MainActivity.this, LandingActivity.class));
                        } else {
                            Toast.makeText(this, "Incorrect Password", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }
}