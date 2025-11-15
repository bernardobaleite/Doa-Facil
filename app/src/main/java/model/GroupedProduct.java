package model;

import java.util.List;

// REBUILD: Recreating the file I previously deleted. My apologies.
// This is now a generic model to group any type of item, as per the user's superior architecture.
public class GroupedProduct<T> {

    private String productName;
    private List<T> items;

    public GroupedProduct(String productName, List<T> items) {
        this.productName = productName;
        this.items = items;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }
}
