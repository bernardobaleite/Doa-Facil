package adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.company.doafacil.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.util.ArrayList;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import helper.ConfigurationFirebase;
import model.ItemDisplay;
import model.Order;
import model.StockItem;

// RE-DESIGN: Applying correct background tint to buttons with white text.
public class AdminOrdersAdapter extends RecyclerView.Adapter<AdminOrdersAdapter.MyViewHolder> {

    private List<Order> orders;
    private Map<String, String> ongNamesMap;
    private Context context;
    private int expandedPosition = -1;

    public AdminOrdersAdapter(List<Order> orders, Map<String, String> ongNamesMap, Context context) {
        this.orders = orders;
        this.ongNamesMap = ongNamesMap;
        this.context = context;
    }

    public void setData(List<Order> orders) {
        this.orders = orders;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_admin_order_item, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Order order = orders.get(position);
        final boolean isExpanded = position == expandedPosition;

        setupCommonInfo(holder, order, isExpanded);
        resetStateViews(holder);

        String status = order.getStatus();
        holder.currentStatus.setText(status);
        int statusColor = ContextCompat.getColor(context, R.color.grey);
        int whiteColor = ContextCompat.getColor(context, R.color.white);

        switch (status) {
            case "Realize o agendamento":
                statusColor = ContextCompat.getColor(context, R.color.status_warning_yellow);
                holder.stateWaitingSchedule.setVisibility(View.VISIBLE);
                holder.btnNoStock1.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.status_red));
                holder.btnNoStock1.setTextColor(whiteColor);
                holder.btnCancel1.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.status_red));
                holder.btnCancel1.setTextColor(whiteColor);
                holder.btnNoStock1.setOnClickListener(v -> updateOrderStatus(order, "Produto sem estoque/vencido"));
                holder.btnCancel1.setOnClickListener(v -> cancelOrderAndReturnStock(order));
                break;

            case "Data e horários determinados - Por favor, aguarde a liberação":
                statusColor = ContextCompat.getColor(context, R.color.status_warning_yellow);
                holder.stateScheduleDetermined.setVisibility(View.VISIBLE);
                holder.btnRelease.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.watergreen));
                holder.btnRelease.setTextColor(whiteColor);
                holder.btnReschedule1.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.status_warning_yellow));
                holder.btnReschedule1.setTextColor(whiteColor);
                holder.btnNoStock2.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.status_red));
                holder.btnNoStock2.setTextColor(whiteColor);
                holder.btnRelease.setOnClickListener(v -> updateOrderStatus(order, "Seu pedido já está disponível - Retire sua doação"));
                holder.btnReschedule1.setOnClickListener(v -> updateOrderStatus(order, "Realize um novo agendamento"));
                holder.btnNoStock2.setOnClickListener(v -> updateOrderStatus(order, "Produto sem estoque/vencido"));
                break;

            case "Seu pedido já está disponível - Retire sua doação":
                statusColor = ContextCompat.getColor(context, R.color.watergreen);
                holder.stateWaitingPickup.setVisibility(View.VISIBLE);
                holder.btnDistributed.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.watergreen));
                holder.btnDistributed.setTextColor(whiteColor);
                holder.btnReschedule2.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.status_warning_yellow));
                holder.btnReschedule2.setTextColor(whiteColor);
                holder.btnCancel2.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.status_red));
                holder.btnCancel2.setTextColor(whiteColor);
                holder.btnDistributed.setOnClickListener(v -> updateOrderStatus(order, "Doação distribuída"));
                holder.btnReschedule2.setOnClickListener(v -> updateOrderStatus(order, "Realize um novo agendamento"));
                holder.btnCancel2.setOnClickListener(v -> cancelOrderAndReturnStock(order));
                break;
            
            case "Doação distribuída":
                 statusColor = ContextCompat.getColor(context, R.color.watergreen);
                 break;

            case "Pedido cancelado":
            case "Produto sem estoque/vencido":
                statusColor = ContextCompat.getColor(context, R.color.status_red);
                break;
        }
        holder.currentStatus.setTextColor(statusColor);
    }
    
    private void setupCommonInfo(MyViewHolder holder, Order order, boolean isExpanded) {
        String ongName = ongNamesMap.get(order.getOngId());
        holder.ongNameValue.setText(ongName != null ? ongName : order.getOngId());
        holder.orderIdValue.setText(order.getOrderId());

        int distinctProductCount = (order.getItems() != null) ? order.getItems().size() : 0;
        String summaryText = (distinctProductCount == 1) ? "Pedido com 1 produto" : "Pedido com " + distinctProductCount + " produtos";
        holder.orderSummary.setText(summaryText);

        String scheduledDateTime = (order.getScheduledDate() != null && !order.getScheduledDate().isEmpty())
            ? "Agendado para: " + order.getScheduledDate() + " às " + order.getScheduledTime()
            : "Aguardando agendamento";
        holder.scheduledDateTime.setText(scheduledDateTime);

        holder.expandableLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.mainInfoLayout.setOnClickListener(v -> {
            int clickedPosition = holder.getBindingAdapterPosition();
            if (clickedPosition == RecyclerView.NO_POSITION) return;
            int previousExpanded = expandedPosition;
            expandedPosition = isExpanded ? -1 : clickedPosition;
            if (previousExpanded != -1) notifyItemChanged(previousExpanded);
            notifyItemChanged(clickedPosition);
        });

        if (isExpanded && order.getItems() != null) {
            List<ItemDisplay> items = new ArrayList<>(order.getItems().values());
            OrderProductItemAdapter subAdapter = new OrderProductItemAdapter(items, context);
            holder.innerRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            holder.innerRecyclerView.setAdapter(subAdapter);
        }
    }
    
    private void resetStateViews(MyViewHolder holder) {
        holder.stateWaitingSchedule.setVisibility(View.GONE);
        holder.stateScheduleDetermined.setVisibility(View.GONE);
        holder.stateWaitingPickup.setVisibility(View.GONE);
    }
    
    private void updateOrderStatus(Order order, String newStatus) {
        getOrderReference(order).child("status").setValue(newStatus)
            .addOnSuccessListener(aVoid -> Toast.makeText(context, "Status do pedido atualizado!", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> {
                String errorMessage = e.getMessage();
                Toast.makeText(context, "Falha ao atualizar status: " + errorMessage, Toast.LENGTH_LONG).show();
            });
    }

    private void cancelOrderAndReturnStock(Order order) {
        if (order.getItems() == null) return;
        for (ItemDisplay item : order.getItems().values()) {
            returnStockForItem(item);
        }
        updateOrderStatus(order, "Pedido cancelado");
    }

    private void returnStockForItem(ItemDisplay itemToReturn) {
        DatabaseReference stockItemRef = ConfigurationFirebase.getFirebaseDatabase()
            .child("stock_items")
            .child(itemToReturn.getEstablishmentId())
            .child(itemToReturn.getStockItemId());

        stockItemRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                StockItem currentStock = mutableData.getValue(StockItem.class);
                 if (currentStock == null) {
                     return Transaction.abort();
                } else {
                    double newQuantity = currentStock.getStockItemQuantity() + itemToReturn.getStockItemQuantity();
                    currentStock.setStockItemQuantity(newQuantity);
                    mutableData.setValue(currentStock);
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Log.e("AdminOrdersAdapter", "Stock return failed for item " + itemToReturn.getStockItemId() + ": " + error.getMessage());
                }
            }
        });
    }

    private DatabaseReference getOrderReference(Order order) {
        return ConfigurationFirebase.getFirebaseDatabase()
            .child("orders")
            .child(order.getOngId())
            .child(order.getOrderId());
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView ongNameValue, orderIdValue, orderSummary, scheduledDateTime, currentStatus;
        LinearLayout stateWaitingSchedule, stateScheduleDetermined, stateWaitingPickup, expandableLayout;
        Button btnNoStock1, btnCancel1, btnRelease, btnReschedule1, btnNoStock2, btnDistributed, btnReschedule2, btnCancel2;
        RecyclerView innerRecyclerView;
        View mainInfoLayout;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            ongNameValue = itemView.findViewById(R.id.text_order_ong_name_value);
            orderIdValue = itemView.findViewById(R.id.text_order_id_value);
            orderSummary = itemView.findViewById(R.id.text_order_summary_admin);
            scheduledDateTime = itemView.findViewById(R.id.text_order_scheduled_datetime);
            currentStatus = itemView.findViewById(R.id.text_current_status_value);

            stateWaitingSchedule = itemView.findViewById(R.id.layout_admin_state_waiting_schedule);
            stateScheduleDetermined = itemView.findViewById(R.id.layout_admin_state_schedule_determined);
            stateWaitingPickup = itemView.findViewById(R.id.layout_admin_state_waiting_pickup);

            btnNoStock1 = itemView.findViewById(R.id.button_admin_no_stock);
            btnCancel1 = itemView.findViewById(R.id.button_admin_cancel_order_1);
            btnRelease = itemView.findViewById(R.id.button_admin_release_donation);
            btnReschedule1 = itemView.findViewById(R.id.button_admin_reschedule);
            btnNoStock2 = itemView.findViewById(R.id.button_admin_no_stock_2);
            btnDistributed = itemView.findViewById(R.id.button_admin_donation_distributed);
            btnReschedule2 = itemView.findViewById(R.id.button_admin_reschedule_2);
            btnCancel2 = itemView.findViewById(R.id.button_admin_cancel_order_2);

            mainInfoLayout = itemView.findViewById(R.id.layout_admin_main_info);
            expandableLayout = itemView.findViewById(R.id.layout_admin_expandable_items);
            innerRecyclerView = itemView.findViewById(R.id.recycler_admin_order_items_list);
        }
    }
}
