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
import java.util.ArrayList;
import java.util.List;

import model.ItemDisplay;

// RE-ARCH: Adding the expiration date to the sub-item view.
public class OrderProductItemAdapter extends RecyclerView.Adapter<OrderProductItemAdapter.ItemViewHolder> {

    private final List<ItemDisplay> items;
    private final Context context;

    public OrderProductItemAdapter(List<ItemDisplay> items, Context context) {
        this.items = items;
        this.context = context;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.adapter_order_product_item, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        ItemDisplay item = items.get(position);

        holder.itemName.setText(item.getProductName());
        
        // THE FIX: Set the expiration date text.
        holder.itemExpiration.setText("Validade: " + item.getStockItemExpirationDate());

        DecimalFormat formatter = new DecimalFormat("0.##");
        String formattedQuantity = formatter.format(item.getStockItemQuantity());
        String unit = item.getProductUnitType() != null ? item.getProductUnitType() : "";
        holder.itemQuantity.setText("Qtd: " + formattedQuantity + " " + unit.trim());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, itemQuantity, itemExpiration; // THE FIX

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.text_order_item_name);
            itemQuantity = itemView.findViewById(R.id.text_order_item_quantity);
            itemExpiration = itemView.findViewById(R.id.text_order_item_expiration); // THE FIX
        }
    }
}
