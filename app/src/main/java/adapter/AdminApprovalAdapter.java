package adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.company.doafacil.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fragment.AdminApprovalsFragment.AdminApprovalItem;
import helper.ConfigurationFirebase;
import model.ReceivedDonationItem;
import model.StockItem;

// RE-DESIGN: Correctly mapping all data to the new layout fields.
public class AdminApprovalAdapter extends RecyclerView.Adapter<AdminApprovalAdapter.MyViewHolder> {

    private List<AdminApprovalItem> items;
    private Context context;

    public AdminApprovalAdapter(List<AdminApprovalItem> items, Context context) {
        this.items = items;
        this.context = context;
    }

    public void setData(List<AdminApprovalItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_admin_approval, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        AdminApprovalItem displayItem = items.get(position);
        String status = displayItem.effectiveStatus;

        // THE FIX: Correctly map all data to the new layout.
        holder.establishmentName.setText("- " + displayItem.establishmentName);
        holder.establishmentAddress.setText(displayItem.establishmentAddress);
        holder.collectionDate.setText("Data para coleta: " + displayItem.donation.getReceivedScheduledDateTime());

        Map<String, ReceivedDonationItem> receivedItems = displayItem.donation.getReceivedItems();
        if (receivedItems != null && !receivedItems.isEmpty()) {
            ReceivedDonationItem firstItem = receivedItems.values().iterator().next();
            holder.productName.setText(firstItem.getProductName());
            DecimalFormat formatter = new DecimalFormat("0.##");
            String quantityText = "Qtd: " + formatter.format(firstItem.getReceivedQuantity()) + " " + firstItem.getProductUnitType();
            holder.quantity.setText(quantityText);
            holder.expiration.setText("Data de validade: " + firstItem.getReceivedExpirationDate());
        } else {
            holder.productName.setText("Doação sem itens");
            holder.quantity.setText("");
            holder.expiration.setText("");
        }

        holder.pendingActionsLayout.setVisibility(View.GONE);
        holder.collectedStateLayout.setVisibility(View.GONE);
        holder.finalStatusText.setVisibility(View.GONE);

        switch (status) {
            case "Aguarde a coleta":
                holder.pendingActionsLayout.setVisibility(View.VISIBLE);
                holder.approveButton.setText("Aprovar"); // Changed from "Confirmar Coleta" to "Aprovar"
                holder.approveButton.setOnClickListener(v -> confirmCollection(displayItem));
                holder.cancelButton.setOnClickListener(v -> cancelDonation(displayItem));
                break;
            case "Doação coletada":
                holder.collectedStateLayout.setVisibility(View.VISIBLE);
                holder.collectedStatusText.setText("Status: Doação coletada");
                holder.collectedStatusText.setTextColor(ContextCompat.getColor(context, R.color.watergreen));
                holder.removeFromStockButton.setOnClickListener(v -> removeItemsFromStock(displayItem));
                break;
            case "Removido do estoque":
            case "Doação cancelada":
            default:
                holder.finalStatusText.setVisibility(View.VISIBLE);
                holder.finalStatusText.setText("Status: " + status);
                holder.finalStatusText.setTextColor(ContextCompat.getColor(context, R.color.status_red));
                break;
        }
    }

    private void confirmCollection(AdminApprovalItem displayItem) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        Map<String, Object> childUpdates = new HashMap<>();

        childUpdates.put("/received_donations/" + displayItem.donation.getDonationId() + "/receivedStatus", "Doação coletada");

        for (Map.Entry<String, ReceivedDonationItem> entry : displayItem.donation.getReceivedItems().entrySet()) {
            ReceivedDonationItem promisedItem = entry.getValue();
            String newStockItemId = rootRef.child("stock_items").push().getKey();

            StockItem newStockItem = new StockItem();
            newStockItem.setStockItemId(newStockItemId);
            newStockItem.setDonationId(displayItem.donation.getDonationId());
            newStockItem.setProductId(promisedItem.getProductId());
            newStockItem.setStockItemQuantity(promisedItem.getReceivedQuantity());
            newStockItem.setStockItemExpirationDate(promisedItem.getReceivedExpirationDate());
            newStockItem.setStockItemStatus("Disponível");
            newStockItem.setStockItemCreatedAt(System.currentTimeMillis());

            childUpdates.put("/stock_items/" + newStockItemId, newStockItem);
        }

        rootRef.updateChildren(childUpdates).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) Toast.makeText(context, "Falha ao confirmar coleta.", Toast.LENGTH_SHORT).show();
        });
    }

    private void cancelDonation(AdminApprovalItem displayItem) {
        DatabaseReference donationRef = ConfigurationFirebase.getFirebaseDatabase()
                .child("received_donations").child(displayItem.donation.getDonationId());
        donationRef.child("receivedStatus").setValue("Doação cancelada");
    }

    private void removeItemsFromStock(AdminApprovalItem displayItem) {
        DatabaseReference stockItemsRef = ConfigurationFirebase.getFirebaseDatabase().child("stock_items");
        Query query = stockItemsRef.orderByChild("donationId").equalTo(displayItem.donation.getDonationId());

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Toast.makeText(context, "Erro: Itens de estoque já foram removidos ou não existem.", Toast.LENGTH_LONG).show();
                    return;
                }
                Map<String, Object> stockUpdates = new HashMap<>();
                for (DataSnapshot stockItemSnapshot : dataSnapshot.getChildren()) {
                    stockUpdates.put(stockItemSnapshot.getKey() + "/stockItemStatus", "Removido do estoque");
                }

                stockItemsRef.updateChildren(stockUpdates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(context, "Itens removidos do estoque.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Falha ao remover itens do estoque.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(context, "Erro ao buscar itens de estoque: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView productName, establishmentName, establishmentAddress, quantity, expiration, collectionDate, collectedStatusText, finalStatusText;
        Button approveButton, cancelButton, removeFromStockButton;
        LinearLayout pendingActionsLayout, collectedStateLayout;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.text_approval_product_name);
            establishmentName = itemView.findViewById(R.id.text_approval_establishment_name);
            establishmentAddress = itemView.findViewById(R.id.text_approval_establishment_address);
            quantity = itemView.findViewById(R.id.text_approval_quantity);
            expiration = itemView.findViewById(R.id.text_approval_expiration);
            collectionDate = itemView.findViewById(R.id.text_approval_collection_date);
            
            pendingActionsLayout = itemView.findViewById(R.id.layout_pending_actions);
            collectedStateLayout = itemView.findViewById(R.id.layout_collected_state);
            finalStatusText = itemView.findViewById(R.id.text_final_status);

            approveButton = itemView.findViewById(R.id.button_approve);
            cancelButton = itemView.findViewById(R.id.button_cancel);
            collectedStatusText = itemView.findViewById(R.id.text_collected_status);
            removeFromStockButton = itemView.findViewById(R.id.button_remove_from_stock);
        }
    }
}
