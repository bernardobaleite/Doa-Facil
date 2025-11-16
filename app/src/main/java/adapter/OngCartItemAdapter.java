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

import fragment.OngCartFragment.UnifiedCartItem;
import model.OrderItem;

// RE-ARCH: Aligning the nested adapter with the new UnifiedCartItem ViewModel.
public class OngCartItemAdapter extends RecyclerView.Adapter<OngCartItemAdapter.ItemViewHolder> {

    private final Context context;
    private final List<UnifiedCartItem> items;
    private final OnItemRemoveListener listener;

    // The listener now passes up a list of original items to be removed.
    public interface OnItemRemoveListener {
        void onRemoveClicked(List<OrderItem> items);
    }

    public OngCartItemAdapter(Context context, List<UnifiedCartItem> items, OnItemRemoveListener listener) {
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
        UnifiedCartItem unifiedItem = items.get(position);

        DecimalFormat formatter = new DecimalFormat("0.##");
        String formattedQuantity = formatter.format(unifiedItem.totalQuantity);
        
        String quantityText = "Qtd: " + formattedQuantity + " " + unifiedItem.unitType;

        holder.expirationDate.setText("Validade: " + unifiedItem.expirationDate);
        holder.quantity.setText(quantityText.trim());

        holder.actionButton.setImageResource(R.drawable.ic_cancel_24dp);
        holder.actionButton.setColorFilter(ContextCompat.getColor(context, R.color.status_red));
        holder.actionButton.setOnClickListener(v -> {
            if (listener != null) {
                // When a unified item is removed, all its underlying original items must be removed.
                listener.onRemoveClicked(unifiedItem.originalOrderItems);
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
            actionButton = itemView.findViewById(R.id.button_add_individual_to_cart);
        }
    }
}
