package adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.company.doafacil.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fragment.OngListProductsFragment.AggregatedStockItem;
import fragment.OngListProductsFragment.GroupedStockItem;
import model.OrderItem;
import model.StockItem;

// RE-ARCH: Implementing the quantity distribution logic when adding to cart.
public class OngProductAdapter extends RecyclerView.Adapter<OngProductAdapter.MyViewHolder> {

    private List<GroupedStockItem> productList;
    private Context context;
    private final ProductInteractionListener listener;
    private int expandedPosition = -1;

    private final List<Integer> placeholderImages;

    // THE FIX: The listener now passes up a list of items to be added.
    public interface ProductInteractionListener {
        void onAddToCart(List<OrderItem> items);
    }

    public OngProductAdapter(Context context, ProductInteractionListener listener) {
        this.productList = new ArrayList<>();
        this.context = context;
        this.listener = listener;
        setHasStableIds(true);

        placeholderImages = Arrays.asList(R.drawable.food_placeholder, R.drawable.food_placeholder2, R.drawable.food_placeholder3);
    }

    @Override
    public long getItemId(int position) {
        if (position >= 0 && position < productList.size()) {
            return productList.get(position).getProductName().hashCode();
        }
        return RecyclerView.NO_ID;
    }

    public void setData(List<GroupedStockItem> productList) {
        this.productList = productList;
        expandedPosition = -1;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_ong_product_list_item, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        GroupedStockItem groupedProduct = productList.get(position);
        final boolean isExpanded = position == expandedPosition;

        holder.productName.setText(groupedProduct.getProductName());

        double totalQuantity = 0;
        for (AggregatedStockItem item : groupedProduct.getItems()) {
            totalQuantity += item.getTotalQuantity();
        }
        DecimalFormat formatter = new DecimalFormat("0.##");
        String summaryText = "Disponível no total: " + formatter.format(totalQuantity) + " " + groupedProduct.getProductUnitType().trim();
        
        holder.productSummary.setText(summaryText);

        int imageIndex = Math.abs(groupedProduct.getProductName().hashCode()) % placeholderImages.size();
        holder.productImage.setImageDrawable(ContextCompat.getDrawable(context, placeholderImages.get(imageIndex)));

        holder.expandableLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        OngProductItemAdapter subAdapter = new OngProductItemAdapter(groupedProduct.getItems(), item -> showQuantityDialog(item, holder.getBindingAdapterPosition()));
        holder.originsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        holder.originsRecyclerView.setAdapter(subAdapter);
        holder.originsRecyclerView.setNestedScrollingEnabled(false);

        holder.itemView.setOnClickListener(v -> {
            int clickedPosition = holder.getBindingAdapterPosition();
            if (clickedPosition == RecyclerView.NO_POSITION) return;

            int previousExpanded = expandedPosition;
            expandedPosition = isExpanded ? -1 : clickedPosition;

            if (previousExpanded != -1) notifyItemChanged(previousExpanded);
            notifyItemChanged(clickedPosition);
        });
    }

    private void showQuantityDialog(AggregatedStockItem aggregatedItem, int adapterPosition) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_quantity, null);
        builder.setView(dialogView);

        final EditText editTextQuantity = dialogView.findViewById(R.id.edit_text_dialog_quantity);
        Button buttonCancel = dialogView.findViewById(R.id.button_dialog_cancel);
        Button buttonConfirm = dialogView.findViewById(R.id.button_dialog_confirm);

        final AlertDialog dialog = builder.create();

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        buttonConfirm.setOnClickListener(v -> {
            String quantityStr = editTextQuantity.getText().toString();
            if (quantityStr.isEmpty()) {
                Toast.makeText(context, "Por favor, insira uma quantidade.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                double requestedQuantity = Double.parseDouble(quantityStr);
                if (requestedQuantity <= 0 || requestedQuantity > aggregatedItem.getTotalQuantity()) {
                     Toast.makeText(context, "Quantidade inválida ou indisponível.", Toast.LENGTH_SHORT).show();
                     return;
                }
                
                // THE FIX: Distribute the requested quantity across the original stock items.
                if (listener != null) {
                    List<OrderItem> itemsToAddToCart = new ArrayList<>();
                    double remainingQuantityToFulfill = requestedQuantity;

                    for (StockItem originalItem : aggregatedItem.getOriginalItems()) {
                        if (remainingQuantityToFulfill <= 0) break;

                        double quantityToTake = Math.min(originalItem.getStockItemQuantity(), remainingQuantityToFulfill);

                        OrderItem orderItem = new OrderItem();
                        orderItem.setProductId(originalItem.getProductId());
                        orderItem.setStockItemId(originalItem.getStockItemId());
                        orderItem.setOrderItemQuantity(quantityToTake);
                        itemsToAddToCart.add(orderItem);

                        remainingQuantityToFulfill -= quantityToTake;
                    }
                    listener.onAddToCart(itemsToAddToCart);
                }

                dialog.dismiss();

            } catch (NumberFormatException e) {
                Toast.makeText(context, "Quantidade inválida.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, productSummary;
        RecyclerView originsRecyclerView;
        LinearLayout expandableLayout;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            productName = itemView.findViewById(R.id.product_name);
            productSummary = itemView.findViewById(R.id.product_summary);
            originsRecyclerView = itemView.findViewById(R.id.origins_recycler_view);
            expandableLayout = itemView.findViewById(R.id.expandable_layout);
        }
    }
}
