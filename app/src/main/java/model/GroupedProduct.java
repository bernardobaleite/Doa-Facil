package model;

import java.io.Serializable;
import java.util.List;

// REFACTOR: Removing isExpanded to handle state correctly and definitively inside the adapter.
public class GroupedProduct implements Serializable {

    private String productName;
    private List<ItemDisplay> items;

    // The isExpanded field and its getter/setter have been removed to prevent state conflicts.

    public GroupedProduct(String productName, List<ItemDisplay> items) {
        this.productName = productName;
        this.items = items;
    }

    public GroupedProduct() {}

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public List<ItemDisplay> getItems() {
        return items;
    }

    public void setItems(List<ItemDisplay> items) {
        this.items = items;
    }
}
