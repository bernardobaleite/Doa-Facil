package model;

// ARCHITECTURE: Implementing the user's superior data model.
public class ReceivedDonationItem {

    private String productId;
    private String productName;
    private String productUnitType;
    private double receivedQuantity;
    private String receivedExpirationDate; // THE FIX: Add the missing field

    public ReceivedDonationItem() {
    }

    // --- Getters and Setters ---

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
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

    public double getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(double receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    public String getReceivedExpirationDate() {
        return receivedExpirationDate;
    }

    public void setReceivedExpirationDate(String receivedExpirationDate) {
        this.receivedExpirationDate = receivedExpirationDate;
    }
}
