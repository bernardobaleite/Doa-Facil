package adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.company.doafacil.R;

import java.text.DecimalFormat;
import java.util.List;

import fragment.OngCartFragment.CartDisplayItem;
import model.OrderItem;

// RE-ARCH: Fixing the final compile error by using the correct view ID.
public class OngCartItemAdapter extends RecyclerView.Adapter<OngCartItemAdapter.ItemViewHolder> {

    private final Context context;
    private final List<CartDisplayItem> items;
    private final OnItemRemoveListener listener;

    public interface OnItemRemoveListener {
        void onRemoveClicked(OrderItem item);
    }

    public OngCartItemAdapter(Context context, List<CartDisplayItem> items, OnItemRemoveListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.adapter_ong_cart_individual_item, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        CartDisplayItem displayItem = items.get(position);

        DecimalFormat formatter = new DecimalFormat("0.##");
        String formattedQuantity = formatter.format(displayItem.orderItem.getOrderItemQuantity());
        
        String unitType = "";
        if(displayItem.productDetails != null){
            unitType = displayItem.productDetails.getProductUnitType();
        }

        String quantityText = "Qtd: " + formattedQuantity + " " + unitType;

        holder.expirationDate.setText("Validade: " + displayItem.stockItem.getStockItemExpirationDate());
        holder.quantity.setText(quantityText.trim());

        holder.actionButton.setImageResource(R.drawable.ic_cancel_24dp);
        holder.actionButton.setColorFilter(ContextCompat.getColor(context, R.color.status_red));
        holder.actionButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveClicked(displayItem.orderItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView expirationDate, quantity;
        ImageButton actionButton;

        ItemViewHolder(View itemView) {
            super(itemView);
            expirationDate = itemView.findViewById(R.id.text_individual_expiration);
            quantity = itemView.findViewById(R.id.text_individual_quantity);
            // THE FIX: Use the correct ID from the layout file.
            actionButton = itemView.findViewById(R.id.button_add_individual_to_cart);
        }
    }
}
