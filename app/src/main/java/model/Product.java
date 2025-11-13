package model;

import com.google.firebase.database.DatabaseReference;

import helper.ConfigurationFirebase;

// REBUILD: Adding establishmentId to the Product model.
public class Product {

    private String productId;
    private String establishmentId; // Owner of the product
    private String productName;
    private String productUnitType;

    public Product() {
    }

    public void save() {
        DatabaseReference firebaseRef = ConfigurationFirebase.getFirebaseDatabase();
        DatabaseReference productRef = firebaseRef
                .child("establishment_product_catalog")
                .child(this.getProductId());
        productRef.setValue(this);
    }

    // --- Getters and Setters ---

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getEstablishmentId() {
        return establishmentId;
    }

    public void setEstablishmentId(String establishmentId) {
        this.establishmentId = establishmentId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductUnitType() {
        return productUnitType;
    }

    public void setProductUnitType(String productUnitType) {
        this.productUnitType = productUnitType;
    }
}
