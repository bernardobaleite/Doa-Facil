package fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.company.doafacil.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import activity.OngActivity;
import adapter.OngProductAdapter;
import helper.ConfigurationFirebase;
import helper.UserFirebase;
import model.Order;
import model.OrderItem;
import model.Product;
import model.StockItem;

// RE-ARCH: Implementing data sanitization for robust aggregation.
public class OngListProductsFragment extends Fragment implements OngProductAdapter.ProductInteractionListener {

    public static class AggregatedStockItem {
        private final String expirationDate;
        private double totalQuantity;
        private final List<StockItem> originalItems = new ArrayList<>();

        public AggregatedStockItem(String expirationDate) {
            this.expirationDate = expirationDate;
        }
        public void addStockItem(StockItem item) {
            this.originalItems.add(item);
            this.totalQuantity += item.getStockItemQuantity();
        }
        public String getExpirationDate() { return expirationDate; }
        public double getTotalQuantity() { return totalQuantity; }
        public List<StockItem> getOriginalItems() { return originalItems; }
    }

    public static class GroupedStockItem {
        private final String productName;
        private final String productUnitType;
        private final List<AggregatedStockItem> aggregatedItems;

        public GroupedStockItem(String productName, String productUnitType, List<AggregatedStockItem> aggregatedItems) {
            this.productName = productName;
            this.productUnitType = productUnitType;
            this.aggregatedItems = aggregatedItems;
        }
        public String getProductName() { return productName; }
        public String getProductUnitType() { return productUnitType; }
        public List<AggregatedStockItem> getItems() { return aggregatedItems; }
    }

    private RecyclerView recyclerProducts;
    private TextView textEmptyProducts;
    private OngProductAdapter adapter;
    private List<GroupedStockItem> fullProductList = new ArrayList<>();
    private DatabaseReference databaseRef;
    private String currentOngId;

    private Map<String, Product> productCatalog = new HashMap<>();
    private Map<String, StockItem> physicalStock = new HashMap<>();
    private Map<String, Double> reservedQuantities = new HashMap<>();

