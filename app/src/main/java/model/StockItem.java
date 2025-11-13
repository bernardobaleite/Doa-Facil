package model;

import com.google.firebase.database.DatabaseReference;
import helper.ConfigurationFirebase;

// REBUILD: Undoing my mistake. Reverting StockItem to its correct, normalized state.
public class StockItem {

    //--- Keys
    private String stockItemId; // Primary Key for this entry
    private String establishmentId;      // Foreign Key to the user who owns this item
    private String productId;   // Foreign Key to the product in the catalog (e.g., "PROD001")

    //--- Data specific to this stock item
    private double stockItemQuantity;
    private String stockItemExpirationDate;
    private String stockItemStatus;
    private String stockItemPickupDate;
    private String stockItemPickupTime;

    public StockItem() {
        this.stockItemStatus = "Aguarde a coleta";
        DatabaseReference stockRef = ConfigurationFirebase.getFirebaseDatabase().child("stock_items");
        this.setStockItemId(stockRef.push().getKey());
    }

    public void save() {
        DatabaseReference firebaseRef = ConfigurationFirebase.getFirebaseDatabase();
        DatabaseReference stockItemRef = firebaseRef
                .child("stock_items")
                .child(this.getEstablishmentId())
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

    public String getEstablishmentId() {
        return establishmentId;
    }

    public void setEstablishmentId(String establishmentId) {
        this.establishmentId = establishmentId;
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

    public String getStockItemPickupDate() {
        return stockItemPickupDate;
    }

    public void setStockItemPickupDate(String stockItemPickupDate) {
        this.stockItemPickupDate = stockItemPickupDate;
    }

    public String getStockItemPickupTime() {
        return stockItemPickupTime;
    }

    public void setStockItemPickupTime(String stockItemPickupTime) {
        this.stockItemPickupTime = stockItemPickupTime;
    }
}
