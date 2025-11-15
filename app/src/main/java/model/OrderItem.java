package model;

// ARCHITECTURE: Implementing the user's superior data model.
public class OrderItem {

    private String productId;
    private String productName;
    private String productUnitType;
    private double orderItemQuantity;
    private String stockItemId; // Optional: specific stock item to fulfill from

    public OrderItem() {
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

    public double getOrderItemQuantity() {
        return orderItemQuantity;
    }

    public void setOrderItemQuantity(double orderItemQuantity) {
        this.orderItemQuantity = orderItemQuantity;
    }

    public String getStockItemId() {
        return stockItemId;
    }

    public void setStockItemId(String stockItemId) {
        this.stockItemId = stockItemId;
    }
}
