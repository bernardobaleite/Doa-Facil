package adapter;

import android.content.Context;
import android.content.res.ColorStateList;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import helper.ConfigurationFirebase;
import model.Order;
import model.OrderItem;
import model.StockItem;

// RE-ARCH: Implementing the final "Calculated Truth" architecture.
public class AdminOrdersAdapter extends RecyclerView.Adapter<AdminOrdersAdapter.MyViewHolder> {

    public interface OnOrderInteractionListener {
        void onOrderCancelled();
    }

    private List<Order> orders;
    private Map<String, String> ongNamesMap;
    private Context context;
    private int expandedPosition = -1;
    private final OnOrderInteractionListener listener;

    public AdminOrdersAdapter(List<Order> orders, Map<String, String> ongNamesMap, Context context, OnOrderInteractionListener listener) {
        this.orders = orders;
        this.ongNamesMap = ongNamesMap;
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<Order> orders, Map<String, String> ongNamesMap) {
        this.orders = orders;
        this.ongNamesMap = ongNamesMap;
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

        String status = order.getOrderStatus();
        holder.currentStatus.setText(status);
        int statusColor = ContextCompat.getColor(context, R.color.grey);
        int whiteColor = ContextCompat.getColor(context, R.color.white);

        switch (status) {
            case "Realize o agendamento":
                statusColor = ContextCompat.getColor(context, R.color.status_warning_yellow);
                holder.stateWaitingSchedule.setVisibility(View.VISIBLE);
                holder.btnNoStock1.setOnClickListener(v -> removeStockItemsAndSetStatus(order, "Produto sem estoque/vencido"));
                holder.btnCancel1.setOnClickListener(v -> cancelOrder(order));
                break;

            case "Data e horários determinados - Por favor, aguarde a liberação":
                statusColor = ContextCompat.getColor(context, R.color.status_warning_yellow);
                holder.stateScheduleDetermined.setVisibility(View.VISIBLE);
                holder.btnRelease.setOnClickListener(v -> releaseOrder(order));
                holder.btnReschedule1.setOnClickListener(v -> updateOrderStatus(order, "Realize um novo agendamento", null));
                holder.btnNoStock2.setOnClickListener(v -> removeStockItemsAndSetStatus(order, "Produto sem estoque/vencido"));
                break;

            case "Seu pedido já está disponível - Retire sua doação":
                statusColor = ContextCompat.getColor(context, R.color.watergreen);
                holder.stateWaitingPickup.setVisibility(View.VISIBLE);
                holder.btnDistributed.setOnClickListener(v -> distributeOrder(order));
                holder.btnReschedule2.setOnClickListener(v -> updateOrderStatus(order, "Realize um novo agendamento", null));
                holder.btnCancel2.setOnClickListener(v -> cancelOrder(order)); // This now just cancels the order status
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
        styleButtons(holder, whiteColor);
    }
    
    private void setupCommonInfo(MyViewHolder holder, Order order, boolean isExpanded) {
        String ongName = ongNamesMap.get(order.getOngId());
        holder.ongNameValue.setText(ongName != null ? ongName : "ID: " + order.getOngId());
        holder.orderIdValue.setText(order.getOrderId());
        int distinctProductCount = (order.getOrderItems() != null) ? order.getOrderItems().size() : 0;
        holder.orderSummary.setText("Pedido com " + distinctProductCount + " produtos");
        String scheduledDateTime = (order.getOrderScheduledDateTime() != null && !order.getOrderScheduledDateTime().isEmpty())
            ? "Agendado para: " + order.getOrderScheduledDateTime()
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
        if (isExpanded && order.getOrderItems() != null) {
            List<OrderItem> items = new ArrayList<>(order.getOrderItems().values());
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
    
    private void updateOrderStatus(Order order, String newStatus, @Nullable Runnable onSuccessAction) {
        getOrderReference(order).child("orderStatus").setValue(newStatus).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(context, "Status do pedido atualizado!", Toast.LENGTH_SHORT).show();
                if (onSuccessAction != null) {
                    onSuccessAction.run();
                }
            } else {
                Toast.makeText(context, "Falha ao atualizar status: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void removeStockItemsAndSetStatus(Order order, String finalStatus) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            updateOrderStatus(order, finalStatus, null);
            return;
        }
        DatabaseReference rootRef = ConfigurationFirebase.getFirebaseDatabase();
        Map<String, Object> updates = new HashMap<>();
        updates.put("/orders/" + order.getOngId() + "/" + order.getOrderId() + "/orderStatus", finalStatus);
        for (OrderItem item : order.getOrderItems().values()) {
            updates.put("/stock_items/" + item.getStockItemId(), null);
        }
        rootRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(context, "Itens removidos do estoque e pedido atualizado.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "Falha na operação de remoção de estoque.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void cancelOrder(Order order) {
        Runnable onCancelSuccess = () -> {
            if (listener != null) {
                listener.onOrderCancelled();
            }
        };
        updateOrderStatus(order, "Pedido cancelado", onCancelSuccess);
    }

    private void releaseOrder(Order order) {
        updateOrderStatus(order, "Seu pedido já está disponível - Retire sua doação", null);
    }

    private void distributeOrder(Order order) {
        DatabaseReference stockRef = ConfigurationFirebase.getFirebaseDatabase().child("stock_items");
        AtomicInteger successCounter = new AtomicInteger(0);
        int totalItems = order.getOrderItems().size();

        for (OrderItem itemToDeduct : order.getOrderItems().values()) {
            stockRef.child(itemToDeduct.getStockItemId()).runTransaction(new Transaction.Handler() {
                @NonNull @Override
                public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                    StockItem currentStock = mutableData.getValue(StockItem.class);
                    if (currentStock == null) {
                        return Transaction.success(mutableData);
                    }
                    double newQuantity = currentStock.getStockItemQuantity() - itemToDeduct.getOrderItemQuantity();
                    currentStock.setStockItemQuantity(Math.max(0, newQuantity));
                    mutableData.setValue(currentStock);
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot dataSnapshot) {
                    if (committed) {
                        if (successCounter.incrementAndGet() == totalItems) {
                            updateOrderStatus(order, "Doação distribuída", null);
                        }
                    } else {
                        Toast.makeText(context, "Falha ao deduzir item do estoque.", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    private DatabaseReference getOrderReference(Order order) {
        return ConfigurationFirebase.getFirebaseDatabase().child("orders").child(order.getOngId()).child(order.getOrderId());
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }
    
    private void styleButtons(MyViewHolder holder, int textColor) {
        Button[] redButtons = {holder.btnNoStock1, holder.btnCancel1, holder.btnNoStock2, holder.btnCancel2};
        Button[] yellowButtons = {holder.btnReschedule1, holder.btnReschedule2};
        Button[] greenButtons = {holder.btnRelease, holder.btnDistributed};

        for(Button btn : redButtons) {
            btn.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.status_red));
            btn.setTextColor(textColor);
        }
        for(Button btn : yellowButtons) {
            btn.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.status_warning_yellow));
            btn.setTextColor(textColor);
        }
        for(Button btn : greenButtons) {
            btn.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.watergreen));
            btn.setTextColor(textColor);
        }
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
