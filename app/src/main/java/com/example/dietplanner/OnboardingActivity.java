package com.example.dietplanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    EditText etAge, etHeight, etWeight;
    Spinner spnGender, spnActivity, spnTarget;
    Button btnFinish;
    final String FIREBASE_URL = "https://dietplannertest-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        // 1. Initialize Views
        etAge = findViewById(R.id.onAge);
        etHeight = findViewById(R.id.onHeight);
        etWeight = findViewById(R.id.onWeight);
        spnGender = findViewById(R.id.spnGender);
        spnActivity = findViewById(R.id.spnActivity);
        spnTarget = findViewById(R.id.spnTarget);
        btnFinish = findViewById(R.id.btnFinishOnboarding);

        // 2. Setup Spinners
        setupSpinners();

        // 3. Calculation & Save Logic
        btnFinish.setOnClickListener(v -> handleOnboarding());
    }

    private void setupSpinners() {
        // Gender Spinner
        List<String> genders = new ArrayList<>();
        genders.add("Male");
        genders.add("Female");
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genders);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnGender.setAdapter(genderAdapter);

        // Activity Level Spinner (Industry Standard Factors)
        List<String> activities = new ArrayList<>();
        activities.add("Sedentary (Office job, little exercise)");
        activities.add("Lightly Active (1-3 days/week)");
        activities.add("Moderately Active (3-5 days/week)");
        activities.add("Very Active (6-7 days/week)");
        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, activities);
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnActivity.setAdapter(activityAdapter);

        // Target Spinner
        List<String> targets = new ArrayList<>();
        targets.add("Maintain Weight");
        targets.add("Lose Weight (Deficit)");
        targets.add("Gain Weight (Surplus)");
        ArrayAdapter<String> targetAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, targets);
        targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnTarget.setAdapter(targetAdapter);
    }

    private void handleOnboarding() {
        String ageStr = etAge.getText().toString().trim();
        String heightStr = etHeight.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();

        if (ageStr.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all health details", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double weight = Double.parseDouble(weightStr);
            double height = Double.parseDouble(heightStr);
            int age = Integer.parseInt(ageStr);
            String gender = spnGender.getSelectedItem().toString();

            // 1. Calculate BMR (Mifflin-St Jeor)
            double bmr;
            if (gender.equals("Male")) {
                bmr = (10 * weight) + (6.25 * height) - (5 * age) + 5;
            } else {
                bmr = (10 * weight) + (6.25 * height) - (5 * age) - 161;
            }

            double factor = 1.2; // Default
            int activityIndex = spnActivity.getSelectedItemPosition();
            if (activityIndex == 1) factor = 1.375;
            else if (activityIndex == 2) factor = 1.55;
            else if (activityIndex == 3) factor = 1.725;

            double tdee = bmr * factor;

            int targetIndex = spnTarget.getSelectedItemPosition();
            if (targetIndex == 1) tdee -= 500; // Lose Weight
            else if (targetIndex == 2) tdee += 500; // Gain Weight

            int finalGoal = (int) tdee;

            saveToFirebase(ageStr, heightStr, weightStr, gender, finalGoal);

        } catch (Exception e) {
            Toast.makeText(this, "Calculation Error", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveToFirebase(String age, String height, String weight, String gender, int goal) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = prefs.getString("saved_username", "User");

        try {
            JSONObject healthData = new JSONObject();

            // Encrypted PHI
            healthData.put("age", EncryptionUtils.encrypt(age));
            healthData.put("height", EncryptionUtils.encrypt(height));
            healthData.put("weight", EncryptionUtils.encrypt(weight));

            // Plaintext metadata for logic
            healthData.put("gender", gender);
            healthData.put("activityIndex", spnActivity.getSelectedItemPosition()); // Save selection 0-3
            healthData.put("targetIndex", spnTarget.getSelectedItemPosition());     // Save selection 0-2
            healthData.put("dailyGoal", goal); // The calculated result

            String url = FIREBASE_URL + "users/" + username + ".json";

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.PATCH, url, healthData,
                    response -> {
                        // Cache the goal locally for the Landing Page
                        prefs.edit().putInt("daily_goal", goal).apply();

                        Toast.makeText(this, "Profile Synced to Cloud!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(OnboardingActivity.this, LandingActivity.class));
                        finish();
                    },
                    error -> Toast.makeText(this, "Sync Failed", Toast.LENGTH_SHORT).show()
            );
            Volley.newRequestQueue(this).add(request);
        } catch (Exception e) { e.printStackTrace(); }
    }
}