package com.example.dietplanner;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    TextView tvSelectedDate, tvTotalCals, tvTotalCost, tvHeaderTDEE;
    ImageButton btnPrev, btnNext;
    ListView lvHistory;
    CardView summaryCard;

    // Date Handling
    Calendar calendar;
    SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    ArrayList<Meal> historyList;
    HistoryAdapter adapter;
    final String FIREBASE_URL = "https://dietplannertest-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Init UI
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvTotalCals = findViewById(R.id.tvHistoryTotalCals);
        tvTotalCost = findViewById(R.id.tvHistoryTotalCost);
        btnPrev = findViewById(R.id.btnPrevDate);
        btnNext = findViewById(R.id.btnNextDate);
        lvHistory = findViewById(R.id.lvHistoryMeals);
        summaryCard = findViewById(R.id.summaryCard);
        tvHeaderTDEE = findViewById(R.id.tvHeaderTDEE);

        // Init Data
        calendar = Calendar.getInstance();
        historyList = new ArrayList<>();
        adapter = new HistoryAdapter();
        lvHistory.setAdapter(adapter);

        updateDateLabel();
        loadHistoryData();
        setupNavigation();

        // Date Picker Listener
        tvSelectedDate.setOnClickListener(v -> showDatePicker());

        // Navigation Button Listeners
        btnPrev.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            updateDateLabel();
            loadHistoryData();
        });

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        int dailyGoal = prefs.getInt("daily_goal", 2000);
        tvHeaderTDEE.setText("Goal: " + dailyGoal + " kcal");

        btnNext.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            updateDateLabel();
            loadHistoryData();
        });

        // Detail Click
        lvHistory.setOnItemClickListener((parent, view, position, id) -> {
            Meal m = historyList.get(position);
            Intent intent = new Intent(HistoryActivity.this, MealDetailActivity.class);
            intent.putExtra("meal_items", m.getItems());
            intent.putExtra("meal_cals", m.getCalories());
            intent.putExtra("meal_price", m.getPrice());
            intent.putExtra("meal_pro", m.getProtein());
            intent.putExtra("meal_fat", m.getFat());
            intent.putExtra("meal_carb", m.getCarb());
            startActivity(intent);
        });
    }

    private void showDatePicker() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    calendar.set(selectedYear, selectedMonth, selectedDay);
                    updateDateLabel();
                    loadHistoryData();
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void updateDateLabel() {
        tvSelectedDate.setText(displayFormat.format(calendar.getTime()));
    }

    private void loadHistoryData() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = prefs.getString("saved_username", "");
        // Retrieve the daily goal (defaulting to 2000 if not found)
        int dailyGoal = prefs.getInt("daily_goal", 2000);

        String targetDate = dbFormat.format(calendar.getTime());
        String url = FIREBASE_URL + "meals/" + username + ".json";

        tvTotalCals.setText("Loading...");
        historyList.clear();
        adapter.notifyDataSetChanged();

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    int dayCalories = 0;
                    double dayCost = 0.0;

                    try {
                        Iterator<String> keys = response.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            JSONObject mealObj = response.getJSONObject(key);

                            if (mealObj.optString("date").equals(targetDate)) {
                                String calStr = EncryptionUtils.decrypt(mealObj.getString("calories"));
                                int cals = Integer.parseInt(calStr);
                                String items = mealObj.optString("items", "");
                                String priceStr = mealObj.optString("total_price", "$0.00");
                                double rawPrice = mealObj.optDouble("raw_price", 0.0);

                                int p = mealObj.optInt("protein", 0);
                                int f = mealObj.optInt("fat", 0);
                                int c = mealObj.optInt("carb", 0);

                                dayCalories += cals;
                                dayCost += rawPrice;

                                historyList.add(new Meal(key, items, cals, priceStr, p, f, c));
                            }
                        }

                        tvTotalCals.setText(dayCalories + " kcal");
                        tvTotalCost.setText(String.format(Locale.getDefault(), "$%.2f", dayCost));

                        // --- Color Logic Based on ±100 kcal Tolerance ---
                        int lowerBound = dailyGoal - 100;
                        int upperBound = dailyGoal + 100;

                        if (dayCalories >= lowerBound && dayCalories <= upperBound) {
                            // Target Hit (Green)
                            summaryCard.setCardBackgroundColor(Color.parseColor("#E8F5E9")); // Light Green
                            tvTotalCals.setTextColor(Color.parseColor("#2E7D32")); // Dark Green Text
                        } else {
                            // Target Missed (Red)
                            summaryCard.setCardBackgroundColor(Color.parseColor("#FFEBEE")); // Light Red
                            tvTotalCals.setTextColor(Color.parseColor("#C62828")); // Dark Red Text
                        }

                        adapter.notifyDataSetChanged();

                        if(historyList.isEmpty()) {
                            Toast.makeText(this, "No records for this date", Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        Log.e("HISTORY", "Error: " + e.getMessage());
                    }
                },
                error -> tvTotalCals.setText("Error")
        );
        Volley.newRequestQueue(this).add(request);
    }

    private class HistoryAdapter extends BaseAdapter {
        @Override
        public int getCount() { return historyList.size(); }
        @Override
        public Object getItem(int position) { return historyList.get(position); }
        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_meal, parent, false);
            }
            Meal meal = historyList.get(position);

            TextView tvTitle = convertView.findViewById(R.id.tvBowlTitle);
            TextView tvDetails = convertView.findViewById(R.id.tvBowlDetails);
            TextView tvPrice = convertView.findViewById(R.id.tvMealPrice);

            tvTitle.setText("Bowl " + (position + 1));
            String details = String.format(Locale.getDefault(),
                    "%d kcal | P: %dg F: %dg C: %dg",
                    meal.getCalories(), meal.getProtein(), meal.getFat(), meal.getCarb());

            tvDetails.setText(details);
            tvPrice.setText(meal.getPrice());
            return convertView;
        }
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_history);
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
                finish();
                return true;
            }
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }
}