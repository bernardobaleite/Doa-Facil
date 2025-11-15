package adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.company.doafacil.R;
import com.google.firebase.database.DatabaseReference;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import helper.ConfigurationFirebase;
import model.ReceivedDonation;
import model.ReceivedDonationItem;

// RE-ARCH: Fixing the incorrect getter method name for the scheduled date.
public class EstablishmentProductAdapter extends RecyclerView.Adapter<EstablishmentProductAdapter.MyViewHolder> {

    private static class FlattenedItem {
        final ReceivedDonation parentDonation;
        final ReceivedDonationItem item;

        FlattenedItem(ReceivedDonation parent, ReceivedDonationItem item) {
            this.parentDonation = parent;
            this.item = item;
        }
    }

    private List<ReceivedDonation> donationList = new ArrayList<>();
    private final List<FlattenedItem> flattenedList = new ArrayList<>();
    private final Context context;

    public EstablishmentProductAdapter(Context context) {
        this.context = context;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        if (position >= 0 && position < flattenedList.size()) {
            FlattenedItem item = flattenedList.get(position);
            return Objects.hash(item.parentDonation.getDonationId(), item.item.getProductId());
        }
        return RecyclerView.NO_ID;
    }

    public void setData(List<ReceivedDonation> newDonationList) {
        this.donationList = newDonationList != null ? newDonationList : new ArrayList<>();
        flattenData();
        notifyDataSetChanged();
    }

    private void flattenData() {
        flattenedList.clear();
        for (ReceivedDonation donation : donationList) {
            if (donation.getReceivedItems() != null) {
                for (ReceivedDonationItem item : donation.getReceivedItems().values()) {
                    flattenedList.add(new FlattenedItem(donation, item));
                }
            }
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View itemLista = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_establishment_product, parent, false);
        return new MyViewHolder(itemLista);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int i) {
        FlattenedItem displayItem = flattenedList.get(i);
        ReceivedDonation parent = displayItem.parentDonation;
        ReceivedDonationItem item = displayItem.item;

        holder.productName.setText(item.getProductName());
        holder.expirationDate.setText("Data de validade: " + item.getReceivedExpirationDate());
        // THE FIX: Use the correct getter for the scheduled date.
        holder.collectionDate.setText("Data para retirada: " + parent.getReceivedScheduledDateTime());

        DecimalFormat formatter = new DecimalFormat("0.##");
        holder.quantity.setText(formatter.format(item.getReceivedQuantity()));
        holder.unitType.setText(item.getProductUnitType());

        String status = parent.getReceivedStatus();
        holder.status.setText("Status: " + status);

        if ("Aguarde a coleta".equals(status)) {
            holder.cancelButton.setVisibility(View.VISIBLE);
            holder.cancelButton.setOnClickListener(v -> {
                DatabaseReference donationRef = ConfigurationFirebase.getFirebaseDatabase()
                        .child("received_donations")
                        .child(parent.getDonationId());
                donationRef.removeValue().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(context, "Doação removida.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Falha ao remover doação.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        } else {
            holder.cancelButton.setVisibility(View.GONE);
        }

        if ("Doação cancelada".equals(status) || "Removido do estoque".equals(status)) {
            holder.status.setTextColor(ContextCompat.getColor(context, R.color.status_red));
        } else if ("Doação coletada".equals(status)) {
            holder.status.setTextColor(ContextCompat.getColor(context, R.color.watergreen));
        } else { 
            holder.status.setTextColor(ContextCompat.getColor(context, R.color.black));
        }
    }

    @Override
    public int getItemCount() {
        return flattenedList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView productName, expirationDate, quantity, unitType, status, collectionDate;
        ImageView cancelButton;

        public MyViewHolder(View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.text_establishment_name_adapter);
            unitType = itemView.findViewById(R.id.text_establishment_unit_adapter);
            expirationDate = itemView.findViewById(R.id.text_establishment_expiration_adapter);
            quantity = itemView.findViewById(R.id.text_establishment_quantity_adapter);
            cancelButton = itemView.findViewById(R.id.image_cancel_donation);
            collectionDate = itemView.findViewById(R.id.text_establishment_datetime_adapter);
            status = itemView.findViewById(R.id.text_establishment_status_adapter);
        }
    }
}
