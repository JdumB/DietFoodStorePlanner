package com.example.dietplanner;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BuildBowlActivity extends AppCompatActivity {

    // UI Elements
    TextView tvLiveTotal, tvSummary, tvTotalPrice, tvDetailedList;
    TextView tvTotalPro, tvTotalCarb, tvTotalFat;
    Button btnSubmit;
    RecyclerView rvBase, rvProtein, rvVeggie, rvExtra;
    RelativeLayout rlToggleCart;
    ImageView ivExpandArrow;

    // Data handling
    List<FoodItem> masterFoodList = new ArrayList<>();
    Map<String, Map<String, Integer>> categorySelectionMap = new HashMap<>();
    boolean isCartExpanded = false;

    // Adapters
    FoodAdapter adapterBase, adapterProtein, adapterVeggie, adapterExtra;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_build_bowl);

        // 1. Initialize UI
        tvLiveTotal = findViewById(R.id.tvLiveTotal);
        tvSummary = findViewById(R.id.tvSelectionSummary);
        tvDetailedList = findViewById(R.id.tvDetailedList);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        tvTotalPro = findViewById(R.id.tvTotalPro);
        tvTotalCarb = findViewById(R.id.tvTotalCarb);
        tvTotalFat = findViewById(R.id.tvTotalFat);
        btnSubmit = findViewById(R.id.btnSubmitBowl);
        rlToggleCart = findViewById(R.id.rlToggleCart);
        ivExpandArrow = findViewById(R.id.ivExpandArrow);

        rvBase = findViewById(R.id.rvBase);
        rvProtein = findViewById(R.id.rvProtein);
        rvVeggie = findViewById(R.id.rvVeggie);
        rvExtra = findViewById(R.id.rvExtra);

        // 2. Setup Grid Layouts
        setupGridLayout(rvBase);
        setupGridLayout(rvProtein);
        setupGridLayout(rvVeggie);
        setupGridLayout(rvExtra);

        // 3. INITIALIZE EMPTY ADAPTERS IMMEDIATELY (Prevents "No adapter attached" error)
        initializeEmptyAdapters();

        // 4. Cart Expand/Collapse Logic
        rlToggleCart.setOnClickListener(v -> toggleCart());

        // 5. Data & Navigation
        fetchFoodData();
        setupNavigation();

        btnSubmit.setOnClickListener(v -> submitOrder());
    }

    private void setupGridLayout(RecyclerView rv) {
        rv.setLayoutManager(new GridLayoutManager(this, 2));
        rv.setNestedScrollingEnabled(false);
    }

    private void initializeEmptyAdapters() {
        // We create the adapters with empty lists first so the UI can draw immediately
        adapterBase = new FoodAdapter(this, new ArrayList<>(), q -> updateSelection("base", q));
        adapterProtein = new FoodAdapter(this, new ArrayList<>(), q -> updateSelection("protein", q));
        adapterVeggie = new FoodAdapter(this, new ArrayList<>(), q -> updateSelection("veggie", q));
        adapterExtra = new FoodAdapter(this, new ArrayList<>(), q -> updateSelection("extra", q));

        rvBase.setAdapter(adapterBase);
        rvProtein.setAdapter(adapterProtein);
        rvVeggie.setAdapter(adapterVeggie);
        rvExtra.setAdapter(adapterExtra);
    }

    private void toggleCart() {
        isCartExpanded = !isCartExpanded;
        if (isCartExpanded) {
            tvDetailedList.setVisibility(View.VISIBLE);
            ivExpandArrow.setRotation(180f);
            tvSummary.setText("Hide selected items");
        } else {
            tvDetailedList.setVisibility(View.GONE);
            ivExpandArrow.setRotation(0f);
            tvSummary.setText("View selected items");
        }
    }

    private void fetchFoodData() {
        // NOTE: Ensure your Firebase has a "products" key, or change this logic!
        String url = "https://dietplannertest-default-rtdb.asia-southeast1.firebasedatabase.app/.json";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        masterFoodList.clear();

                        // Check if "products" exists, otherwise try parsing the root
                        JSONObject productsObj = response.optJSONObject("products");
                        if (productsObj == null) {
                            // Fallback: assume the root response IS the list of products
                            productsObj = response;
                        }

                        if (productsObj != null) {
                            Iterator<String> keys = productsObj.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                // Skip "users" or "meals" nodes if we are parsing the root
                                if (key.equals("users") || key.equals("meals")) continue;

                                JSONObject f = productsObj.optJSONObject(key);
                                if (f != null) {
                                    masterFoodList.add(new FoodItem(
                                            key,
                                            f.optString("name", "Unknown"),
                                            f.optInt("cal", 0),
                                            f.optDouble("pro", 0.0),
                                            f.optDouble("fat", 0.0),
                                            f.optDouble("carb", 0.0),
                                            f.optDouble("price", 0.0),
                                            f.optString("category", "extra"),
                                            key
                                    ));
                                }
                            }
                            // Refresh the existing adapters with new data
                            refreshAdapters();
                        }
                    } catch (Exception e) {
                        Log.e("JSON_ERROR", "Parsing error: " + e.getMessage());
                        Toast.makeText(this, "Error loading menu", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("VOLLEY_ERROR", error.toString());
                    Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show();
                });

        Volley.newRequestQueue(this).add(request);
    }

    private void refreshAdapters() {
        // Update the data inside the adapters and notify changes
        adapterBase.updateData(filter("base"));
        adapterProtein.updateData(filter("protein"));
        adapterVeggie.updateData(filter("veggie"));
        adapterExtra.updateData(filter("extra"));
    }

    private List<FoodItem> filter(String cat) {
        List<FoodItem> filtered = new ArrayList<>();
        for (FoodItem f : masterFoodList) {
            // Case-insensitive check
            if (f.getCategory() != null && f.getCategory().equalsIgnoreCase(cat)) {
                filtered.add(f);
            }
        }
        return filtered;
    }

    private void updateSelection(String category, Map<String, Integer> quantities) {
        categorySelectionMap.put(category, quantities);

        int totalCals = 0;
        double totalPro = 0, totalCarb = 0, totalFat = 0, totalPrice = 0;
        StringBuilder detailedSummary = new StringBuilder();

        for (Map.Entry<String, Map<String, Integer>> entry : categorySelectionMap.entrySet()) {
            Map<String, Integer> itemsInCat = entry.getValue();

            for (FoodItem food : masterFoodList) {
                if (itemsInCat.containsKey(food.getName())) {
                    int qty = itemsInCat.get(food.getName());
                    if (qty > 0) {
                        totalCals += (food.getCal() * qty);
                        totalPro += (food.getPro() * qty);
                        totalCarb += (food.getCarb() * qty);
                        totalFat += (food.getFat() * qty);
                        totalPrice += (food.getPrice() * qty);
                        detailedSummary.append("• ").append(food.getName())
                                .append(" (x").append(qty).append(")\n");
                    }
                }
            }
        }

        // Update UI
        tvLiveTotal.setText(totalCals + " kcal");
        tvTotalPrice.setText(String.format("Total Price: $%.2f", totalPrice));
        tvTotalPro.setText(String.format("P: %.1fg", totalPro));
        tvTotalCarb.setText(String.format("C: %.1fg", totalCarb));
        tvTotalFat.setText(String.format("F: %.1fg", totalFat));

        String finalDetails = detailedSummary.toString().trim();
        tvDetailedList.setText(finalDetails.isEmpty() ? "No items selected" : finalDetails);
    }

    private void submitOrder() {
        String items = tvDetailedList.getText().toString();
        // Check if items list is empty OR if it contains the placeholder text
        if (items.isEmpty() || items.equals("No items selected")) {
            Toast.makeText(this, "Please select items first!", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(this, CheckoutActivity.class);
            intent.putExtra("order_items", items);
            intent.putExtra("total_price", tvTotalPrice.getText().toString());
            intent.putExtra("total_cals", tvLiveTotal.getText().toString());
            intent.putExtra("total_pro", tvTotalPro.getText().toString());
            intent.putExtra("total_carb", tvTotalCarb.getText().toString());
            intent.putExtra("total_fat", tvTotalFat.getText().toString());
            startActivity(intent);
        }
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_build);
        bottomNav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, LandingActivity.class));
                finish();
                return true;
            }
            if (id == R.id.nav_build) {
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