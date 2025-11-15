package adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.company.doafacil.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DatabaseReference;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fragment.OngCartFragment.CartDisplayItem;
import helper.ConfigurationFirebase;
import helper.UserFirebase;
import model.GroupedProduct;
import model.OrderItem;

// RE-ARCH: Removing the faulty expandable logic that caused the compile error.
public class OngCartAdapter extends RecyclerView.Adapter<OngCartAdapter.MyViewHolder> {

    private List<GroupedProduct<CartDisplayItem>> productList;
    private final Context context;
    private final List<Integer> placeholderImages;

    public OngCartAdapter(Context context, List<GroupedProduct<CartDisplayItem>> productList) {
        this.context = context;
        this.productList = productList != null ? productList : new ArrayList<>();
        this.placeholderImages = Arrays.asList(
                R.drawable.food_placeholder,
                R.drawable.food_placeholder2,
                R.drawable.food_placeholder3
        );
        setHasStableIds(true);
    }
    
    @Override
    public long getItemId(int position) {
        return productList.get(position).getProductName().hashCode();
    }

    public void setData(List<GroupedProduct<CartDisplayItem>> productList) {
        this.productList = productList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_ong_cart_list_item, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        GroupedProduct<CartDisplayItem> group = productList.get(position);

        holder.productName.setText(group.getProductName());

        double totalQuantityInCart = 0;
        String unitType = "";
        if (group.getItems() != null && !group.getItems().isEmpty()) {
            CartDisplayItem firstItem = group.getItems().get(0);
            if (firstItem.productDetails != null) {
                unitType = firstItem.productDetails.getProductUnitType();
            }
            if (unitType == null) unitType = "";
            for (CartDisplayItem item : group.getItems()) {
                totalQuantityInCart += item.orderItem.getOrderItemQuantity();
            }
        }
        DecimalFormat formatter = new DecimalFormat("0.##");
        String summaryText = "Você pediu " + formatter.format(totalQuantityInCart) + " " + unitType.trim();
        holder.productSummary.setText(summaryText);

        int imageIndex = Math.abs(group.getProductName().hashCode()) % placeholderImages.size();
        holder.productImage.setImageDrawable(ContextCompat.getDrawable(context, placeholderImages.get(imageIndex)));

        // The nested RecyclerView is now always visible, as per the layout file.
        OngCartItemAdapter itemAdapter = new OngCartItemAdapter(context, group.getItems(), this::removeItemFromCart);
        holder.itemsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        holder.itemsRecyclerView.setAdapter(itemAdapter);
        holder.itemsRecyclerView.setNestedScrollingEnabled(false);
    }
    
    private void removeItemFromCart(OrderItem orderItem) {
        String ongId = UserFirebase.getIdUser();
        if (ongId == null) return;
        
        DatabaseReference cartItemRef = ConfigurationFirebase.getFirebaseDatabase()
                .child("shopping_carts")
                .child(ongId)
                .child(orderItem.getStockItemId());
        
        cartItemRef.removeValue();
        // The fragment's listener will handle the UI update.
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productSummary;
        ShapeableImageView productImage;
        RecyclerView itemsRecyclerView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.product_name);
            productSummary = itemView.findViewById(R.id.product_summary);
            productImage = itemView.findViewById(R.id.product_image);
            itemsRecyclerView = itemView.findViewById(R.id.recycler_cart_items_nested);
        }
    }
}
