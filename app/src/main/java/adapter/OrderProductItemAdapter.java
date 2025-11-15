package adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.company.doafacil.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.List;

import helper.ConfigurationFirebase;
import model.OrderItem;
import model.StockItem;

// REBUILD: This adapter now displays OrderItems and performs a lookup to get the expiration date from the StockItem.
public class OrderProductItemAdapter extends RecyclerView.Adapter<OrderProductItemAdapter.ItemViewHolder> {

    private final List<OrderItem> items;
    private final Context context;

    public OrderProductItemAdapter(List<OrderItem> items, Context context) {
        this.items = items;
        this.context = context;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.adapter_ong_order, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        OrderItem item = items.get(position);

        // Set data that is already available in the OrderItem
        holder.itemName.setText(item.getProductName());

        DecimalFormat formatter = new DecimalFormat("0.##");
        String formattedQuantity = formatter.format(item.getOrderItemQuantity());
        String unit = item.getProductUnitType() != null ? item.getProductUnitType() : "";
        holder.itemQuantity.setText("Qtd: " + formattedQuantity + " " + unit.trim());

        // Perform a lookup to get the expiration date from the original StockItem
        holder.itemExpiration.setText("Validade: Carregando...");
        if (item.getStockItemId() != null && !item.getStockItemId().isEmpty()) {
            DatabaseReference stockItemRef = ConfigurationFirebase.getFirebaseDatabase()
                    .child("stock_items")
                    .child(item.getStockItemId());

            stockItemRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        StockItem stockItem = snapshot.getValue(StockItem.class);
                        if (stockItem != null) {
                            holder.itemExpiration.setText("Validade: " + stockItem.getStockItemExpirationDate());
                        }
                    } else {
                        holder.itemExpiration.setText("Validade: N/A");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    holder.itemExpiration.setText("Validade: Erro");
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, itemQuantity, itemExpiration;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.text_order_item_name);
            itemQuantity = itemView.findViewById(R.id.text_order_item_quantity);
            itemExpiration = itemView.findViewById(R.id.text_order_item_expiration);
        }
    }
}
