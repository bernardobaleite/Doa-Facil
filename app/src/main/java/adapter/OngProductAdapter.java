package adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.company.doafacil.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import model.GroupedProduct;
import model.ItemDisplay;

// RE-FIX: Applying the consistent ID naming to the product adapter.
public class OngProductAdapter extends RecyclerView.Adapter<OngProductAdapter.MyViewHolder> {

    private List<GroupedProduct> productList;
    private Context context;
    private final ProductInteractionListener listener;
    private int expandedPosition = -1;

    private final List<Integer> placeholderImages;

    public interface ProductInteractionListener {
        void onAddToCart(ItemDisplay item, int quantity);
    }

    public OngProductAdapter(Context context, ProductInteractionListener listener) {
        this.productList = new ArrayList<>();
        this.context = context;
        this.listener = listener;
        setHasStableIds(true);

        placeholderImages = Arrays.asList(
                R.drawable.food_placeholder,
                R.drawable.food_placeholder2,
                R.drawable.food_placeholder3
        );
    }

    @Override
    public long getItemId(int position) {
        if (position >= 0 && position < productList.size()) {
            return productList.get(position).getProductName().hashCode();
        }
        return RecyclerView.NO_ID;
    }

    public void setData(List<GroupedProduct> productList) {
        this.productList = productList;
        expandedPosition = -1;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_ong_product_list_item, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        GroupedProduct groupedProduct = productList.get(position);
        final boolean isExpanded = position == expandedPosition;

        holder.productName.setText(groupedProduct.getProductName());

        double totalQuantity = 0;
        String unitType = "";
        if (!groupedProduct.getItems().isEmpty()) {
            unitType = groupedProduct.getItems().get(0).getProductUnitType();
            if (unitType == null) unitType = "";
            for (ItemDisplay item : groupedProduct.getItems()) {
                totalQuantity += item.getStockItemQuantity();
            }
        }
        DecimalFormat formatter = new DecimalFormat("0.##");
        String summaryText = "Possui " + formatter.format(totalQuantity) + " " + unitType.trim() + " no total";
        
        // THE FIX: Use the correct TextView variable.
        holder.productSummary.setText(summaryText);

        int imageIndex = Math.abs(groupedProduct.getProductName().hashCode()) % placeholderImages.size();
        int deterministicImageId = placeholderImages.get(imageIndex);
        holder.productImage.setImageDrawable(ContextCompat.getDrawable(context, deterministicImageId));

        holder.expandableLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        OngProductItemAdapter subAdapter = new OngProductItemAdapter(
                groupedProduct.getItems(),
                item -> showQuantityDialog(item, holder.getBindingAdapterPosition())
        );
        holder.originsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        holder.originsRecyclerView.setAdapter(subAdapter);
        holder.originsRecyclerView.setNestedScrollingEnabled(false);

        holder.itemView.setOnClickListener(v -> {
            int clickedPosition = holder.getBindingAdapterPosition();
            if (clickedPosition == RecyclerView.NO_POSITION) return;

            int previousExpanded = expandedPosition;
            expandedPosition = isExpanded ? -1 : clickedPosition;

            if (previousExpanded != -1) notifyItemChanged(previousExpanded);
            notifyItemChanged(clickedPosition);
        });
    }

    private void showQuantityDialog(ItemDisplay item, int adapterPosition) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_quantity, null);
        builder.setView(dialogView);

        final EditText editTextQuantity = dialogView.findViewById(R.id.edit_text_dialog_quantity);
        Button buttonCancel = dialogView.findViewById(R.id.button_dialog_cancel);
        Button buttonConfirm = dialogView.findViewById(R.id.button_dialog_confirm);

        final AlertDialog dialog = builder.create();

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        buttonConfirm.setOnClickListener(v -> {
            String quantityStr = editTextQuantity.getText().toString();
            if (quantityStr.isEmpty()) {
                Toast.makeText(context, "Por favor, insira uma quantidade.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                double quantity = Double.parseDouble(quantityStr);
                if (quantity <= 0 || quantity > item.getStockItemQuantity()) {
                     Toast.makeText(context, "Quantidade inválida ou indisponível.", Toast.LENGTH_SHORT).show();
                     return;
                }
                
                if (listener != null) {
                    listener.onAddToCart(item, (int) quantity);
                }

                if (adapterPosition != RecyclerView.NO_POSITION) {
                    int previousExpanded = expandedPosition;
                    expandedPosition = -1; // Collapse after adding
                    if (previousExpanded != -1) {
                        notifyItemChanged(previousExpanded);
                    }
                }
                dialog.dismiss();

            } catch (NumberFormatException e) {
                Toast.makeText(context, "Quantidade inválida.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, productSummary;
        RecyclerView originsRecyclerView;
        LinearLayout expandableLayout;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            productName = itemView.findViewById(R.id.product_name);
            productSummary = itemView.findViewById(R.id.product_summary);
            originsRecyclerView = itemView.findViewById(R.id.origins_recycler_view);
            expandableLayout = itemView.findViewById(R.id.expandable_layout);
        }
    }
}
