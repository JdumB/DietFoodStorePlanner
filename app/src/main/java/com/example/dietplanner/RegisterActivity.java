package com.example.dietplanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    EditText etUser, etPass, etCard, etExpiry, etCVV;
    Button btnSubmit;
    final String FIREBASE_URL = "https://dietplannertest-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 1. Initialize Views (Only Account and Payment)
        etUser = findViewById(R.id.regUser);
        etPass = findViewById(R.id.regPass);
        etCard = findViewById(R.id.regCard);
        etExpiry = findViewById(R.id.regExpiry);
        etCVV = findViewById(R.id.regCVV);
        btnSubmit = findViewById(R.id.btnRegisterSubmit);

        // 2. Formatters for UX
        setupFormatters();

        // 3. Submit Logic
        btnSubmit.setOnClickListener(v -> handleRegistration());
    }

    private void handleRegistration() {
        String username = etUser.getText().toString().trim();
        String password = etPass.getText().toString().trim();
        String card = etCard.getText().toString().trim();
        String expiry = etExpiry.getText().toString().trim();
        String cvv = etCVV.getText().toString().trim();

        // Validation
        if (username.isEmpty() || password.isEmpty() || card.isEmpty()) {
            Toast.makeText(this, "Please fill in all account and payment fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject data = new JSONObject();

            // --- SECURITY LAYER: Encrypting sensitive account/payment fields ---
            data.put("username", username);
            data.put("password", EncryptionUtils.encrypt(password));
            data.put("cardNumber", EncryptionUtils.encrypt(card));
            data.put("expiry", EncryptionUtils.encrypt(expiry));
            data.put("cvv", EncryptionUtils.encrypt(cvv));

            String url = FIREBASE_URL + "users/" + username + ".json";

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, data,
                    response -> {
                        // Save username to SharedPreferences for session management
                        SharedPreferences.Editor editor = getSharedPreferences("UserPrefs", MODE_PRIVATE).edit();
                        editor.putString("saved_username", username);
                        editor.apply();

                        Toast.makeText(this, "Step 1 Complete: Account Secured!", Toast.LENGTH_SHORT).show();

                        // GO TO STEP 2: Onboarding
                        Intent intent = new Intent(RegisterActivity.this, OnboardingActivity.class);
                        startActivity(intent);
                        finish();
                    },
                    error -> Toast.makeText(this, "Network Error: Register failed", Toast.LENGTH_SHORT).show()
            );
            Volley.newRequestQueue(this).add(request);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Encryption Error", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupFormatters() {
        // --- Card Formatter (1234 1234...) ---
        etCard.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {
                if (s.length() > 0 && (s.length() % 5) == 0) {
                    if (s.charAt(s.length() - 1) == ' ') s.delete(s.length() - 1, s.length());
                    else s.insert(s.length() - 1, " ");
                }
            }
        });

        // --- Expiry Formatter (MM/YY) ---
        etExpiry.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {
                if (s.length() == 2 && !s.toString().contains("/")) {
                    s.append("/");
                }
            }
        });
    }
}