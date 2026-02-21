package com.example.dietplanner;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class ProfileActivity extends AppCompatActivity {

    // UI Elements
    TextView tvUser, tvCard, tvBMI, tvTDEE, tvGender;
    EditText etAge, etWeight, etHeight;
    Spinner spnActivity, spnTarget;
    ImageView ivEdit;
    Button btnDecrypt;

    // State & Data
    boolean isEditMode = false;
    String encCard = "", gender = "Male";
    final String FIREBASE_URL = "https://dietplannertest-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 1. Initialize Views
        tvUser = findViewById(R.id.tvProfileUser);
        tvCard = findViewById(R.id.tvSecureCard);
        tvBMI = findViewById(R.id.tvBMI);
        tvTDEE = findViewById(R.id.tvTDEE);
        tvGender = findViewById(R.id.tvGenderDisplay);
        etAge = findViewById(R.id.etAge);
        etWeight = findViewById(R.id.etWeight);
        etHeight = findViewById(R.id.etHeight);
        ivEdit = findViewById(R.id.ivEditProfile);
        btnDecrypt = findViewById(R.id.btnDecrypt);
        spnActivity = findViewById(R.id.spnProfileActivity);
        spnTarget = findViewById(R.id.spnProfileTarget);

        // 2. Setup Spinner Data
        setupSpinners();

        // 3. Initial Data Load
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = prefs.getString("saved_username", "User");
        tvUser.setText("Username: " + username);
        fetchUserData(username);

        // 4. Listeners
        ivEdit.setOnClickListener(v -> {
            if (!isEditMode) enableEditing(true);
            else verifyAndSave(); // Biometric check before saving
        });

        btnDecrypt.setOnClickListener(v -> showBiometricPrompt());

        setupNavigation();
    }

    private void setupSpinners() {
        List<String> activities = new ArrayList<>();
        activities.add("Sedentary");
        activities.add("Lightly Active");
        activities.add("Moderately Active");
        activities.add("Very Active");
        ArrayAdapter<String> actAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, activities);
        actAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnActivity.setAdapter(actAdapter);

        List<String> targets = new ArrayList<>();
        targets.add("Maintain Weight");
        targets.add("Lose Weight");
        targets.add("Gain Weight");
        ArrayAdapter<String> tarAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, targets);
        tarAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnTarget.setAdapter(tarAdapter);
    }

    private void enableEditing(boolean enable) {
        isEditMode = enable;
        etAge.setEnabled(enable);
        etWeight.setEnabled(enable);
        etHeight.setEnabled(enable);
        spnActivity.setEnabled(enable);
        spnTarget.setEnabled(enable);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if (enable) {
            ivEdit.setImageResource(android.R.drawable.ic_menu_save);
            etAge.requestFocus();
            imm.showSoftInput(etAge, InputMethodManager.SHOW_IMPLICIT);
            Toast.makeText(this, "Edit Mode Active", Toast.LENGTH_SHORT).show();
        } else {
            ivEdit.setImageResource(android.R.drawable.ic_menu_edit);
            imm.hideSoftInputFromWindow(etAge.getWindowToken(), 0);
        }
    }

    private void verifyAndSave() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                saveDataToFirebase();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirm Changes")
                .setSubtitle("Fingerprint required to save your health plan")
                .setNegativeButtonText("Cancel").build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void saveDataToFirebase() {
        try {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String username = prefs.getString("saved_username", "User");

            double w = Double.parseDouble(etWeight.getText().toString());
            double h = Double.parseDouble(etHeight.getText().toString());
            int a = Integer.parseInt(etAge.getText().toString());

            double bmr = (gender.equalsIgnoreCase("Male")) ?
                    (10 * w) + (6.25 * h) - (5 * a) + 5 :
                    (10 * w) + (6.25 * h) - (5 * a) - 161;

            double[] factors = {1.2, 1.375, 1.55, 1.725};
            double tdee = bmr * factors[spnActivity.getSelectedItemPosition()];

            int targetIdx = spnTarget.getSelectedItemPosition();
            if (targetIdx == 1) tdee -= 500;
            else if (targetIdx == 2) tdee += 500;

            // FIX: Variable for lambda must be final
            final int finalCalculatedGoal = (int) tdee;

            JSONObject updates = new JSONObject();
            updates.put("age", EncryptionUtils.encrypt(String.valueOf(a)));
            updates.put("weight", EncryptionUtils.encrypt(String.valueOf(w)));
            updates.put("height", EncryptionUtils.encrypt(String.valueOf(h)));
            updates.put("activityIndex", spnActivity.getSelectedItemPosition());
            updates.put("targetIndex", targetIdx);
            updates.put("dailyGoal", finalCalculatedGoal);

            String url = FIREBASE_URL + "users/" + username + ".json";

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.PATCH, url, updates,
                    response -> {
                        // Use the final variable here
                        prefs.edit().putInt("daily_goal", finalCalculatedGoal).apply();
                        enableEditing(false);
                        fetchUserData(username);
                        Toast.makeText(this, "Health Plan Updated!", Toast.LENGTH_SHORT).show();
                    },
                    error -> Toast.makeText(this, "Save Failed", Toast.LENGTH_SHORT).show());

            Volley.newRequestQueue(this).add(request);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void fetchUserData(String user) {
        String url = FIREBASE_URL + "users/" + user + ".json";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        encCard = response.getString("cardNumber");
                        gender = response.getString("gender");

                        etAge.setText(EncryptionUtils.decrypt(response.getString("age")));
                        etHeight.setText(EncryptionUtils.decrypt(response.getString("height")));
                        etWeight.setText(EncryptionUtils.decrypt(response.getString("weight")));
                        tvGender.setText("Gender: " + gender);

                        spnActivity.setSelection(response.getInt("activityIndex"));
                        spnTarget.setSelection(response.getInt("targetIndex"));

                        calculateHealthDisplays();

                    } catch (Exception e) { e.printStackTrace(); }
                }, null);
        Volley.newRequestQueue(this).add(request);
    }

    private void calculateHealthDisplays() {
        try {
            double w = Double.parseDouble(etWeight.getText().toString());
            double h = Double.parseDouble(etHeight.getText().toString());
            double bmi = w / ((h/100) * (h/100));
            tvBMI.setText(String.format("BMI: %.1f", bmi));

            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            int goal = prefs.getInt("daily_goal", 0);
            tvTDEE.setText("Goal: " + goal + " kcal");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                try {
                    tvCard.setText(EncryptionUtils.decrypt(encCard));
                    btnDecrypt.setText("Card Unlocked");
                    btnDecrypt.setEnabled(false);
                } catch (Exception e) { e.printStackTrace(); }
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("View Payment Card")
                .setNegativeButtonText("Cancel").build();
        biometricPrompt.authenticate(promptInfo);
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_profile);
        bottomNav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, LandingActivity.class));
                finish();
                return true;
            }
            if (id == R.id.nav_build) {
                startActivity(new Intent(this, BuildBowlActivity.class));
                finish();
                return true;
            }
            if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                finish();
                return true;
            }
            if (id == R.id.nav_profile) {
                finish();
                return true;
            }
            return false;
        });
    }
}