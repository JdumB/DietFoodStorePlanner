package com.example.dietplanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

public class LandingActivity extends AppCompatActivity {

    // UI Elements
    TextView tvWelcome, tvCalories, tvEEInfo, tvRemainingText;
    TextView tvTotalPro, tvTotalFat, tvTotalCarb;
    ProgressBar calProgressBar;
    Button btnBuildBowl;
    ListView lvMeals;

    // Data handling
    ArrayList<Meal> mealObjects;
    MealAdapter mealAdapter;

    int dailyGoal;
    int totalConsumed = 0;

    // Daily Macro Totals
    int dayProtein = 0, dayFat = 0, dayCarb = 0;

    final String FIREBASE_URL = "https://dietplannertest-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        // 1. Initialize UI
        tvWelcome = findViewById(R.id.tvWelcome);
        tvCalories = findViewById(R.id.tvCalories);
        tvRemainingText = findViewById(R.id.tvRemainingText);

        // This is the line that will display your "Total Daily EE..." message
        tvEEInfo = findViewById(R.id.tvSecurityStatus);

        // Macro Summaries
        tvTotalPro = findViewById(R.id.tvTotalPro);
        tvTotalFat = findViewById(R.id.tvTotalFat);
        tvTotalCarb = findViewById(R.id.tvTotalCarb);

        calProgressBar = findViewById(R.id.calProgressBar);
        btnBuildBowl = findViewById(R.id.btnBuildBowl);
        lvMeals = findViewById(R.id.lvMeals);

        // 2. Setup Adapter
        mealObjects = new ArrayList<>();
        mealAdapter = new MealAdapter();
        lvMeals.setAdapter(mealAdapter);

        // 3. Click Listener (Passes data to detail)
        lvMeals.setOnItemClickListener((parent, view, position, id) -> {
            Meal selectedMeal = mealObjects.get(position);
            Intent intent = new Intent(LandingActivity.this, MealDetailActivity.class);
            intent.putExtra("meal_items", selectedMeal.getItems());
            intent.putExtra("meal_cals", selectedMeal.getCalories());
            intent.putExtra("meal_price", selectedMeal.getPrice());
            intent.putExtra("meal_pro", selectedMeal.getProtein());
            intent.putExtra("meal_fat", selectedMeal.getFat());
            intent.putExtra("meal_carb", selectedMeal.getCarb());
            startActivity(intent);
        });

        setupNavigation();
        btnBuildBowl.setOnClickListener(v -> startActivity(new Intent(this, BuildBowlActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void loadDashboardData() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // Retrieve values from SharedPreferences
        String username = prefs.getString("saved_username", "User");

        int actIdx = prefs.getInt("activity_index", 3);
        int targetIdx = prefs.getInt("target_index", 2);
        dailyGoal = prefs.getInt("daily_goal", 2000);

        String[] activityLabels = {"Sedentary", "Lightly Active", "Moderately Active", "Very Active"};
        String[] targetLabels = {"Maintain Weight", "Lose Weight", "Gain Weight"};

        // 3. Convert Index to String
        // Based on your database: User 'qwe' has activityIndex 3 and targetIndex 0
        String activity = (actIdx >= 0 && actIdx < activityLabels.length) ? activityLabels[actIdx] : "Moderate";
        String target = (targetIdx >= 0 && targetIdx < targetLabels.length) ? targetLabels[targetIdx] : "Maintain";

        // Update the Welcome Heading
        tvWelcome.setText("Welcome back, " + username);

        // UPDATED: Dynamic EE info string as requested
        // Example: "Total Daily EE for Very Active and Lose Weight is 3027 kcal"
        String eeMessage = "Total Daily EE for " + activity + " and " + target + " is " + dailyGoal + " kcal";
        tvEEInfo.setText(eeMessage);

        fetchTodayMeals(username);
    }

    private void fetchTodayMeals(String username) {
        String url = FIREBASE_URL + "meals/" + username + ".json";
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        totalConsumed = 0;
        dayProtein = 0;
        dayFat = 0;
        dayCarb = 0;
        mealObjects.clear();

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Iterator<String> keys = response.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            JSONObject mealObj = response.getJSONObject(key);

                            if (mealObj.optString("date").equals(todayDate)) {
                                // Decrypt data (Ensure you have EncryptionUtils class in your project)
                                String calStr = EncryptionUtils.decrypt(mealObj.getString("calories"));
                                int calories = Integer.parseInt(calStr);

                                String items = mealObj.optString("items", "");
                                String price = mealObj.optString("total_price", "$0.00");

                                // Fetch Macros (default to 0 if missing)
                                int p = mealObj.optInt("protein", 0);
                                int f = mealObj.optInt("fat", 0);
                                int c = mealObj.optInt("carb", 0);

                                // Add to totals
                                totalConsumed += calories;
                                dayProtein += p;
                                dayFat += f;
                                dayCarb += c;

                                mealObjects.add(new Meal(key, items, calories, price, p, f, c));
                            }
                        }
                        updateUI();
                    } catch (Exception e) {
                        Log.e("DATA", "Error parsing: " + e.getMessage());
                        updateUI();
                    }
                }, error -> updateUI());

        Volley.newRequestQueue(this).add(request);
    }

    private void updateUI() {
        // 1. Update Macro Card
        tvTotalPro.setText(dayProtein + "g");
        tvTotalFat.setText(dayFat + "g");
        tvTotalCarb.setText(dayCarb + "g");

        // 2. Calories & Progress Logic
        int remaining = dailyGoal - totalConsumed;

        // Prevent division by zero if dailyGoal isn't loaded yet
        double progressPercent = (dailyGoal > 0) ? ((double) totalConsumed / (double) dailyGoal) * 100 : 0;
        calProgressBar.setProgress((int) progressPercent);

        if (remaining < 0) {
            // User exceeded goal
            tvCalories.setText(String.valueOf(Math.abs(remaining)));
            tvCalories.setTextColor(Color.RED);
            tvRemainingText.setText("Over");
            tvRemainingText.setTextColor(Color.RED);
        } else {
            // User is within goal
            tvCalories.setText(String.valueOf(remaining));
            tvCalories.setTextColor(Color.parseColor("#212121"));
            tvRemainingText.setText("Left");
            tvRemainingText.setTextColor(Color.parseColor("#757575"));
        }

        mealAdapter.notifyDataSetChanged();
    }

    private class MealAdapter extends BaseAdapter {
        @Override
        public int getCount() { return mealObjects.size(); }
        @Override
        public Object getItem(int position) { return mealObjects.get(position); }
        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_meal, parent, false);
            }

            Meal meal = mealObjects.get(position);

            TextView tvTitle = convertView.findViewById(R.id.tvBowlTitle);
            TextView tvDetails = convertView.findViewById(R.id.tvBowlDetails);
            TextView tvPrice = convertView.findViewById(R.id.tvMealPrice);

            tvTitle.setText("Bowl " + (position + 1));

            String details = String.format(Locale.getDefault(),
                    "%d kcal | P: %dg | F: %dg | C: %dg",
                    meal.getCalories(), meal.getProtein(), meal.getFat(), meal.getCarb());

            tvDetails.setText(details);
            tvPrice.setText(meal.getPrice());

            return convertView;
        }
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_home) return true;
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
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }
}