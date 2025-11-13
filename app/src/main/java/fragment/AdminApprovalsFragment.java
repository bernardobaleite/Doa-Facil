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

import activity.AdministratorActivity;
import adapter.AdminApprovalAdapter;
import helper.ConfigurationFirebase;
import model.ItemDisplay;
import model.Product;
import model.StockItem;

// RE-ARCH: Refactoring to use the new decoupled toolbar control from AdministratorActivity.
public class AdminApprovalsFragment extends Fragment {

    private static class ProfileData {
        String establishmentName;
        String establishmentAddress;
    }

    private RecyclerView recyclerApproval;
    private AdminApprovalAdapter adapter;
    private List<ItemDisplay> itemList = new ArrayList<>();
    private DatabaseReference databaseRef;
    private ValueEventListener stockItemsListener;

    private Map<String, ProfileData> establishmentProfileMap = new HashMap<>();
    private Map<String, Product> productCatalogMap = new HashMap<>();

    public AdminApprovalsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_approvals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerApproval = view.findViewById(R.id.recycler_admin_approval);
        databaseRef = ConfigurationFirebase.getFirebaseDatabase();

        recyclerApproval.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerApproval.setHasFixedSize(true);
        adapter = new AdminApprovalAdapter(itemList, getContext());
        recyclerApproval.setAdapter(adapter);

        loadEstablishmentProfiles();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // THE FIX: Set the toolbar title using the new decoupled method.
        if (getActivity() instanceof AdministratorActivity) {
            ((AdministratorActivity) getActivity()).setToolbarTitle("Aprovações Pendentes", AdministratorActivity.TitleAlignment.CENTER);
        }
    }

    private void loadEstablishmentProfiles() {
        DatabaseReference profilesRef = databaseRef.child("establishment_profiles");
        profilesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                establishmentProfileMap.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String establishmentName = ds.child("establishmentName").getValue(String.class);
                    String establishmentAddress = ds.child("establishmentAddress").getValue(String.class);

                    if (establishmentName != null) {
                        ProfileData profile = new ProfileData();
                        profile.establishmentName = establishmentName;
                        profile.establishmentAddress = establishmentAddress;
                        establishmentProfileMap.put(ds.getKey(), profile);
                    }
                }
                loadProductCatalog();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Falha ao carregar perfis.", Toast.LENGTH_SHORT).show();
                }
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
                        String productId = ds.getKey();
                        product.setProductId(productId);
                        productCatalogMap.put(productId, product);
                    }
                }
                listenForAllStockItems();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                 if (getContext() != null) {
                    Toast.makeText(getContext(), "Falha ao carregar catálogo.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void listenForAllStockItems() {
        DatabaseReference stockRef = databaseRef.child("stock_items");
        stockItemsListener = stockRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                itemList.clear();
                for (DataSnapshot userDs : snapshot.getChildren()) {
                    String userId = userDs.getKey();
                    ProfileData profile = establishmentProfileMap.get(userId);

                    for (DataSnapshot itemDs : userDs.getChildren()) {
                        StockItem stockItem = itemDs.getValue(StockItem.class);
                        if (stockItem != null) {
                            Product productDetails = productCatalogMap.get(stockItem.getProductId());
                            if (productDetails != null) {
                                ItemDisplay displayItem = new ItemDisplay();
                                displayItem.setEstablishmentId(userId);
                                displayItem.setStockItemId(itemDs.getKey());
                                displayItem.setStockItemStatus(stockItem.getStockItemStatus());
                                displayItem.setProductName(productDetails.getProductName());
                                displayItem.setProductUnitType(productDetails.getProductUnitType());
                                
                                if (profile != null) {
                                    displayItem.setEstablishmentName(profile.establishmentName);
                                    displayItem.setEstablishmentAddress(profile.establishmentAddress);
                                } else {
                                    displayItem.setEstablishmentName("Nome Desconhecido");
                                }

                                displayItem.setStockItemQuantity(stockItem.getStockItemQuantity());
                                displayItem.setStockItemExpirationDate(stockItem.getStockItemExpirationDate());
                                itemList.add(displayItem);
                            }
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Falha ao carregar itens de estoque.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (stockItemsListener != null) {
            databaseRef.child("stock_items").removeEventListener(stockItemsListener);
        }
    }
}
