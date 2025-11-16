package adapter;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
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
import com.google.firebase.database.DatabaseReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import helper.ConfigurationFirebase;
import model.Order;
import model.OrderItem;

// RE-ARCH: Aligning with the final "Calculated Truth" architecture.
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

        String status = order.getOrderStatus();
        int statusColor;

        switch (status) {
            case "Realize o agendamento":
            case "Realize um novo agendamento":
                holder.schedulingForm.setVisibility(View.VISIBLE);
                holder.cancelButton.setVisibility(View.VISIBLE);
                holder.cancelButton.setOnClickListener(v -> cancelOrder(order)); 
                setupSchedulingControls(holder, order);
                break; 

            case "Data e horários determinados - Por favor, aguarde a liberação":
                statusColor = ContextCompat.getColor(context, R.color.status_warning_yellow);
                holder.statusText.setText(status);
                holder.statusText.setTextColor(statusColor);
                holder.statusText.setVisibility(View.VISIBLE);
                break;
                
            case "Seu pedido já está disponível - Retire sua doação":
            case "Doação distribuída":
                statusColor = ContextCompat.getColor(context, R.color.watergreen);
                holder.statusText.setText(status);
                holder.statusText.setTextColor(statusColor);
                holder.statusText.setVisibility(View.VISIBLE);
                break;

            case "Pedido cancelado":
            case "Produto sem estoque/vencido":
                statusColor = ContextCompat.getColor(context, R.color.status_red);
                holder.statusText.setText(status);
                holder.statusText.setTextColor(statusColor);
                holder.statusText.setVisibility(View.VISIBLE);
                break;

            default:
                statusColor = ContextCompat.getColor(context, R.color.grey);
                holder.statusText.setText(status);
                holder.statusText.setTextColor(statusColor);
                holder.statusText.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void setupCommonInfo(MyViewHolder holder, Order order, boolean isExpanded) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.orderDate.setText("Pedido realizado em " + sdf.format(new Date(order.getOrderCreatedAt())));
        holder.orderIdValue.setText(order.getOrderId());

        int distinctProductCount = (order.getOrderItems() != null) ? order.getOrderItems().size() : 0;
        String summaryText = (distinctProductCount == 1) ? "Contém 1 tipo de produto" : "Contém " + distinctProductCount + " tipos de produto";
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

        if (isExpanded && order.getOrderItems() != null) {
            List<OrderItem> items = new ArrayList<>(order.getOrderItems().values());
            OrderProductItemAdapter subAdapter = new OrderProductItemAdapter(items, context);
            holder.innerRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            holder.innerRecyclerView.setAdapter(subAdapter);
        }
    }

    private void resetStateViews(MyViewHolder holder) {
        holder.schedulingForm.setVisibility(View.GONE);
        holder.statusText.setVisibility(View.GONE);
        holder.cancelButton.setVisibility(View.GONE);
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
                updateOrderSchedule(order, date + " " + time);
            }
        });
    }

    // THE FIX: Cancellation just changes the order status. The availability is recalculated automatically by the listener in OngListProductsFragment.
    private void cancelOrder(Order order) {
        updateOrderStatus(order, "Pedido cancelado", null);
    }

    private void updateOrderStatus(Order order, String newStatus, @Nullable Runnable onComplete) {
        getOrderReference(order).child("orderStatus").setValue(newStatus).addOnCompleteListener(task -> {
             if (task.isSuccessful()) {
                Toast.makeText(context, "Status atualizado para: " + newStatus, Toast.LENGTH_SHORT).show();
                 if(onComplete != null) onComplete.run();
            } else {
                Toast.makeText(context, "Falha ao atualizar status.", Toast.LENGTH_SHORT).show();
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

    private void updateOrderSchedule(Order order, String dateTime) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("orderScheduledDateTime", dateTime);
        updates.put("orderStatus", "Data e horários determinados - Por favor, aguarde a liberação");

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
