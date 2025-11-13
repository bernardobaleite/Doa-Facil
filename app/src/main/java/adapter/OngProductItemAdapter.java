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

import model.ItemDisplay;

// RE-ARCH: Using the user's correctly renamed layout file.
public class OngProductItemAdapter extends RecyclerView.Adapter<OngProductItemAdapter.OriginViewHolder> {

    private final List<ItemDisplay> origins;
    private final OnOriginClickedListener listener;

    public interface OnOriginClickedListener {
        void onOriginClicked(ItemDisplay item);
    }

    public OngProductItemAdapter(List<ItemDisplay> origins, OnOriginClickedListener listener) {
        this.origins = origins;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OriginViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // THE FINAL FIX: Use the correct layout file for this item, as renamed by the user.
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_ong_product_individual_item, parent, false);
        return new OriginViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OriginViewHolder holder, int position) {
        ItemDisplay item = origins.get(position);
        Context context = holder.itemView.getContext();

        DecimalFormat formatter = new DecimalFormat("0.##");
        String formattedQuantity = formatter.format(item.getStockItemQuantity());
        String unitType = item.getProductUnitType() != null ? item.getProductUnitType() : "";
        String quantityText = "Qtd: " + formattedQuantity + " " + unitType;

        holder.quantity.setText(quantityText.trim());
        holder.expiration.setText(context.getString(R.string.expiration_format, item.getStockItemExpirationDate()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOriginClicked(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return origins.size();
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
