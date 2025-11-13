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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import model.GroupedProduct;
import model.ItemDisplay;

// RE-FIX: Correcting the renamed view ID to prevent the crash.
public class OngCartAdapter extends RecyclerView.Adapter<OngCartAdapter.MyViewHolder> {

    private List<GroupedProduct> productList;
    private final Context context;
    private final List<Integer> placeholderImages;

    public OngCartAdapter(Context context, List<GroupedProduct> productList) {
        this.context = context;
        this.productList = productList != null ? productList : new ArrayList<>();
        this.placeholderImages = Arrays.asList(
                R.drawable.food_placeholder,
                R.drawable.food_placeholder2,
                R.drawable.food_placeholder3
        );
    }

    public void setData(List<GroupedProduct> productList) {
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
        GroupedProduct group = productList.get(position);

        holder.productName.setText(group.getProductName());

        double totalQuantityInCart = 0;
        String unitType = "";
        if (group.getItems() != null && !group.getItems().isEmpty()) {
            unitType = group.getItems().get(0).getProductUnitType();
            if (unitType == null) unitType = "";
            for (ItemDisplay item : group.getItems()) {
                totalQuantityInCart += item.getStockItemQuantity();
            }
        }
        DecimalFormat formatter = new DecimalFormat("0.##");
        String summaryText = "Você tem " + formatter.format(totalQuantityInCart) + " " + unitType.trim() + " no carrinho";
        
        // THE FIX: Use the correct TextView variable.
        holder.productSummary.setText(summaryText);

        int imageIndex = Math.abs(group.getProductName().hashCode()) % placeholderImages.size();
        int deterministicImageId = placeholderImages.get(imageIndex);
        holder.productImage.setImageDrawable(ContextCompat.getDrawable(context, deterministicImageId));

        holder.itemsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        OngCartItemAdapter itemAdapter = new OngCartItemAdapter(context, group.getItems());
        holder.itemsRecyclerView.setAdapter(itemAdapter);
        holder.itemsRecyclerView.setNestedScrollingEnabled(false);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        // THE FIX: Rename variable to match its purpose.
        TextView productName, productSummary;
        ShapeableImageView productImage;
        RecyclerView itemsRecyclerView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.product_name);
            // THE FIX: Use the new, correct ID from the layout file.
            productSummary = itemView.findViewById(R.id.product_summary);
            productImage = itemView.findViewById(R.id.product_image);
            itemsRecyclerView = itemView.findViewById(R.id.recycler_cart_items_nested);
        }
    }
}
