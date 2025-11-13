package fragment;

import android.content.DialogInterface;
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
import com.google.firebase.database.ServerValue;
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
import model.ItemDisplay;
import model.Product;

// RE-DEBUG: Implementing user's strategy to display the detailed Firebase error on failure.
public class OngCartFragment extends Fragment {

    private RecyclerView recyclerCart;
    private OngCartAdapter adapter;
    private FloatingActionButton fabConfirmOrder;

    private DatabaseReference cartRef;
    private ValueEventListener cartListener;
    private String currentOngId;

    private Map<String, Product> productCatalogMap = new HashMap<>();
    private List<ItemDisplay> currentCartItems = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ong_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerCart = view.findViewById(R.id.recycler_cart_items);
        fabConfirmOrder = view.findViewById(R.id.fab_confirm_order);
        
        recyclerCart.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OngCartAdapter(getContext(), new ArrayList<>());
        recyclerCart.setAdapter(adapter);

        fabConfirmOrder.setOnClickListener(v -> {
            if (currentCartItems.isEmpty()) {
                Toast.makeText(getContext(), "Seu carrinho está vazio.", Toast.LENGTH_SHORT).show();
            } else {
                showConfirmOrderDialog();
            }
        });

        currentOngId = UserFirebase.getIdUser();
        if (currentOngId != null) {
            cartRef = ConfigurationFirebase.getFirebaseDatabase().child("shopping_carts").child(currentOngId);
            loadProductCatalog();
        }
    }
    
    private void showConfirmOrderDialog(){
        new AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Pedido")
            .setMessage("Deseja confirmar o pedido?")
            .setPositiveButton("Confirmar", (dialog, which) -> confirmOrder())
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void confirmOrder() {
        if (currentOngId == null || currentCartItems.isEmpty()) {
            Toast.makeText(getContext(), "Erro: Não foi possível confirmar o pedido.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference databaseRef = ConfigurationFirebase.getFirebaseDatabase();
        String orderId = databaseRef.child("orders").child(currentOngId).push().getKey();

        if (orderId == null) {
            Toast.makeText(getContext(), "Erro ao gerar ID do pedido.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, ItemDisplay> itemsMap = new HashMap<>();
        for (ItemDisplay item : currentCartItems) {
            itemsMap.put(item.getStockItemId(), item);
        }

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("status", "Realize o agendamento");
        orderData.put("timestamp", ServerValue.TIMESTAMP);
        orderData.put("items", itemsMap);
        orderData.put("ongId", currentOngId);
        orderData.put("orderId", orderId);

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/orders/" + currentOngId + "/" + orderId, orderData);
        childUpdates.put("/shopping_carts/" + currentOngId, null);

        databaseRef.updateChildren(childUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Pedido criado com sucesso!", Toast.LENGTH_LONG).show();
                if (getActivity() instanceof OngActivity) {
                    ((OngActivity) getActivity()).navigateToOrders();
                }
            } else {
                // THE FIX: Display the actual Firebase error message.
                String errorMessage = task.getException() != null ? task.getException().getMessage() : "Erro desconhecido.";
                Toast.makeText(getContext(), "Falha ao criar o pedido: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof OngActivity) {
            ((OngActivity) getActivity()).setToolbarTitle("Carrinho", OngActivity.TitleAlignment.CENTER);
        }
    }

    private void loadProductCatalog() {
        DatabaseReference catalogRef = ConfigurationFirebase.getFirebaseDatabase().child("establishment_product_catalog");
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
                listenForCartItems();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) Toast.makeText(getContext(), "Falha ao carregar catálogo.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenForCartItems() {
        cartListener = cartRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot cartSnapshot) {
                currentCartItems.clear();
                if (!cartSnapshot.exists()) {
                    adapter.setData(new ArrayList<>());
                    fabConfirmOrder.setVisibility(View.GONE);
                    return;
                }

                DatabaseReference stockRef = ConfigurationFirebase.getFirebaseDatabase().child("stock_items");
                stockRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot stockSnapshot) {
                        List<ItemDisplay> validItems = new ArrayList<>();
                        List<String> itemsToRemove = new ArrayList<>();

                        for (DataSnapshot cartItemDs : cartSnapshot.getChildren()) {
                            ItemDisplay cartItem = cartItemDs.getValue(ItemDisplay.class);
                            if (cartItem == null || cartItem.getEstablishmentId() == null || cartItem.getStockItemId() == null) {
                                if (cartItem != null && cartItem.getStockItemId() != null) itemsToRemove.add(cartItem.getStockItemId());
                                continue;
                            }

                            if (stockSnapshot.child(cartItem.getEstablishmentId()).hasChild(cartItem.getStockItemId())) {
                                Product productDetails = productCatalogMap.get(cartItem.getProductId());
                                if (productDetails != null) {
                                    cartItem.setProductUnitType(productDetails.getProductUnitType());
                                }
                                validItems.add(cartItem);
                            } else {
                                itemsToRemove.add(cartItem.getStockItemId());
                            }
                        }

                        processValidationResults(validItems, itemsToRemove);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Erro ao validar estoque.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Falha ao carregar o carrinho.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void processValidationResults(List<ItemDisplay> validItems, List<String> itemIdsToRemove) {
        currentCartItems.clear();
        currentCartItems.addAll(validItems);

        if (currentCartItems.isEmpty()) {
            fabConfirmOrder.setVisibility(View.GONE);
        } else {
            fabConfirmOrder.setVisibility(View.VISIBLE);
        }

        Map<String, GroupedProduct> groupedMap = new HashMap<>();
        for (ItemDisplay item : validItems) {
            if (item.getProductName() != null) {
                GroupedProduct group = groupedMap.computeIfAbsent(item.getProductName(),
                        k -> new GroupedProduct(item.getProductName(), new ArrayList<>()));
                group.getItems().add(item);
            }
        }

        List<GroupedProduct> groupedList = new ArrayList<>(groupedMap.values());
        Collections.sort(groupedList, (o1, o2) -> o1.getProductName().compareToIgnoreCase(o2.getProductName()));

        adapter.setData(groupedList);

        if (!itemIdsToRemove.isEmpty() && currentOngId != null) {
            for (String itemIdToRemove : itemIdsToRemove) {
                cartRef.child(itemIdToRemove).removeValue();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (cartListener != null && cartRef != null) {
            cartRef.removeEventListener(cartListener);
        }
    }
}
