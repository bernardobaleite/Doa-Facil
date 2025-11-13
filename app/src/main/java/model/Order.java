package model;

import java.util.List;
import java.util.Map;

// RE-ARCH: Using a Map for items to ensure correct Firebase serialization.
public class  Order {

    private String orderId;
    private String ongId;
    private String status;
    private long timestamp;
    private Map<String, ItemDisplay> items; // THE FIX
    private String scheduledDate;
    private String scheduledTime;

    public Order() {
    }

    // --- Getters and Setters ---

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getOngId() {
        return ongId;
    }

    public void setOngId(String ongId) {
        this.ongId = ongId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, ItemDisplay> getItems() {
        return items;
    }

    public void setItems(Map<String, ItemDisplay> items) {
        this.items = items;
    }

    public String getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(String scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public String getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(String scheduledTime) {
        this.scheduledTime = scheduledTime;
    }
}
