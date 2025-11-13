package fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.company.doafacil.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import activity.EstablishmentActivity;
import adapter.EstablishmentProductAdapter;
import helper.ConfigurationFirebase;
import helper.UserFirebase;
import model.Product;
import model.StockItem;
import model.ItemDisplay;

// RE-ARCH: Refactoring to use the new decoupled toolbar control from EstablishmentActivity.
public class EstablishmentListProductsFragment extends Fragment {

    private RecyclerView recyclerProducts;
    private EstablishmentProductAdapter adapter;
    private List<ItemDisplay> stockItemsDisplayList = new ArrayList<>();
    private Map<String, Product> productCatalog = new HashMap<>();
    private DatabaseReference databaseRef;
    private ValueEventListener stockItemsListener;
    private DatabaseReference stockItemsRef;
    private String idCurrentUser;

    public EstablishmentListProductsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_establishment_list_products, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerProducts = view.findViewById(R.id.recycler_establishment_products);
        databaseRef = ConfigurationFirebase.getFirebaseDatabase();
        idCurrentUser = UserFirebase.getIdUser();

        recyclerProducts.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerProducts.setHasFixedSize(true);
        adapter = new EstablishmentProductAdapter(stockItemsDisplayList, getContext(), "pendente");
        recyclerProducts.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        // THE FIX: Set the toolbar title using the new decoupled method.
        if (getActivity() instanceof EstablishmentActivity) {
            ((EstablishmentActivity) getActivity()).setToolbarTitle("Doações Pendentes", EstablishmentActivity.TitleAlignment.CENTER);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        loadLocalCatalogAndThenListenForStock();
    }

    private void loadLocalCatalogAndThenListenForStock() {
        DatabaseReference catalogRef = databaseRef.child("establishment_product_catalog");
        catalogRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                productCatalog.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Product product = ds.getValue(Product.class);
                    if (product != null) {
                        String productId = ds.getKey();
                        product.setProductId(productId);
                        productCatalog.put(productId, product);
                    }
                }
                listenForStockItems();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if(getContext() != null)
                    Toast.makeText(getContext(), "Falha ao carregar o catálogo de produtos.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenForStockItems() {
        stockItemsRef = databaseRef.child("stock_items").child(idCurrentUser);
        if (stockItemsListener == null) {
            stockItemsListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    stockItemsDisplayList.clear();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        StockItem stockItem = ds.getValue(StockItem.class);
                        if (stockItem != null && "Pendente de Aprovação".equals(stockItem.getStockItemStatus())) {
                            Product productDetails = productCatalog.get(stockItem.getProductId());
                            if (productDetails != null) {
                                ItemDisplay displayItem = new ItemDisplay();
                                displayItem.setStockItemId(ds.getKey());
                                displayItem.setEstablishmentId(stockItem.getEstablishmentId());
                                displayItem.setProductName(productDetails.getProductName());
                                displayItem.setProductUnitType(productDetails.getProductUnitType());
                                displayItem.setStockItemQuantity(stockItem.getStockItemQuantity());
                                displayItem.setStockItemExpirationDate(stockItem.getStockItemExpirationDate());
                                displayItem.setStockItemStatus(stockItem.getStockItemStatus());
                                displayItem.setStockItemPickupDate(stockItem.getStockItemPickupDate());
                                displayItem.setStockItemPickupTime(stockItem.getStockItemPickupTime());
                                stockItemsDisplayList.add(displayItem);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if(getContext() != null)
                        Toast.makeText(getContext(), "Falha ao carregar produtos doados: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            };
            stockItemsRef.addValueEventListener(stockItemsListener);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (stockItemsRef != null && stockItemsListener != null) {
            stockItemsRef.removeEventListener(stockItemsListener);
            stockItemsListener = null;
        }
    }
}
