package model;

// REBUILD: Moving establishmentId to the Product section for better logical grouping.
public class ItemDisplay {

    // --- From StockItem ---
    private String stockItemId;
    private double stockItemQuantity;
    private String stockItemExpirationDate;
    private String stockItemStatus;
    private String stockItemPickupDate;
    private String stockItemPickupTime;

    // --- From Product ---
    private String productId;
    private String establishmentId; // Moved from StockItem section
    private String productName;
    private String productUnitType;
    private String establishmentName;
    private String establishmentAddress;

    public ItemDisplay() {
    }

    // --- Getters and Setters ---

    public String getStockItemId() {
        return stockItemId;
    }

    public void setStockItemId(String stockItemId) {
        this.stockItemId = stockItemId;
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

    public String getEstablishmentName() {
        return establishmentName;
    }

    public void setEstablishmentName(String establishmentName) {
        this.establishmentName = establishmentName;
    }

    public String getEstablishmentAddress() {
        return establishmentAddress;
    }

    public void setEstablishmentAddress(String establishmentAddress) {
        this.establishmentAddress = establishmentAddress;
    }
}
