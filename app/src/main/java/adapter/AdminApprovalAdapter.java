package adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
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
import model.ItemDisplay;

// REBUILD: Restoring adapter to its original state.
public class AdminApprovalAdapter extends RecyclerView.Adapter<AdminApprovalAdapter.MyViewHolder> {

    private List<ItemDisplay> items;
    private Context context;

    public AdminApprovalAdapter(List<ItemDisplay> items, Context context) {
        this.items = items;
        this.context = context;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_admin_approval, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ItemDisplay item = items.get(position);

        // --- Bind common data ---
        holder.productName.setText(item.getProductName());
        holder.establishmentName.setText("- " + item.getEstablishmentName());
        
        if (item.getEstablishmentAddress() != null && !item.getEstablishmentAddress().isEmpty()) {
            holder.establishmentAddress.setText(item.getEstablishmentAddress());
            holder.establishmentAddress.setVisibility(View.VISIBLE);
        } else {
            holder.establishmentAddress.setVisibility(View.GONE);
        }

        DecimalFormat formatter = new DecimalFormat("0.##");
        String formattedQuantity = formatter.format(item.getStockItemQuantity());
        String unitType = item.getProductUnitType() != null ? item.getProductUnitType() : "";
        holder.quantity.setText("Quantidade: " + formattedQuantity + " " + unitType.trim());

        holder.expiration.setText("Validade: " + item.getStockItemExpirationDate());
        
        // --- State Machine Logic ---
        holder.pendingActionsLayout.setVisibility(View.GONE);
        holder.collectedStateLayout.setVisibility(View.GONE);
        holder.finalStatusText.setVisibility(View.GONE);

        switch (item.getStockItemStatus()) {
            case "Aguarde a coleta":
                holder.pendingActionsLayout.setVisibility(View.VISIBLE);
                holder.approveButton.setOnClickListener(v -> updateDonationStatus(item, "Doação Coletada"));
                holder.cancelButton.setOnClickListener(v -> updateDonationStatus(item, "Doação Cancelada"));
                break;

            case "Doação Coletada":
                holder.collectedStateLayout.setVisibility(View.VISIBLE);
                holder.collectedStatusText.setTextColor(ContextCompat.getColor(context, R.color.watergreen));
                holder.removeFromStockButton.setOnClickListener(v -> showRemoveFromStockConfirmation(item));
                break;

            case "Doação Cancelada":
                holder.finalStatusText.setVisibility(View.VISIBLE);
                holder.finalStatusText.setText("Doação Cancelada");
                holder.finalStatusText.setTextColor(ContextCompat.getColor(context, R.color.status_red));
                break;

            case "Produto retirado do estoque":
                holder.finalStatusText.setVisibility(View.VISIBLE);
                holder.finalStatusText.setText("Produto retirado do estoque");
                holder.finalStatusText.setTextColor(Color.GRAY);
                break;

            default:
                // Catch-all for any other status
                holder.finalStatusText.setVisibility(View.VISIBLE);
                holder.finalStatusText.setText(item.getStockItemStatus());
                holder.finalStatusText.setTextColor(Color.GRAY);
                break;
        }
    }
    
    private void showRemoveFromStockConfirmation(ItemDisplay item) {
        new AlertDialog.Builder(context)
            .setTitle("Retirar do Estoque")
            .setMessage("Tem certeza que deseja retirar o produto do estoque?")
            .setPositiveButton("Confirmar", (dialog, which) -> {
                updateDonationStatus(item, "Produto retirado do estoque");
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void updateDonationStatus(ItemDisplay item, String newStatus) {
        getDatabaseReference(item).child("stockItemStatus").setValue(newStatus)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(context, "Status atualizado!", Toast.LENGTH_SHORT).show();
                // If the product is now retired, remove it from all carts
                if ("Produto retirado do estoque".equals(newStatus)) {
                    removeOrphanedCartItems(item.getStockItemId());
                }
            })
            .addOnFailureListener(e -> Toast.makeText(context, "Falha ao atualizar status: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void removeOrphanedCartItems(String removedStockItemId) {
        if (removedStockItemId == null) return;
        DatabaseReference cartsRef = ConfigurationFirebase.getFirebaseDatabase().child("shopping_carts");
        cartsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ongCartSnapshot : dataSnapshot.getChildren()) {
                    if (ongCartSnapshot.hasChild(removedStockItemId)) {
                         ongCartSnapshot.child(removedStockItemId).getRef().removeValue();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("AdminApprovalAdapter", "Error cleaning carts: " + databaseError.getMessage());
            }
        });
    }

    private DatabaseReference getDatabaseReference(ItemDisplay item) {
        return ConfigurationFirebase.getFirebaseDatabase()
                .child("stock_items")
                .child(item.getEstablishmentId())
                .child(item.getStockItemId());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView productName, establishmentName, establishmentAddress, quantity, expiration, collectedStatusText, finalStatusText;
        Button approveButton, cancelButton, removeFromStockButton;
        LinearLayout pendingActionsLayout, collectedStateLayout;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.text_approval_product_name);
            establishmentName = itemView.findViewById(R.id.text_approval_establishment_name);
            establishmentAddress = itemView.findViewById(R.id.text_approval_establishment_address);
            quantity = itemView.findViewById(R.id.text_approval_quantity);
            expiration = itemView.findViewById(R.id.text_approval_expiration);
            
            // State Layouts
            pendingActionsLayout = itemView.findViewById(R.id.layout_pending_actions);
            collectedStateLayout = itemView.findViewById(R.id.layout_collected_state);
            finalStatusText = itemView.findViewById(R.id.text_final_status);

            // Buttons and Texts inside states
            approveButton = itemView.findViewById(R.id.button_approve);
            cancelButton = itemView.findViewById(R.id.button_cancel);
            collectedStatusText = itemView.findViewById(R.id.text_collected_status);
            removeFromStockButton = itemView.findViewById(R.id.button_remove_from_stock);
        }
    }
}
