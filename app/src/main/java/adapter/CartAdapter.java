package adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import helper.ConfigurationFirebase;
import helper.UserFirebase;
import model.GroupedProduct;
import model.ItemDisplay;
import model.StockItem;

// RE-FIX: Correcting the renamed view ID to prevent the crash and align with the layout file.
public class CartAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_GROUP = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private List<Object> displayList = new ArrayList<>();
    private Context context;
    private String currentOngId;

    private final List<Integer> placeholderImages;

    public CartAdapter(Context context) {
        this.context = context;
        this.currentOngId = UserFirebase.getIdUser();

        placeholderImages = Arrays.asList(
                R.drawable.food_placeholder,
                R.drawable.food_placeholder2,
                R.drawable.food_placeholder3
        );
    }

    public void setData(List<GroupedProduct> groupedProducts) {
        displayList.clear();
        for (GroupedProduct group : groupedProducts) {
            displayList.add(group);
            displayList.addAll(group.getItems());
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (displayList.get(position) instanceof GroupedProduct) {
            return VIEW_TYPE_GROUP;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_GROUP) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_ong_cart_list_item, parent, false);
            return new GroupViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_ong_cart_individual_item, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_GROUP) {
            ((GroupViewHolder) holder).bind((GroupedProduct) displayList.get(position));
        } else {
            ((ItemViewHolder) holder).bind((ItemDisplay) displayList.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    // --- GROUP VIEWHOLDER ---
    class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView name, productSummary;
        ImageView productImage;

        GroupViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.product_name);
            // THE FIX: Use the new, correct ID from the layout file.
            productSummary = itemView.findViewById(R.id.product_summary);
            productImage = itemView.findViewById(R.id.product_image);
        }

        void bind(GroupedProduct group) {
            name.setText(group.getProductName());

            // THE FIX: Calculate total quantity of THIS product in the cart.
            double totalQuantityInCart = 0;
            String unitType = "";
            if (group.getItems() != null && !group.getItems().isEmpty()) {
                unitType = group.getItems().get(0).getProductUnitType();
                if (unitType == null) unitType = "";
                for (ItemDisplay item : group.getItems()) {
                    totalQuantityInCart += item.getStockItemQuantity();
                }
            }
            DecimalFormat formatter = new DecimalFormat("0.##");
            String summaryText = "Você tem " + formatter.format(totalQuantityInCart) + " " + unitType.trim() + " no carrinho";
            productSummary.setText(summaryText);

            int imageIndex = Math.abs(group.getProductName().hashCode()) % placeholderImages.size();
            int deterministicImageId = placeholderImages.get(imageIndex);
            productImage.setImageDrawable(ContextCompat.getDrawable(context, deterministicImageId));
        }
    }

    // --- ITEM VIEWHOLDER ---
    class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView expirationDate, quantity;
        ImageButton actionButton;

        ItemViewHolder(View itemView) {
            super(itemView);
            expirationDate = itemView.findViewById(R.id.text_individual_expiration);
            quantity = itemView.findViewById(R.id.text_individual_quantity);
            actionButton = itemView.findViewById(R.id.button_add_individual_to_cart);
        }

        void bind(ItemDisplay item) {
            DecimalFormat formatter = new DecimalFormat("0.##");
            String formattedQuantity = formatter.format(item.getStockItemQuantity());
            expirationDate.setText("Validade: " + item.getStockItemExpirationDate());
            quantity.setText("Qtd: " + formattedQuantity);

            actionButton.setImageResource(R.drawable.ic_cancel_24dp);
            actionButton.setColorFilter(ContextCompat.getColor(context, R.color.status_red));
            actionButton.setOnClickListener(v -> removeItemAndReturnStock(item));
        }
    }

    // --- LOGIC FOR REMOVAL AND STOCK RETURN (Preserved) ---
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
                StockItem currentItem = mutableData.getValue(StockItem.class);
                double quantityToReturn = cartItem.getStockItemQuantity();

                if (currentItem == null) {
                    StockItem recreatedItem = new StockItem();
                    recreatedItem.setStockItemId(cartItem.getStockItemId());
                    recreatedItem.setEstablishmentId(cartItem.getEstablishmentId());
                    recreatedItem.setProductId(cartItem.getProductId());
                    recreatedItem.setStockItemQuantity(quantityToReturn);
                    recreatedItem.setStockItemExpirationDate(cartItem.getStockItemExpirationDate());
                    recreatedItem.setStockItemStatus("Doação Coletada");
                    mutableData.setValue(recreatedItem);
                } else {
                    double newQuantity = currentItem.getStockItemQuantity() + quantityToReturn;
                    currentItem.setStockItemQuantity(newQuantity);
                    mutableData.setValue(currentItem);
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Toast.makeText(context, "Erro ao devolver item: " + error.getMessage(), Toast.LENGTH_LONG).show();
                } else if (committed) {
                    removeCartItemFromDatabase(cartItem);
                } else {
                     Toast.makeText(context, "Não foi possível devolver o item.", Toast.LENGTH_SHORT).show();
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
