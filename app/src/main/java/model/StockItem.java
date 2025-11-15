package model;

import com.google.firebase.database.DatabaseReference;
import helper.ConfigurationFirebase;

// ARCHITECTURE: Aligning StockItem with the user's superior data model.
public class StockItem {

    private String stockItemId;
    private String productId;
    private double stockItemQuantity;
    private String stockItemExpirationDate;
    private String stockItemStatus;
    private long stockItemCreatedAt;
    private String donationId; // Optional: Originating donation

    public StockItem() {
    }

    public void save() {
        DatabaseReference firebaseRef = ConfigurationFirebase.getFirebaseDatabase();
        DatabaseReference stockItemRef = firebaseRef
                .child("stock_items")
                .child(this.getStockItemId());
        stockItemRef.setValue(this);
    }

    // --- Getters and Setters ---

    public String getStockItemId() {
        return stockItemId;
    }

    public void setStockItemId(String stockItemId) {
        this.stockItemId = stockItemId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public double getStockItemQuantity() {
        return stockItemQuantity;
    }

    public void setStockItemQuantity(double stockItemQuantity) {
        this.stockItemQuantity = stockItemQuantity;
    }

    public String getStockItemExpirationDate() {
        return stockItemExpirationDate;
    }

    public void setStockItemExpirationDate(String stockItemExpirationDate) {
        this.stockItemExpirationDate = stockItemExpirationDate;
    }

    public String getStockItemStatus() {
        return stockItemStatus;
    }

    public void setStockItemStatus(String stockItemStatus) {
        this.stockItemStatus = stockItemStatus;
    }

    public long getStockItemCreatedAt() {
        return stockItemCreatedAt;
    }

    public void setStockItemCreatedAt(long stockItemCreatedAt) {
        this.stockItemCreatedAt = stockItemCreatedAt;
    }

    public String getDonationId() {
        return donationId;
    }

    public void setDonationId(String donationId) {
        this.donationId = donationId;
    }
}
