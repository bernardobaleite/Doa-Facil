package adapter;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import helper.ConfigurationFirebase;
import model.ItemDisplay;
import model.Order;
import model.StockItem;

// RE-DESIGN: Implementing the user's color-coded state machine.
public class OngOrdersAdapter extends RecyclerView.Adapter<OngOrdersAdapter.MyViewHolder> {

    private List<Order> orders;
    private Context context;
    private int expandedPosition = -1;

    public OngOrdersAdapter(List<Order> orders, Context context) {
        this.orders = orders;
        this.context = context;
    }

    public void setData(List<Order> orders) {
        this.orders = orders;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_ong_order_item, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Order order = orders.get(position);
        final boolean isExpanded = position == expandedPosition;

        setupCommonInfo(holder, order, isExpanded);
        resetStateViews(holder);

        switch (order.getStatus()) {
            case "Realize o agendamento":
                holder.schedulingForm.setVisibility(View.VISIBLE);
                holder.confirmScheduleButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.watergreen));
                holder.cancelButton.setVisibility(View.VISIBLE);
                holder.cancelButton.setColorFilter(ContextCompat.getColor(context, R.color.status_red));
                holder.cancelButton.setOnClickListener(v -> cancelOrder(order));
                setupSchedulingControls(holder, order);
                break;

            case "Realize um novo agendamento":
                holder.statusText.setVisibility(View.VISIBLE);
                holder.statusText.setText(order.getStatus());
                holder.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_warning_yellow));
                holder.schedulingForm.setVisibility(View.VISIBLE);
                holder.confirmScheduleButton.setText("Confirmar Novo Agendamento");
                holder.confirmScheduleButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.watergreen));
                holder.cancelButton.setVisibility(View.VISIBLE);
                holder.cancelButton.setColorFilter(ContextCompat.getColor(context, R.color.status_red));
                holder.cancelButton.setOnClickListener(v -> cancelOrder(order));
                setupSchedulingControls(holder, order);
                break;

            case "Seu pedido já está disponível - Retire sua doação":
                 holder.statusText.setVisibility(View.VISIBLE);
                 holder.statusText.setText(order.getStatus());
                 holder.statusText.setTextColor(ContextCompat.getColor(context, R.color.watergreen));
                 // TODO: Add "Retirar agora" button logic if user requests it again.
                 break;

            case "Data e horários determinados - Por favor, aguarde a liberação":
                holder.statusText.setVisibility(View.VISIBLE);
                holder.statusText.setText(order.getStatus());
                holder.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_warning_yellow));
                break;

            case "Pedido cancelado":
            case "Produto sem estoque/vencido":
                holder.statusText.setVisibility(View.VISIBLE);
                holder.statusText.setText(order.getStatus());
                holder.statusText.setTextColor(ContextCompat.getColor(context, R.color.status_red));
                break;

            default: // Other states like "Doação distribuída"
                holder.statusText.setVisibility(View.VISIBLE);
                holder.statusText.setText(order.getStatus());
                // Set a default or positive color for other states if needed
                break;
        }
    }

    private void setupCommonInfo(MyViewHolder holder, Order order, boolean isExpanded) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        holder.orderDate.setText("Pedido realizado em " + sdf.format(new Date(order.getTimestamp())));
        holder.orderIdValue.setText(order.getOrderId());

        int distinctProductCount = (order.getItems() != null) ? order.getItems().size() : 0;
        String summaryText = (distinctProductCount == 1) ? "Contém 1 produto" : "Contém " + distinctProductCount + " produtos";
        holder.orderSummary.setText(summaryText);

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
        holder.schedulingForm.setVisibility(View.GONE);
        holder.statusText.setVisibility(View.GONE);
        holder.cancelButton.setVisibility(View.GONE);
        holder.confirmScheduleButton.setText("Confirmar agendamento");
    }

    private void setupSchedulingControls(MyViewHolder holder, Order order) {
        holder.scheduleDate.setOnClickListener(v -> showDatePickerDialog(holder.scheduleDate));
        holder.scheduleTime.setOnClickListener(v -> showTimePickerDialog(holder.scheduleTime));
        holder.confirmScheduleButton.setOnClickListener(v -> {
            String date = holder.scheduleDate.getText().toString();
            String time = holder.scheduleTime.getText().toString();
            if (date.isEmpty() || time.isEmpty()) {
                Toast.makeText(context, "Por favor, selecione a data e o horário.", Toast.LENGTH_SHORT).show();
            } else {
                updateOrderSchedule(order, date, time);
            }
        });
    }

    private void cancelOrder(Order order) {
        DatabaseReference orderRef = getOrderReference(order);
        if (order.getItems() != null) {
            for (ItemDisplay item : order.getItems().values()) {
                returnStockForItem(item);
            }
        }
        orderRef.child("status").setValue("Pedido cancelado")
                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Pedido cancelado.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(context, "Falha ao cancelar o pedido.", Toast.LENGTH_SHORT).show());
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
                    Log.e("OngOrdersAdapter", "Stock return failed for item " + itemToReturn.getStockItemId() + ": " + error.getMessage());
                }
            }
        });
    }

    private void showDatePickerDialog(EditText dateEditText) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(context, (view, year, month, day) -> {
            String selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year);
            dateEditText.setText(selectedDate);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePickerDialog(EditText timeEditText) {
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(context, (view, hour, minute) -> {
            String selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
            timeEditText.setText(selectedTime);
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void updateOrderSchedule(Order order, String date, String time) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("scheduledDate", date);
        updates.put("scheduledTime", time);
        updates.put("status", "Data e horários determinados - Por favor, aguarde a liberação");

        getOrderReference(order).updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(context, "Agendamento enviado!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Falha ao enviar agendamento.", Toast.LENGTH_SHORT).show();
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
        TextView orderDate, orderSummary, statusText, confirmedSchedule, orderIdValue;
        LinearLayout schedulingForm, expandableLayout;
        EditText scheduleDate, scheduleTime;
        Button confirmScheduleButton;
        ImageButton cancelButton;
        RecyclerView innerRecyclerView;
        View mainInfoLayout;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            orderDate = itemView.findViewById(R.id.text_order_date);
            orderIdValue = itemView.findViewById(R.id.text_order_id_value);
            orderSummary = itemView.findViewById(R.id.text_order_summary);
            statusText = itemView.findViewById(R.id.text_order_status);
            confirmedSchedule = itemView.findViewById(R.id.text_confirmed_schedule);
            schedulingForm = itemView.findViewById(R.id.layout_scheduling_form);
            scheduleDate = itemView.findViewById(R.id.edit_text_schedule_date);
            scheduleTime = itemView.findViewById(R.id.edit_text_schedule_time);
            confirmScheduleButton = itemView.findViewById(R.id.button_confirm_schedule);
            cancelButton = itemView.findViewById(R.id.button_cancel_order);
            mainInfoLayout = itemView.findViewById(R.id.layout_main_info);
            expandableLayout = itemView.findViewById(R.id.layout_expandable_items);
            innerRecyclerView = itemView.findViewById(R.id.recycler_order_items_list);
        }
    }
}