    private ValueEventListener catalogListener, stockListener, ordersListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ong_products, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        initializeComponents(view);
        databaseRef = ConfigurationFirebase.getFirebaseDatabase();
        currentOngId = UserFirebase.getIdUser();
    }

    @Override
    public void onAddToCart(List<OrderItem> items) {
        if (currentOngId == null || currentOngId.isEmpty()) {
            Toast.makeText(getContext(), "Erro: Usuário não identificado.", Toast.LENGTH_LONG).show();
            return;
        }
        
        DatabaseReference cartRef = ConfigurationFirebase.getFirebaseDatabase().child("shopping_carts").child(currentOngId);
        Map<String, Object> cartUpdates = new HashMap<>();

        for (OrderItem item : items) {
            Product productDetails = productCatalog.get(item.getProductId());
            if (productDetails != null) {
                item.setProductName(productDetails.getProductName());
                item.setProductUnitType(productDetails.getProductUnitType());
            }
            cartUpdates.put(item.getStockItemId(), item);
        }

        cartRef.updateChildren(cartUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Itens adicionados ao carrinho!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Falha ao adicionar ao carrinho.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof OngActivity) {
            ((OngActivity) getActivity()).setToolbarTitle("Produtos Disponíveis", OngActivity.TitleAlignment.LEFT);
        }
        attachListeners();
    }

    @Override
    public void onStop() {
        super.onStop();
        detachListeners();
    }

    private void attachListeners() {
        detachListeners();

        catalogListener = databaseRef.child("establishment_product_catalog").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productCatalog.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Product p = ds.getValue(Product.class);
                    if (p != null) {
                        p.setProductId(ds.getKey());
                        productCatalog.put(p.getProductId(), p);
                    }
                }
                calculateAndDisplayProducts();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { /* Handle error */ }
        });

        stockListener = databaseRef.child("stock_items").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                physicalStock.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    StockItem si = ds.getValue(StockItem.class);
                    if (si != null) {
                        si.setStockItemId(ds.getKey());
                        physicalStock.put(si.getStockItemId(), si);
                    }
                }
                calculateAndDisplayProducts();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { /* Handle error */ }
        });

        ordersListener = databaseRef.child("orders").addValueEventListener(new ValueEventListener() {
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
                calculateAndDisplayProducts();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { /* Handle error */ }
        });
    }

    private void detachListeners() {
        if (catalogListener != null) databaseRef.child("establishment_product_catalog").removeEventListener(catalogListener);
        if (stockListener != null) databaseRef.child("stock_items").removeEventListener(stockListener);
        if (ordersListener != null) databaseRef.child("orders").removeEventListener(ordersListener);
    }

    private void calculateAndDisplayProducts() {
        if (!isAdded() || productCatalog.isEmpty() || physicalStock.isEmpty()) {
            return;
        }

        Map<String, List<StockItem>> availableStockByProduct = new HashMap<>();

        for (StockItem physicalItem : physicalStock.values()) {
            if (!"Disponível".equals(physicalItem.getStockItemStatus())) {
                continue;
            }
            
            double reserved = reservedQuantities.getOrDefault(physicalItem.getStockItemId(), 0.0);
            double available = physicalItem.getStockItemQuantity() - reserved;

            if (available > 0) {
                StockItem availableItem = new StockItem();
                availableItem.setStockItemId(physicalItem.getStockItemId());
                availableItem.setProductId(physicalItem.getProductId());
                availableItem.setStockItemExpirationDate(physicalItem.getStockItemExpirationDate());
                availableItem.setStockItemQuantity(available); 

                availableStockByProduct.computeIfAbsent(physicalItem.getProductId(), k -> new ArrayList<>()).add(availableItem);
            }
        }

        fullProductList.clear();
        for (Map.Entry<String, List<StockItem>> entry : availableStockByProduct.entrySet()) {
            String productId = entry.getKey();
            Product productDetails = productCatalog.get(productId);
            if (productDetails != null) {
                Map<String, AggregatedStockItem> aggregatedMap = new HashMap<>();
                for(StockItem item : entry.getValue()){
                    // THE FIX: Sanitize the key to prevent grouping failures due to whitespace.
                    String expirationDate = item.getStockItemExpirationDate() != null ? item.getStockItemExpirationDate().trim() : "";
                    aggregatedMap.computeIfAbsent(expirationDate, k -> new AggregatedStockItem(k)).addStockItem(item);
                }
                List<AggregatedStockItem> aggregatedList = new ArrayList<>(aggregatedMap.values());
                Collections.sort(aggregatedList, (o1, o2) -> o1.getExpirationDate().compareTo(o2.getExpirationDate()));

                fullProductList.add(new GroupedStockItem(
                    productDetails.getProductName(),
                    productDetails.getProductUnitType(),
                    aggregatedList
                ));
            }
        }
        
        Collections.sort(fullProductList, (o1, o2) -> o1.getProductName().compareToIgnoreCase(o2.getProductName()));
        
        adapter.setData(new ArrayList<>(fullProductList));
        updateViewVisibility(fullProductList.isEmpty());
    }

    private void initializeComponents(View view) {
        recyclerProducts = view.findViewById(R.id.recycler_products_list);
        textEmptyProducts = view.findViewById(R.id.text_empty_products);
        recyclerProducts.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerProducts.setHasFixedSize(false);
        adapter = new OngProductAdapter(requireActivity(), this);
        recyclerProducts.setAdapter(adapter);
    }

    private void filter(String text) {
        List<GroupedStockItem> filteredList = new ArrayList<>();
        if (text.isEmpty()) {
            filteredList.addAll(fullProductList);
        } else {
            text = text.toLowerCase(Locale.ROOT);
            for (GroupedStockItem group : fullProductList) {
                if (group.getProductName().toLowerCase(Locale.ROOT).contains(text)) {
                    filteredList.add(group);
                }
            }
        }
        adapter.setData(filteredList);
        updateViewVisibility(filteredList.isEmpty());
    }

    private void updateViewVisibility(boolean isEmpty) {
        if (isEmpty) {
            textEmptyProducts.setVisibility(View.VISIBLE);
            recyclerProducts.setVisibility(View.GONE);
        } else {
            textEmptyProducts.setVisibility(View.GONE);
            recyclerProducts.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar_menu_ong, menu);
        MenuItem searchItem = menu.findItem(R.id.menuSearch);
        SearchView searchView = (SearchView) searchItem.getActionView();

        if (searchView == null) return;

        searchView.setMaxWidth(Integer.MAX_VALUE);

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                if (getActivity() instanceof OngActivity) {
                    ((OngActivity) getActivity()).setToolbarTitle("", OngActivity.TitleAlignment.LEFT);
                }
                return true;
            }
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                if (getActivity() instanceof OngActivity) {
                    ((OngActivity) getActivity()).setToolbarTitle("Produtos Disponíveis", OngActivity.TitleAlignment.LEFT);
                }
                return true;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });

        MenuItem cartItem = menu.findItem(R.id.menuCart);
        cartItem.setOnMenuItemClickListener(item -> {
            if (getActivity() instanceof OngActivity) {
                ((OngActivity) getActivity()).navigateToCart();
            }
            return true;
        });
    }
}
