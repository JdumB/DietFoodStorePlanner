package com.example.dietplanner;

public class FoodItem {
    private String id;
    private String name;
    private int cal;
    private double pro;
    private double fat;
    private double carb;
    private double price;
    private String category;
    private String image; // 1. Add this field

    // 2. Update the Constructor to include image
    public FoodItem(String id, String name, int cal, double pro, double fat, double carb, double price, String category, String image) {
        this.id = id;
        this.name = name;
        this.cal = cal;
        this.pro = pro;
        this.fat = fat;
        this.carb = carb;
        this.price = price;
        this.category = category;
        this.image = image;
    }

    // 3. Add the missing Getter method
    public String getImage() {
        return image;
    }

    // Keep your other existing getters below...
    public String getName() { return name; }
    public int getCal() { return cal; }
    public double getPro() { return pro; }
    public double getFat() { return fat; }
    public double getCarb() { return carb; }
    public double getPrice() { return price; }
    public String getCategory() { return category; }
}