package com.example.dietplanner;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MealDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_detail);

        // Get data
        String items = getIntent().getStringExtra("meal_items");
        int cals = getIntent().getIntExtra("meal_cals", 0);
        String price = getIntent().getStringExtra("meal_price");

        // Get Macros (These default to 0 if not found)
        int protein = getIntent().getIntExtra("meal_pro", 0);
        int fat = getIntent().getIntExtra("meal_fat", 0);
        int carb = getIntent().getIntExtra("meal_carb", 0);

        // Initialize Views
        TextView tvDetailItems = findViewById(R.id.tvDetailItems);
        TextView tvDetailCals = findViewById(R.id.tvDetailCals);
        TextView tvDetailPrice = findViewById(R.id.tvDetailPrice);

        TextView tvPro = findViewById(R.id.tvProtein);
        TextView tvFat = findViewById(R.id.tvFats);
        TextView tvCarb = findViewById(R.id.tvCarbs);

        Button btnBack = findViewById(R.id.btnBack);

        // Set Data
        tvDetailItems.setText(items);
        tvDetailCals.setText(cals + " kcal");
        tvDetailPrice.setText(price);

        // Set Macros
        tvPro.setText("Protein\n" + protein + "g");
        tvFat.setText("Fats\n" + fat + "g");
        tvCarb.setText("Carbs\n" + carb + "g");

        btnBack.setOnClickListener(v -> finish());
    }
}