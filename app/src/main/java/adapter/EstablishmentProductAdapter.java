package adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.company.doafacil.R;
import com.google.firebase.database.DatabaseReference;

import java.text.DecimalFormat;
import java.util.List;

import helper.ConfigurationFirebase;
import helper.UserFirebase;
import model.ItemDisplay;

// REBUILD: Restoring adapter to its original state.
public class EstablishmentProductAdapter extends RecyclerView.Adapter<EstablishmentProductAdapter.MyViewHolder> {

    private List<ItemDisplay> stockItemsDisplay;
    private Context context;
    private String mode;

    public EstablishmentProductAdapter(List<ItemDisplay> stockItemsDisplay, Context context, String mode) {
        this.stockItemsDisplay = stockItemsDisplay;
        this.context = context;
        this.mode = mode;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View itemLista = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_establishment_product, parent, false);
        return new MyViewHolder(itemLista);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int i) {
        ItemDisplay displayItem = stockItemsDisplay.get(i);

        holder.nameTextView.setText(displayItem.getProductName());
        holder.expirationDateTextView.setText("Validade: " + displayItem.getStockItemExpirationDate());

        DecimalFormat formatter = new DecimalFormat("0.##");
        holder.quantityTextView.setText(formatter.format(displayItem.getStockItemQuantity()));

        holder.unitTypeTextView.setText(displayItem.getProductUnitType());

        if ("pendente".equals(mode)) {
            holder.statusTextView.setText("Status: Aguarde a coleta");
            holder.cancelButton.setVisibility(View.VISIBLE);
            holder.cancelButton.setOnClickListener(v -> {
                String currentUserId = UserFirebase.getIdUser();
                if (currentUserId == null || displayItem.getStockItemId() == null) return;

                DatabaseReference stockItemRef = ConfigurationFirebase.getFirebaseDatabase()
                        .child("stock_items")
                        .child(currentUserId)
                        .child(displayItem.getStockItemId())
                        .child("stockItemStatus");

                stockItemRef.setValue("Doação Cancelada").addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(context, "Doação cancelada.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Falha ao cancelar doação.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        } else { // "historico" mode
            holder.statusTextView.setText("Status: " + displayItem.getStockItemStatus());
            holder.cancelButton.setVisibility(View.GONE);
        }

        String pickupDate = displayItem.getStockItemPickupDate();
        String pickupTime = displayItem.getStockItemPickupTime();

        if (pickupDate != null && !pickupDate.isEmpty() && pickupTime != null && !pickupTime.isEmpty()) {
            String dateTimeString = "Data para retirada: " + pickupDate + " às " + pickupTime;
            holder.pickupDateTimeTextView.setText(dateTimeString);
            holder.pickupDateTimeTextView.setVisibility(View.VISIBLE);
        } else {
            holder.pickupDateTimeTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return stockItemsDisplay.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView nameTextView;
        TextView expirationDateTextView;
        TextView quantityTextView;
        TextView unitTypeTextView;
        ImageView cancelButton;
        TextView pickupDateTimeTextView;
        TextView statusTextView;

        public MyViewHolder(View itemView) {
            super(itemView);

            nameTextView = itemView.findViewById(R.id.text_establishment_name_adapter);
            unitTypeTextView = itemView.findViewById(R.id.text_establishment_unit_adapter);
            expirationDateTextView = itemView.findViewById(R.id.text_establishment_expiration_adapter);
            quantityTextView = itemView.findViewById(R.id.text_establishment_quantity_adapter);
            cancelButton = itemView.findViewById(R.id.image_cancel_donation);
            pickupDateTimeTextView = itemView.findViewById(R.id.text_establishment_datetime_adapter);
            statusTextView = itemView.findViewById(R.id.text_establishment_status_adapter);
        }
    }
}
