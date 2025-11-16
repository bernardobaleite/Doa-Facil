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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import activity.OngActivity;
import adapter.OngCartAdapter;
import helper.ConfigurationFirebase;
import helper.UserFirebase;
import model.GroupedProduct;
import model.Order;
import model.OrderItem;
import model.Product;
import model.StockItem;

// RE-ARCH: Implementing the final, unified cart view architecture.
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
    }

    public static class UnifiedCartItem {
        public final String expirationDate;
        public double totalQuantity = 0;
        public final String unitType;
        public final List<OrderItem> originalOrderItems = new ArrayList<>();

        public UnifiedCartItem(String expirationDate, String unitType) {
            this.expirationDate = expirationDate;
            this.unitType = unitType;
        }

        public void addCartDisplayItem(CartDisplayItem item) {
            this.originalOrderItems.add(item.orderItem);
            this.totalQuantity += item.orderItem.getOrderItemQuantity();
        }
    }

    private RecyclerView recyclerCart;
    private OngCartAdapter adapter;
    private FloatingActionButton fabConfirmOrder;

    private DatabaseReference dbRef;
    private ValueEventListener cartListener, catalogListener, stockListener, ordersListener;
    private String currentOngId;

    private List<OrderItem> cartPointers = new ArrayList<>();
    private Map<String, Product> productCatalogMap = new HashMap<>();
    private Map<String, StockItem> allStockItemsMap = new HashMap<>();
    private Map<String, Double> reservedQuantities = new HashMap<>();

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
        currentOngId = UserFirebase.getIdUser();

        fabConfirmOrder.setOnClickListener(v -> {
            if (cartPointers.isEmpty()) {
                Toast.makeText(getContext(), "Seu carrinho está vazio.", Toast.LENGTH_SHORT).show();
            } else {
                showConfirmOrderDialog();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof OngActivity) {
            ((OngActivity) getActivity()).setToolbarTitle("Carrinho", OngActivity.TitleAlignment.CENTER);
        }
        if (currentOngId != null) {
            attachListeners();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        detachListeners();
    }

    private void attachListeners() {
        detachListeners();
        catalogListener = dbRef.child("establishment_product_catalog").addValueEventListener(createCatalogListener());
        stockListener = dbRef.child("stock_items").addValueEventListener(createStockListener());
        ordersListener = dbRef.child("orders").addValueEventListener(createOrdersListener());
        cartListener = dbRef.child("shopping_carts").child(currentOngId).addValueEventListener(createCartListener());
    }

    private void detachListeners() {
        if (catalogListener != null) dbRef.child("establishment_product_catalog").removeEventListener(catalogListener);
        if (stockListener != null) dbRef.child("stock_items").removeEventListener(stockListener);
        if (ordersListener != null) dbRef.child("orders").removeEventListener(ordersListener);
        if (cartListener != null && currentOngId != null) dbRef.child("shopping_carts").child(currentOngId).removeEventListener(cartListener);
    }

    private ValueEventListener createCatalogListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productCatalogMap.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Product p = ds.getValue(Product.class);
                    if (p != null) {
                        p.setProductId(ds.getKey());
                        productCatalogMap.put(p.getProductId(), p);
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
                        item.setStockItemId(ds.getKey());
                        allStockItemsMap.put(ds.getKey(), item);
                    }
                }
                combineDataAndRefreshUI();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { showToast("Falha ao carregar estoque."); }
        };
    }
    
    private ValueEventListener createOrdersListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reservedQuantities.clear();
                 for (DataSnapshot ongDs : snapshot.getChildren()) {
                    for (DataSnapshot orderDs : ongDs.getChildren()) {
                        Order order = orderDs.getValue(Order.class);
                        if (order != null && (order.getOrderStatus().equals("Realize o agendamento") || order.getOrderStatus().equals("Realize um novo agendamento") || order.getOrderStatus().equals("Data e horários determinados - Por favor, aguarde a liberação"))) {
                            if (order.getOrderItems() != null) {
                                for (OrderItem item : order.getOrderItems().values()) {
                                    reservedQuantities.merge(item.getStockItemId(), item.getOrderItemQuantity(), Double::sum);
                                }
                            }
                        }
                    }
                }
                combineDataAndRefreshUI();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { /* Handle error */ }
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
            return;
        }

        List<CartDisplayItem> displayItems = new ArrayList<>();
        List<String> itemsToRemoveFromCart = new ArrayList<>();

        for (OrderItem pointer : cartPointers) {
            if (pointer == null || pointer.getStockItemId() == null) continue;

            StockItem physicalItem = allStockItemsMap.get(pointer.getStockItemId());
            if (physicalItem != null) {
                Product productDetails = productCatalogMap.get(physicalItem.getProductId());
                double reserved = reservedQuantities.getOrDefault(physicalItem.getStockItemId(), 0.0);
                double available = physicalItem.getStockItemQuantity() - reserved;

                if ("Disponível".equals(physicalItem.getStockItemStatus()) && available >= pointer.getOrderItemQuantity()) {
                    displayItems.add(new CartDisplayItem(physicalItem, pointer, productDetails));
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
        }
    }

    private void updateAdapter(List<CartDisplayItem> items) {
        fabConfirmOrder.setVisibility(!items.isEmpty() ? View.VISIBLE : View.GONE);

        Map<String, List<CartDisplayItem>> itemsByProduct = new HashMap<>();
        for (CartDisplayItem item : items) {
            itemsByProduct.computeIfAbsent(item.orderItem.getProductName(), k -> new ArrayList<>()).add(item);
        }

        List<GroupedProduct<UnifiedCartItem>> finalList = new ArrayList<>();

        for (Map.Entry<String, List<CartDisplayItem>> entry : itemsByProduct.entrySet()) {
            String productName = entry.getKey();
            List<CartDisplayItem> productItems = entry.getValue();

            if (productItems.isEmpty()) continue;

            final String unitType = productItems.get(0).productDetails != null ? productItems.get(0).productDetails.getProductUnitType() : "";
            
            Map<String, UnifiedCartItem> unifiedByDate = new HashMap<>();

            for (CartDisplayItem item : productItems) {
                // THE FIX: Sanitize the expiration date key, just like in the other fragment.
                String expiration = item.stockItem.getStockItemExpirationDate() != null ? item.stockItem.getStockItemExpirationDate().trim() : "";
                UnifiedCartItem unifiedItem = unifiedByDate.computeIfAbsent(expiration, k -> new UnifiedCartItem(k, unitType));
                unifiedItem.addCartDisplayItem(item);
            }

            List<UnifiedCartItem> unifiedList = new ArrayList<>(unifiedByDate.values());
            finalList.add(new GroupedProduct<>(productName, unifiedList));
        }
        
        Collections.sort(finalList, (o1, o2) -> o1.getProductName().compareToIgnoreCase(o2.getProductName()));
        adapter.setData(finalList);
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
                .setMessage("Deseja confirmar o seu pedido?")
                .setPositiveButton("Confirmar", (dialog, which) -> createOrderAndClearCart())
                .setNegativeButton("Cancelar", null)
                .show();
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
    
    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }
}
