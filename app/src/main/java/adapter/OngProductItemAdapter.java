package adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.company.doafacil.R;

import java.text.DecimalFormat;
import java.util.List;

import fragment.OngListProductsFragment.AggregatedStockItem;

// RE-ARCH: This adapter now displays the aggregated stock items.
public class OngProductItemAdapter extends RecyclerView.Adapter<OngProductItemAdapter.OriginViewHolder> {

    // THE FIX: The adapter now works with the AggregatedStockItem ViewModel.
    private final List<AggregatedStockItem> items;
    private final OnOriginClickedListener listener;

    public interface OnOriginClickedListener {
        void onOriginClicked(AggregatedStockItem item);
    }

    public OngProductItemAdapter(List<AggregatedStockItem> items, OnOriginClickedListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OriginViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_ong_product_individual_item, parent, false);
        return new OriginViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OriginViewHolder holder, int position) {
        AggregatedStockItem item = items.get(position);
        Context context = holder.itemView.getContext();

        DecimalFormat formatter = new DecimalFormat("0.##");
        // THE FIX: Display the total aggregated quantity.
        String formattedQuantity = formatter.format(item.getTotalQuantity());
        String quantityText = "Qtd: " + formattedQuantity;
        
        holder.quantity.setText(quantityText.trim());
        // THE FIX: Display the expiration date for the aggregated group.
        holder.expiration.setText(context.getString(R.string.expiration_format, item.getExpirationDate()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOriginClicked(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class OriginViewHolder extends RecyclerView.ViewHolder {
        TextView quantity, expiration;

        OriginViewHolder(View view) {
            super(view);
            quantity = view.findViewById(R.id.origin_quantity);
            expiration = view.findViewById(R.id.origin_expiration);
        }
    }
}
