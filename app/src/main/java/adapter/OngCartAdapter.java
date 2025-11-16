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
import com.google.firebase.database.FirebaseDatabase;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fragment.OngCartFragment.UnifiedCartItem;
import helper.ConfigurationFirebase;
import helper.UserFirebase;
import model.GroupedProduct;
import model.OrderItem;

// RE-ARCH: Aligning the adapter with the new UnifiedCartItem ViewModel.
public class OngCartAdapter extends RecyclerView.Adapter<OngCartAdapter.MyViewHolder> {

    private List<GroupedProduct<UnifiedCartItem>> productList;
    private final Context context;
    private final List<Integer> placeholderImages;

    public OngCartAdapter(Context context, List<GroupedProduct<UnifiedCartItem>> productList) {
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

    public void setData(List<GroupedProduct<UnifiedCartItem>> productList) {
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
        GroupedProduct<UnifiedCartItem> group = productList.get(position);

        holder.productName.setText(group.getProductName());

        double totalQuantityInCart = 0;
        String unitType = "";
        if (group.getItems() != null && !group.getItems().isEmpty()) {
            UnifiedCartItem firstItem = group.getItems().get(0);
            unitType = firstItem.unitType;
            for (UnifiedCartItem item : group.getItems()) {
                totalQuantityInCart += item.totalQuantity;
            }
        }
        DecimalFormat formatter = new DecimalFormat("0.##");
        String summaryText = "Você pediu " + formatter.format(totalQuantityInCart) + " " + unitType.trim();
        holder.productSummary.setText(summaryText);

        int imageIndex = Math.abs(group.getProductName().hashCode()) % placeholderImages.size();
        holder.productImage.setImageDrawable(ContextCompat.getDrawable(context, placeholderImages.get(imageIndex)));

        OngCartItemAdapter itemAdapter = new OngCartItemAdapter(context, group.getItems(), this::removeItemsFromCart);
        holder.itemsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        holder.itemsRecyclerView.setAdapter(itemAdapter);
        holder.itemsRecyclerView.setNestedScrollingEnabled(false);
    }
    
    private void removeItemsFromCart(List<OrderItem> itemsToRemove) {
        String ongId = UserFirebase.getIdUser();
        if (ongId == null) return;
        
        DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("shopping_carts").child(ongId);
        Map<String, Object> updates = new HashMap<>();
        for (OrderItem item : itemsToRemove) {
            updates.put(item.getStockItemId(), null);
        }
        cartRef.updateChildren(updates);
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
