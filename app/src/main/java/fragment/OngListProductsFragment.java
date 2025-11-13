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
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
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
import model.GroupedProduct;
import model.ItemDisplay;
import model.Product;
import model.StockItem;

// RE-ARCH: Reverting the toolbar icon to be a shortcut to the cart tab.
public class OngListProductsFragment extends Fragment implements OngProductAdapter.ProductInteractionListener {

    private RecyclerView recyclerProducts;
    private TextView textEmptyProducts;
    private OngProductAdapter adapter;
    private List<GroupedProduct> fullProductList = new ArrayList<>();
    private DatabaseReference databaseRef;
    private ValueEventListener stockItemsListener;
    private Map<String, Product> productCatalogMap = new HashMap<>();

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

        loadProductCatalog();
    }

    @Override
    public void onAddToCart(ItemDisplay item, int quantity) {
        String ongId = UserFirebase.getIdUser();
        if (ongId == null || ongId.isEmpty()) {
            Toast.makeText(getContext(), "Erro: Usuário não identificado. Faça login novamente.", Toast.LENGTH_LONG).show();
            return;
        }

        DatabaseReference stockItemRef = ConfigurationFirebase.getFirebaseDatabase()
                .child("stock_items")
                .child(item.getEstablishmentId())
                .child(item.getStockItemId());

        stockItemRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                StockItem stockItem = mutableData.getValue(StockItem.class);
                if (stockItem == null || stockItem.getStockItemQuantity() < quantity) {
                    return Transaction.abort();
                }

                double newQuantity = stockItem.getStockItemQuantity() - quantity;
                if (newQuantity > 0) {
                    stockItem.setStockItemQuantity(newQuantity);
                    mutableData.setValue(stockItem);
                } else {
                    mutableData.setValue(null);
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Toast.makeText(getContext(), "Falha ao atualizar o estoque: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                } else if (committed) {
                    DatabaseReference cartItemRef = ConfigurationFirebase.getFirebaseDatabase()
                            .child("shopping_carts")
                            .child(ongId)
                            .child(item.getStockItemId());

                    cartItemRef.runTransaction(new Transaction.Handler() {
                        @NonNull
                        @Override
                        public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                            ItemDisplay currentCartItem = mutableData.getValue(ItemDisplay.class);
                            if (currentCartItem == null) {
                                ItemDisplay newCartItem = item;
                                newCartItem.setStockItemQuantity(quantity);
                                mutableData.setValue(newCartItem);
                            } else {
                                double newTotalQuantity = currentCartItem.getStockItemQuantity() + quantity;
                                currentCartItem.setStockItemQuantity(newTotalQuantity);
                                mutableData.setValue(currentCartItem);
                            }
                            return Transaction.success(mutableData);
                        }

                        @Override
                        public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                            if (committed) {
                                Toast.makeText(getContext(), "Carrinho atualizado!", Toast.LENGTH_SHORT).show();
                            } else if (error != null) {
                                Toast.makeText(getContext(), "Falha ao atualizar carrinho: " + error.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                } else {
                    Toast.makeText(getContext(), "Conflito de estoque. Tente novamente.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof OngActivity) {
            ((OngActivity) getActivity()).setToolbarTitle("Produtos Disponíveis", OngActivity.TitleAlignment.LEFT);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (stockItemsListener != null && databaseRef != null) {
            databaseRef.child("stock_items").removeEventListener(stockItemsListener);
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
        // THE FIX: The toolbar icon is now a shortcut to the cart tab.
        cartItem.setOnMenuItemClickListener(item -> {
            if (getActivity() instanceof OngActivity) {
                ((OngActivity) getActivity()).navigateToCart();
            }
            return true;
        });
    }

    private void filter(String text) {
        List<GroupedProduct> filteredList = new ArrayList<>();
        if (text.isEmpty()) {
            filteredList.addAll(fullProductList);
        } else {
            text = text.toLowerCase(Locale.ROOT);
            for (GroupedProduct group : fullProductList) {
                if (group.getProductName().toLowerCase(Locale.ROOT).contains(text)) {
                    filteredList.add(group);
                }
            }
        }
        adapter.setData(filteredList);
        updateViewVisibility(filteredList.isEmpty());
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
                        productCatalogMap.put(ds.getKey(), product);
                    }
                }
                listenForAvailableItems();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) Toast.makeText(getContext(), "Falha ao carregar catálogo.", Toast.LENGTH_SHORT).show();
                updateViewVisibility(true);
            }
        });
    }

    private void listenForAvailableItems() {
        DatabaseReference stockRef = databaseRef.child("stock_items");
        stockItemsListener = stockRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, List<ItemDisplay>> groupedMap = new HashMap<>();

                for (DataSnapshot userDs : snapshot.getChildren()) {
                    for (DataSnapshot itemDs : userDs.getChildren()) {
                        StockItem stockItem = itemDs.getValue(StockItem.class);
                        if (stockItem != null && "Doação Coletada".equals(stockItem.getStockItemStatus())) {
                            Product productDetails = productCatalogMap.get(stockItem.getProductId());
                            if (productDetails != null) {
                                ItemDisplay displayItem = new ItemDisplay();
                                displayItem.setEstablishmentId(userDs.getKey());
                                displayItem.setStockItemId(itemDs.getKey());
                                displayItem.setProductName(productDetails.getProductName());
                                displayItem.setProductUnitType(productDetails.getProductUnitType());
                                displayItem.setStockItemQuantity(stockItem.getStockItemQuantity());
                                displayItem.setStockItemExpirationDate(stockItem.getStockItemExpirationDate());
                                displayItem.setProductId(stockItem.getProductId());

                                String productName = productDetails.getProductName();
                                List<ItemDisplay> itemsForProduct = groupedMap.computeIfAbsent(productName, k -> new ArrayList<>());
                                itemsForProduct.add(displayItem);
                            }
                        }
                    }
                }

                fullProductList.clear();
                for (Map.Entry<String, List<ItemDisplay>> entry : groupedMap.entrySet()) {
                    GroupedProduct newGroup = new GroupedProduct(entry.getKey(), entry.getValue());
                    fullProductList.add(newGroup);
                }
                
                Collections.sort(fullProductList, (o1, o2) -> o1.getProductName().compareToIgnoreCase(o2.getProductName()));
                
                adapter.setData(new ArrayList<>(fullProductList));
                updateViewVisibility(fullProductList.isEmpty());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if(getContext() != null) Toast.makeText(getContext(), "Falha ao carregar doações.", Toast.LENGTH_SHORT).show();
                updateViewVisibility(true);
            }
        });
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
}
