package fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.company.doafacil.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import activity.OngActivity;
import adapter.OngCartAdapter;
import helper.ConfigurationFirebase;
import helper.UserFirebase;
import model.GroupedProduct;
import model.Order;
import model.OrderItem;
import model.Product;
import model.StockItem;

// RE-ARCH: Implementing a fully reactive, multi-listener architecture to prevent race conditions.
public class OngCartFragment extends Fragment {

    public static class CartDisplayItem {
        public final StockItem stockItem;
        public final OrderItem orderItem;
        public final Product productDetails;

        public CartDisplayItem(StockItem stockItem, OrderItem orderItem, Product productDetails) {
            this.stockItem = stockItem;
            this.orderItem = orderItem;
            this.productDetails = productDetails;
        }

        public String getProductName() {
            return productDetails != null ? productDetails.getProductName() : "Produto Desconhecido";
        }
    }

    private RecyclerView recyclerCart;
    private OngCartAdapter adapter;
    private FloatingActionButton fabConfirmOrder;

    private DatabaseReference dbRef;
    private ValueEventListener cartListener, catalogListener, stockListener;
    private String currentOngId;

    private List<OrderItem> cartPointers = new ArrayList<>();
    private Map<String, Product> productCatalogMap = new HashMap<>();
    private Map<String, StockItem> allStockItemsMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ong_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeComponents(view);
        dbRef = ConfigurationFirebase.getFirebaseDatabase();

        fabConfirmOrder.setOnClickListener(v -> {
            if (cartPointers.isEmpty()) {
                Toast.makeText(getContext(), "Seu carrinho está vazio.", Toast.LENGTH_SHORT).show();
            } else {
                showConfirmOrderDialog();
            }
        });

