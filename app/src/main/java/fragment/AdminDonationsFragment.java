package fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import activity.AdministratorActivity;
import adapter.AdminOrdersAdapter;
import helper.ConfigurationFirebase;
import model.Order;
import model.ProfileOng;

// RE-ARCH: Fixing the call to setData to pass the ongNamesMap.
public class AdminDonationsFragment extends Fragment implements AdminOrdersAdapter.OnOrderInteractionListener {

    private RecyclerView recyclerAdminOrders;
    private TextView textNoOrders;
    private AdminOrdersAdapter adapter;
    private DatabaseReference databaseRef;
    private ValueEventListener ordersListener, ongProfilesListener;

    private List<Order> allOrders = new ArrayList<>();
    private Map<String, String> ongNamesMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_donations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerAdminOrders = view.findViewById(R.id.recycler_admin_donations);
        textNoOrders = view.findViewById(R.id.text_admin_no_donations);
        databaseRef = ConfigurationFirebase.getFirebaseDatabase();

        recyclerAdminOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        // Pass initial empty map, it will be updated by the listener.
        adapter = new AdminOrdersAdapter(new ArrayList<>(), new HashMap<>(), getContext(), this);
        recyclerAdminOrders.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof AdministratorActivity) {
            ((AdministratorActivity) getActivity()).setToolbarTitle("Doações Solicitadas", AdministratorActivity.TitleAlignment.CENTER);
        }
        attachListeners();
    }

    @Override
    public void onStop() {
        super.onStop();
        detachListeners();
    }

    @Override
    public void onOrderCancelled() {
        if (getActivity() instanceof AdministratorActivity) {
            ((AdministratorActivity) getActivity()).navigateToHome();
        }
    }

    private void attachListeners() {
        detachListeners();

        ongProfilesListener = databaseRef.child("ong_profiles").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ongNamesMap.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ProfileOng profile = ds.getValue(ProfileOng.class);
                    if (profile != null) {
                        ongNamesMap.put(ds.getKey(), profile.getOngName());
                    }
                }
                combineAndRefreshUI();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("Falha ao carregar perfis de ONGs.");
            }
        });

        ordersListener = databaseRef.child("orders").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allOrders.clear();
                for (DataSnapshot ongIdSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot orderSnapshot : ongIdSnapshot.getChildren()) {
                        if (orderSnapshot.hasChildren()) {
                            Order order = orderSnapshot.getValue(Order.class);
                            if (order != null) {
                                allOrders.add(order);
                            }
                        }
                    }
                }
                combineAndRefreshUI();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("Falha ao carregar pedidos.");
            }
        });
    }

    private void detachListeners() {
        if (ordersListener != null) {
            databaseRef.child("orders").removeEventListener(ordersListener);
        }
        if (ongProfilesListener != null) {
            databaseRef.child("ong_profiles").removeEventListener(ongProfilesListener);
        }
    }

    private void combineAndRefreshUI() {
        Collections.sort(allOrders, (o1, o2) -> Long.compare(o2.getOrderCreatedAt(), o1.getOrderCreatedAt()));
        // THE FIX: Pass the updated map to the adapter.
        adapter.setData(allOrders, ongNamesMap);
        textNoOrders.setVisibility(allOrders.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerAdminOrders.setVisibility(allOrders.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
