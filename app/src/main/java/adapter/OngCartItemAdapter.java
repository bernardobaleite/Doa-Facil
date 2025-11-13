package adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.company.doafacil.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.text.DecimalFormat;
import java.util.List;

import helper.ConfigurationFirebase;
import helper.UserFirebase;
import model.ItemDisplay;
import model.StockItem;

// RE-LOGIC: Implementing a robust, foolproof transaction to correctly return stock quantity.
public class OngCartItemAdapter extends RecyclerView.Adapter<OngCartItemAdapter.ItemViewHolder> {

    private final Context context;
    private final List<ItemDisplay> items;
    private final String currentOngId;

    public OngCartItemAdapter(Context context, List<ItemDisplay> items) {
        this.context = context;
        this.items = items;
        this.currentOngId = UserFirebase.getIdUser();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.adapter_ong_cart_individual_item, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        ItemDisplay item = items.get(position);

        DecimalFormat formatter = new DecimalFormat("0.##");
        String formattedQuantity = formatter.format(item.getStockItemQuantity());
        String unitType = item.getProductUnitType() != null ? item.getProductUnitType() : "";
        String quantityText = "Qtd: " + formattedQuantity + " " + unitType;

        holder.expirationDate.setText("Validade: " + item.getStockItemExpirationDate());
        holder.quantity.setText(quantityText.trim());

        holder.actionButton.setImageResource(R.drawable.ic_cancel_24dp);
        holder.actionButton.setColorFilter(ContextCompat.getColor(context, R.color.status_red));
        holder.actionButton.setOnClickListener(v -> removeItemAndReturnStock(item));
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

    private void removeItemAndReturnStock(ItemDisplay cartItem) {
        if (cartItem.getEstablishmentId() == null || cartItem.getStockItemId() == null) {
            Toast.makeText(context, "Erro: Origem do item desconhecida.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference originalStockItemRef = ConfigurationFirebase.getFirebaseDatabase()
                .child("stock_items")
                .child(cartItem.getEstablishmentId())
                .child(cartItem.getStockItemId());

        originalStockItemRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                // THE FIX: A robust, single-path logic for returning stock.
                StockItem currentStock = mutableData.getValue(StockItem.class);
                double quantityToReturn = cartItem.getStockItemQuantity();
                double newTotalQuantity = quantityToReturn;

                if (currentStock != null) {
                    newTotalQuantity += currentStock.getStockItemQuantity();
                }

                // Always build a fresh object to guarantee data integrity.
                StockItem updatedStock = new StockItem();
                updatedStock.setStockItemId(cartItem.getStockItemId());
                updatedStock.setEstablishmentId(cartItem.getEstablishmentId());
                updatedStock.setProductId(cartItem.getProductId());
                updatedStock.setStockItemQuantity(newTotalQuantity);
                updatedStock.setStockItemExpirationDate(cartItem.getStockItemExpirationDate());
                updatedStock.setStockItemStatus("Doação Coletada");

                mutableData.setValue(updatedStock);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Toast.makeText(context, "Erro ao devolver item: " + error.getMessage(), Toast.LENGTH_LONG).show();
                } else if (committed) {
                    // Only remove from cart if the stock transaction was successful.
                    removeCartItemFromDatabase(cartItem);
                } else {
                     Toast.makeText(context, "Não foi possível devolver o item. Tente novamente.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void removeCartItemFromDatabase(ItemDisplay cartItem) {
        if (currentOngId == null) return;
        DatabaseReference cartItemRef = ConfigurationFirebase.getFirebaseDatabase()
                .child("shopping_carts")
                .child(currentOngId)
                .child(cartItem.getStockItemId());

        cartItemRef.removeValue().addOnSuccessListener(aVoid -> {
            Toast.makeText(context, "Item removido do carrinho!", Toast.LENGTH_SHORT).show();
        });
    }
}