        currentOngId = UserFirebase.getIdUser();
        if (currentOngId != null) {
            attachListeners();
        }
    }

    private void attachListeners() {
        // Attach persistent listeners to all data sources.
        catalogListener = dbRef.child("establishment_product_catalog").addValueEventListener(createCatalogListener());
        stockListener = dbRef.child("stock_items").addValueEventListener(createStockListener());
        cartListener = dbRef.child("shopping_carts").child(currentOngId).addValueEventListener(createCartListener());
    }

    private ValueEventListener createCatalogListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productCatalogMap.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Product product = ds.getValue(Product.class);
                    if (product != null) {
                        product.setProductId(ds.getKey());
                        productCatalogMap.put(product.getProductId(), product);
                    }
                }
                combineDataAndRefreshUI();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { showToast("Falha ao carregar catálogo."); }
        };
    }

    private ValueEventListener createStockListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allStockItemsMap.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    StockItem item = ds.getValue(StockItem.class);
                    if (item != null) {
                        allStockItemsMap.put(ds.getKey(), item);
                    }
                }
                combineDataAndRefreshUI();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { showToast("Falha ao carregar estoque."); }
        };
    }

    private ValueEventListener createCartListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cartPointers.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    cartPointers.add(ds.getValue(OrderItem.class));
                }
                combineDataAndRefreshUI();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { showToast("Falha ao carregar carrinho."); }
        };
    }

    private void combineDataAndRefreshUI() {
        if (!isAdded() || productCatalogMap.isEmpty() || allStockItemsMap.isEmpty()) {
            return; // Wait for all data sources to be loaded at least once.
        }

        List<CartDisplayItem> displayItems = new ArrayList<>();
        List<String> itemsToRemoveFromCart = new ArrayList<>();

        for (OrderItem pointer : cartPointers) {
            if(pointer == null || pointer.getStockItemId() == null) continue;

            StockItem stockItem = allStockItemsMap.get(pointer.getStockItemId());

            if (stockItem != null) {
                Product productDetails = productCatalogMap.get(stockItem.getProductId());
                if ("Disponível".equals(stockItem.getStockItemStatus()) && stockItem.getStockItemQuantity() >= pointer.getOrderItemQuantity()) {
                    displayItems.add(new CartDisplayItem(stockItem, pointer, productDetails));
                } else {
                    itemsToRemoveFromCart.add(pointer.getStockItemId());
                }
            } else {
                itemsToRemoveFromCart.add(pointer.getStockItemId());
            }
        }

        updateAdapter(displayItems);

        if (!itemsToRemoveFromCart.isEmpty()) {
            DatabaseReference cartRef = dbRef.child("shopping_carts").child(currentOngId);
            Map<String, Object> updates = new HashMap<>();
            for (String itemIdToRemove : itemsToRemoveFromCart) {
                updates.put(itemIdToRemove, null);
            }
            cartRef.updateChildren(updates);
            showToast("Alguns itens não estão mais disponíveis e foram removidos.");
        }
    }

    private void updateAdapter(List<CartDisplayItem> items) {
        fabConfirmOrder.setVisibility(!items.isEmpty() ? View.VISIBLE : View.GONE);
        Map<String, GroupedProduct<CartDisplayItem>> groupedMap = new HashMap<>();
        for (CartDisplayItem item : items) {
            GroupedProduct<CartDisplayItem> group = groupedMap.computeIfAbsent(item.getProductName(), k -> new GroupedProduct<>(item.getProductName(), new ArrayList<>()));
            group.getItems().add(item);
        }
        List<GroupedProduct<CartDisplayItem>> groupedList = new ArrayList<>(groupedMap.values());
        Collections.sort(groupedList, (o1, o2) -> o1.getProductName().compareToIgnoreCase(o2.getProductName()));
        adapter.setData(groupedList);
    }
    
    // ... (Transaction logic remains the same) ...

    @Override
    public void onStop() {
        super.onStop();
        // Detach all listeners to prevent memory leaks.
        if (cartListener != null) dbRef.child("shopping_carts").child(currentOngId).removeEventListener(cartListener);
        if (catalogListener != null) dbRef.child("establishment_product_catalog").removeEventListener(catalogListener);
        if (stockListener != null) dbRef.child("stock_items").removeEventListener(stockListener);
    }
    
    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    private void initializeComponents(View view) {
        recyclerCart = view.findViewById(R.id.recycler_cart_items);
        fabConfirmOrder = view.findViewById(R.id.fab_confirm_order);
        recyclerCart.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OngCartAdapter(getContext(), new ArrayList<>());
        recyclerCart.setAdapter(adapter);
    }

    private void showConfirmOrderDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar Pedido")
                .setMessage("Os itens do pedido serão deduzidos do estoque. Deseja confirmar?")
                .setPositiveButton("Confirmar", (dialog, which) -> runStockDeductionTransactions())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void runStockDeductionTransactions() {
        final int totalItemsToProcess = cartPointers.size();
        if (totalItemsToProcess == 0) return;

        final AtomicInteger successCounter = new AtomicInteger(0);
        final List<String> failedItemIds = new ArrayList<>();

        for (OrderItem pointer : cartPointers) {
            DatabaseReference stockItemRef = dbRef.child("stock_items").child(pointer.getStockItemId());

            stockItemRef.runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                    StockItem stockItem = mutableData.getValue(StockItem.class);
                    if (stockItem == null || !"Disponível".equals(stockItem.getStockItemStatus())) {
                        return Transaction.abort();
                    }
                    double currentQuantity = stockItem.getStockItemQuantity();
                    double requestedQuantity = pointer.getOrderItemQuantity();

                    if (currentQuantity < requestedQuantity) {
                        return Transaction.abort();
                    }
                    stockItem.setStockItemQuantity(currentQuantity - requestedQuantity);
                    mutableData.setValue(stockItem);
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot dataSnapshot) {
                    if (!committed) {
                        failedItemIds.add(pointer.getStockItemId());
                    }
                    if (successCounter.incrementAndGet() == totalItemsToProcess) {
                        onAllTransactionsComplete(failedItemIds);
                    }
                }
            });
        }
    }

    private void onAllTransactionsComplete(List<String> failedItemIds) {
        if (!failedItemIds.isEmpty()) {
            DatabaseReference cartRef = dbRef.child("shopping_carts").child(currentOngId);
            for(String failedId : failedItemIds) {
                cartRef.child(failedId).removeValue();
            }
            showToast("Alguns itens não estão mais disponíveis e foram removidos do seu carrinho.");
        } else {
            createOrderAndClearCart();
        }
    }

    private void createOrderAndClearCart() {
        String orderId = dbRef.child("orders").child(currentOngId).push().getKey();
        if (orderId == null) { return; }

        Order newOrder = new Order();
        newOrder.setOrderId(orderId);
        newOrder.setOngId(currentOngId);
        newOrder.setOrderStatus("Realize o agendamento");
        newOrder.setOrderCreatedAt(System.currentTimeMillis());

        Map<String, OrderItem> itemsForOrder = new HashMap<>();
        for (OrderItem item : cartPointers) {
            itemsForOrder.put(item.getStockItemId(), item);
        }
        newOrder.setOrderItems(itemsForOrder);

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/orders/" + currentOngId + "/" + orderId, newOrder);
        childUpdates.put("/shopping_carts/" + currentOngId, null);

        dbRef.updateChildren(childUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                showToast("Pedido criado com sucesso!");
                if (getActivity() instanceof OngActivity) {
                    ((OngActivity) getActivity()).navigateToOrders();
                }
            } else {
                showToast("Falha ao salvar o pedido.");
            }
        });
    }
}
