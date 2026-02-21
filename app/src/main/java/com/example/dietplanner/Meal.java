package com.example.dietplanner;

public class Meal {
    private String id;
    private String items;
    private int calories;
    private String price;
    private int protein, fat, carbs;

    public Meal(String id, String items, int calories, String price, int protein, int fat, int carbs) {
        this.id = id;
        this.items = items;
        this.calories = calories;
        this.price = price;
        this.protein = protein;
        this.fat = fat;
        this.carbs = carbs;
    }

    public String getId() { return id; }
    public String getItems() { return items; }
    public int getCalories() { return calories; }
    public String getPrice() { return price; }
    public int getProtein() { return protein; }
    public int getFat() { return fat; }
    public int getCarb() { return carbs; }
}