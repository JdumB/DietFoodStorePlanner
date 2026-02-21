package com.example.dietplanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CheckoutActivity extends AppCompatActivity {

    TextView tvItems, tvTotal, tvCardNumber;
    Button btnPay;
    final String FIREBASE_URL = "https://dietplannertest-default-rtdb.asia-southeast1.firebasedatabase.app/";
    String username;
    String storedDecryptedCVV = ""; // To store the real CVV for verification

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        tvItems = findViewById(R.id.tvCheckoutItems);
        tvTotal = findViewById(R.id.tvCheckoutTotal);
        tvCardNumber = findViewById(R.id.tvCardNumber);
        btnPay = findViewById(R.id.btnPayNow);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = prefs.getString("saved_username", "");

        if (username.isEmpty()) {
            Toast.makeText(this, "Session expired.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(CheckoutActivity.this, MainActivity.class));
            finish();
            return;
        }

        // Display Data
        String orderItems = getIntent().getStringExtra("order_items");
        String totalPrice = getIntent().getStringExtra("total_price");
        tvItems.setText(orderItems != null ? orderItems : "No items selected");
        tvTotal.setText(totalPrice != null ? totalPrice : "$0.00");

        fetchCardDetails();

        // SECURITY: Ask for CVV before paying
        btnPay.setOnClickListener(v -> showSecurityDialog());
    }

    private void fetchCardDetails() {
        String url = FIREBASE_URL + "users/" + username + ".json";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String encryptedCard = response.getString("cardNumber");
                        String encryptedCVV = response.getString("cvv"); // Fetch saved CVV

                        String fullNumber = EncryptionUtils.decrypt(encryptedCard);
                        storedDecryptedCVV = EncryptionUtils.decrypt(encryptedCVV); // Store for check

                        if (fullNumber != null && fullNumber.length() >= 4) {
                            String masked = "**** **** **** " + fullNumber.substring(fullNumber.length() - 4);
                            tvCardNumber.setText(masked);
                            btnPay.setEnabled(true);
                        }
                    } catch (Exception e) {
                        tvCardNumber.setText("Card error");
                        btnPay.setEnabled(false);
                    }
                },
                error -> tvCardNumber.setText("Network Error"));
        Volley.newRequestQueue(this).add(request);
    }

    private void showSecurityDialog() {
        if (storedDecryptedCVV.isEmpty()) {
            Toast.makeText(this, "Security Error: No CVV on file", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Security Check");
        builder.setMessage("Please enter your card CVV to confirm:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Verify", (dialog, which) -> {
            String enteredCVV = input.getText().toString();
            if (enteredCVV.equals(storedDecryptedCVV)) {
                processPaymentAndLogNutrients();
            } else {
                Toast.makeText(this, "Incorrect CVV!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void processPaymentAndLogNutrients() {
        String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String url = FIREBASE_URL + "meals/" + username + "/" + System.currentTimeMillis() + ".json";

        try {
            JSONObject mealData = new JSONObject();

            // 1. Basic Info
            mealData.put("name", EncryptionUtils.encrypt("Custom Bowl"));
            mealData.put("items", getIntent().getStringExtra("order_items"));
            mealData.put("date", dateKey);

            // 2. Nutrition Data (Passed from BuildBowlActivity)
            mealData.put("protein", parseDoubleSafe(getIntent().getStringExtra("total_pro")));
            mealData.put("fat", parseDoubleSafe(getIntent().getStringExtra("total_fat")));
            mealData.put("carb", parseDoubleSafe(getIntent().getStringExtra("total_carb")));

            // 3. Clean Calories (Store as Encrypted String per your requirement)
            String rawCal = getIntent().getStringExtra("total_cals");
            String cleanCal = rawCal.replaceAll("[^0-9]", "");
            mealData.put("calories", EncryptionUtils.encrypt(cleanCal));

            // 4. Clean Price (Store raw number for math, string for display)
            String rawPriceStr = getIntent().getStringExtra("total_price"); // "Total Price: $10.00"
            double rawPrice = 0.0;
            if (rawPriceStr != null) {
                // Remove everything except numbers and dots
                String p = rawPriceStr.replaceAll("[^0-9.]", "");
                rawPrice = Double.parseDouble(p);
            }
            mealData.put("total_price", rawPriceStr); // Keep display string
            mealData.put("raw_price", rawPrice);      // Keep math number

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, mealData,
                    response -> {
                        Toast.makeText(this, "Payment Successful!", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(CheckoutActivity.this, LandingActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    },
                    error -> Toast.makeText(this, "Save Failed", Toast.LENGTH_SHORT).show()
            );
            Volley.newRequestQueue(this).add(request);

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Helper to extract "35.5" from "P: 35.5g"
    private double parseDoubleSafe(String input) {
        if (input == null) return 0.0;
        try {
            return Double.parseDouble(input.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }
}