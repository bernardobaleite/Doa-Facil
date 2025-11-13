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
public class EstablishmentHistoryFragment extends Fragment {

    private RecyclerView recyclerHistory;
    private EstablishmentProductAdapter adapter;
    private List<ItemDisplay> historyItemsDisplayList = new ArrayList<>();
    private Map<String, Product> productCatalog = new HashMap<>();
    private DatabaseReference databaseRef;
    private ValueEventListener historyListener;
    private DatabaseReference stockItemsRef;
    private String idCurrentUser;

    public EstablishmentHistoryFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_establishment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerHistory = view.findViewById(R.id.recycler_establishment_history);
        databaseRef = ConfigurationFirebase.getFirebaseDatabase();
        idCurrentUser = UserFirebase.getIdUser();

        recyclerHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerHistory.setHasFixedSize(true);
        adapter = new EstablishmentProductAdapter(historyItemsDisplayList, getContext(), "historico");
        recyclerHistory.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        // THE FIX: Set the toolbar title using the new decoupled method.
        if (getActivity() instanceof EstablishmentActivity) {
            ((EstablishmentActivity) getActivity()).setToolbarTitle("Histórico de Doações", EstablishmentActivity.TitleAlignment.CENTER);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        loadLocalCatalogAndThenListenForHistory();
    }

    private void loadLocalCatalogAndThenListenForHistory() {
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
                listenForHistoryItems();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if(getContext() != null)
                    Toast.makeText(getContext(), "Falha ao carregar o catálogo de produtos.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenForHistoryItems() {
        stockItemsRef = databaseRef.child("stock_items").child(idCurrentUser);
        if (historyListener == null) {
            historyListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    historyItemsDisplayList.clear();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        StockItem stockItem = ds.getValue(StockItem.class);
                        if (stockItem != null && !"Pendente de Aprovação".equals(stockItem.getStockItemStatus())) {
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
                                historyItemsDisplayList.add(displayItem);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if(getContext() != null)
                        Toast.makeText(getContext(), "Falha ao carregar histórico: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            };
            stockItemsRef.addValueEventListener(historyListener);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (stockItemsRef != null && historyListener != null) {
            stockItemsRef.removeEventListener(historyListener);
            historyListener = null;
        }
    }
}
