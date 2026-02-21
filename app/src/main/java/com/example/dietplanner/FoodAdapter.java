package com.example.dietplanner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.FoodViewHolder> {

    private Context context;
    private List<FoodItem> foodList;
    private OnQuantityChangeListener listener;
    // Tracks quantities locally for this specific category
    private Map<String, Integer> itemQuantities = new HashMap<>();

    public interface OnQuantityChangeListener {
        void onQuantityChanged(Map<String, Integer> updatedQuantities);
    }

    public FoodAdapter(Context context, List<FoodItem> foodList, OnQuantityChangeListener listener) {
        this.context = context;
        this.foodList = foodList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_food, parent, false);
        return new FoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        FoodItem item = foodList.get(position);

        // Basic Info
        holder.tvName.setText(item.getName());
        holder.tvCal.setText(item.getCal() + " kcal");
        holder.tvPrice.setText(String.format("$%.2f", item.getPrice()));

        // Macro breakdown
        holder.tvPro.setText("P: " + item.getPro() + "g");
        holder.tvCarb.setText("C: " + item.getCarb() + "g");
        holder.tvFat.setText("F: " + item.getFat() + "g");

        // Set Image (Assuming the image name matches the key in drawable)
        int resId = context.getResources().getIdentifier(item.getImage(), "drawable", context.getPackageName());
        if (resId != 0) {
            holder.ivImage.setImageResource(resId);
        } else {
            holder.ivImage.setImageResource(R.drawable.placeholder_food); // Fallback image
        }

        // Handle Quantity logic
        int currentQty = itemQuantities.getOrDefault(item.getName(), 0);
        holder.tvQuantity.setText(String.valueOf(currentQty));

        holder.btnPlus.setOnClickListener(v -> {
            int newQty = itemQuantities.getOrDefault(item.getName(), 0) + 1;
            itemQuantities.put(item.getName(), newQty);
            holder.tvQuantity.setText(String.valueOf(newQty));
            listener.onQuantityChanged(itemQuantities);
        });

        holder.btnMinus.setOnClickListener(v -> {
            int current = itemQuantities.getOrDefault(item.getName(), 0);
            if (current > 0) {
                int newQty = current - 1;
                itemQuantities.put(item.getName(), newQty);
                holder.tvQuantity.setText(String.valueOf(newQty));
                listener.onQuantityChanged(itemQuantities);
            }
        });
    }

    @Override
    public int getItemCount() {
        return foodList.size();
    }

    // Inside FoodAdapter.java
    public void updateData(List<FoodItem> newItems) {
        this.foodList = newItems;
        notifyDataSetChanged();
    }

    public static class FoodViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvCal, tvPrice, tvPro, tvCarb, tvFat, tvQuantity;
        ImageButton btnPlus, btnMinus;

        public FoodViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivFoodImage);
            tvName = itemView.findViewById(R.id.tvFoodName);
            tvCal = itemView.findViewById(R.id.tvFoodCal);
            tvPrice = itemView.findViewById(R.id.tvFoodPrice);
            tvPro = itemView.findViewById(R.id.tvPro);
            tvCarb = itemView.findViewById(R.id.tvCarb);
            tvFat = itemView.findViewById(R.id.tvFat);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnMinus = itemView.findViewById(R.id.btnMinus);
        }
    }
}