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
import model.OrderItem;
import model.Product;
import model.StockItem;

// RE-ARCH: Fixing the fragment lifecycle to ensure data is always fresh.
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
    private ValueEventListener stockItemsListener;
    private Map<String, Product> productCatalogMap = new HashMap<>();
    private String currentOngId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ong_products, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        recyclerProducts = view.findViewById(R.id.recycler_products_list);
        textEmptyProducts = view.findViewById(R.id.text_empty_products);
        databaseRef = ConfigurationFirebase.getFirebaseDatabase();

        recyclerProducts.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerProducts.setHasFixedSize(false);

        adapter = new OngProductAdapter(requireActivity(), this);
        recyclerProducts.setAdapter(adapter);

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
            Product productDetails = productCatalogMap.get(item.getProductId());
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
    
    private void loadProductCatalog() {
        DatabaseReference catalogRef = databaseRef.child("establishment_product_catalog");
        catalogRef.addListenerForSingleValueEvent(new ValueEventListener() {
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
                listenForAvailableItems();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) Toast.makeText(getContext(), "Falha ao carregar catálogo: " + error.getMessage(), Toast.LENGTH_LONG).show();
                updateViewVisibility(true);
            }
        });
    }

    private void listenForAvailableItems() {
        DatabaseReference stockRef = databaseRef.child("stock_items");
        if (stockItemsListener != null) { 
            stockRef.removeEventListener(stockItemsListener);
        }
        stockItemsListener = stockRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, List<StockItem>> groupedByProduct = new HashMap<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    StockItem stockItem = ds.getValue(StockItem.class);
                    if (stockItem != null && "Disponível".equals(stockItem.getStockItemStatus()) && stockItem.getStockItemQuantity() > 0) {
                        groupedByProduct.computeIfAbsent(stockItem.getProductId(), k -> new ArrayList<>()).add(stockItem);
                    }
                }

                fullProductList.clear();
                for (Map.Entry<String, List<StockItem>> entry : groupedByProduct.entrySet()) {
                    String productId = entry.getKey();
                    Product productDetails = productCatalogMap.get(productId);
                    if (productDetails != null) {
                        Map<String, AggregatedStockItem> aggregatedMap = new HashMap<>();
                        for(StockItem item : entry.getValue()){
                            String expirationDate = item.getStockItemExpirationDate();
                            aggregatedMap.computeIfAbsent(expirationDate, k -> new AggregatedStockItem(k)).addStockItem(item);
                        }

                        List<AggregatedStockItem> aggregatedList = new ArrayList<>(aggregatedMap.values());
                        Collections.sort(aggregatedList, (o1, o2) -> o1.getExpirationDate().compareTo(o2.getExpirationDate()));

                        GroupedStockItem newGroup = new GroupedStockItem(
                            productDetails.getProductName(),
                            productDetails.getProductUnitType(),
                            aggregatedList
                        );
                        fullProductList.add(newGroup);
                    }
                }
                
                Collections.sort(fullProductList, (o1, o2) -> o1.getProductName().compareToIgnoreCase(o2.getProductName()));
                
                adapter.setData(new ArrayList<>(fullProductList));
                updateViewVisibility(fullProductList.isEmpty());
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if(getContext() != null) Toast.makeText(getContext(), "Falha ao carregar estoque: " + error.getMessage(), Toast.LENGTH_LONG).show();
                updateViewVisibility(true);
            }
        });
    }

    // THE FIX: Attach listeners in onResume to get fresh data.
    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof OngActivity) {
            ((OngActivity) getActivity()).setToolbarTitle("Produtos Disponíveis", OngActivity.TitleAlignment.LEFT);
        }
        if (currentOngId != null) {
            loadProductCatalog(); // Starts the listener chain
        } else {
            Toast.makeText(getContext(), "Erro: Usuário não autenticado.", Toast.LENGTH_LONG).show();
            updateViewVisibility(true);
        }
    }

    // THE FIX: Detach listeners in onStop to prevent leaks and unnecessary background updates.
    @Override
    public void onStop() {
        super.onStop();
        if (stockItemsListener != null && databaseRef != null) {
            databaseRef.child("stock_items").removeEventListener(stockItemsListener);
        }
        // Note: The catalog listener is a single-value-event listener and does not need to be removed.
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
