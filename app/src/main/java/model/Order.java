package model;

import java.util.Map;

// ARCHITECTURE: Aligning Order with the user's superior data model.
public class Order {

    private String orderId;
    private String ongId;
    private String orderStatus;
    private long orderCreatedAt;
    private String orderScheduledDateTime;
    private Map<String, OrderItem> orderItems;

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

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public long getOrderCreatedAt() {
        return orderCreatedAt;
    }

    public void setOrderCreatedAt(long orderCreatedAt) {
        this.orderCreatedAt = orderCreatedAt;
    }

    public String getOrderScheduledDateTime() {
        return orderScheduledDateTime;
    }

    public void setOrderScheduledDateTime(String orderScheduledDateTime) {
        this.orderScheduledDateTime = orderScheduledDateTime;
    }

    public Map<String, OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(Map<String, OrderItem> orderItems) {
        this.orderItems = orderItems;
    }
}
