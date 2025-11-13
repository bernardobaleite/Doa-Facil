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

// RE-DEBUG: Implementing user's strategy to show detailed error on ONG name load failure.
public class AdminDonationsFragment extends Fragment {

    private RecyclerView recyclerOrders;
    private TextView textNoOrders;
    private AdminOrdersAdapter adapter;
    private List<Order> allOrders = new ArrayList<>();
    private Map<String, String> ongNamesMap = new HashMap<>();

    private DatabaseReference ordersRef;
    private DatabaseReference ongsRef;
    private ValueEventListener ordersListener;
    private ValueEventListener ongsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_donations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerOrders = view.findViewById(R.id.recycler_admin_donations);
        textNoOrders = view.findViewById(R.id.text_admin_no_donations);

        recyclerOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdminOrdersAdapter(new ArrayList<>(), ongNamesMap, getContext());
        recyclerOrders.setAdapter(adapter);

        ordersRef = ConfigurationFirebase.getFirebaseDatabase().child("orders");
        ongsRef = ConfigurationFirebase.getFirebaseDatabase().child("ong_profiles");
        
        loadOngNames();
    }

    private void loadOngNames() {
        ongsListener = ongsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ongNamesMap.clear();
                for (DataSnapshot ongDs : snapshot.getChildren()) {
                    ProfileOng ongProfile = ongDs.getValue(ProfileOng.class);
                    if (ongProfile != null) {
                        ongNamesMap.put(ongDs.getKey(), ongProfile.getOngName());
                    }
                }
                listenForAllOrders();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if(getContext() != null) {
                    // THE FIX: Display the actual Firebase error message.
                    Toast.makeText(getContext(), "Falha ao carregar nomes das ONGs: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void listenForAllOrders() {
        if (ordersListener != null) {
            ordersRef.removeEventListener(ordersListener);
        }
        ordersListener = ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allOrders.clear();
                if (!snapshot.exists()) {
                    updateViewVisibility();
                    return;
                }

                for (DataSnapshot ongSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot orderSnapshot : ongSnapshot.getChildren()) {
                        Order order = orderSnapshot.getValue(Order.class);
                        if (order != null) {
                            allOrders.add(order);
                        }
                    }
                }

                Collections.sort(allOrders, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                adapter.setData(allOrders);
                updateViewVisibility();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if(getContext() != null) {
                    Toast.makeText(getContext(), "Falha ao carregar pedidos: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void updateViewVisibility() {
        if (allOrders.isEmpty()) {
            textNoOrders.setVisibility(View.VISIBLE);
            recyclerOrders.setVisibility(View.GONE);
        } else {
            textNoOrders.setVisibility(View.GONE);
            recyclerOrders.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof AdministratorActivity) {
            ((AdministratorActivity) getActivity()).setToolbarTitle("Doações", AdministratorActivity.TitleAlignment.CENTER);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (ordersListener != null && ordersRef != null) {
            ordersRef.removeEventListener(ordersListener);
        }
        if (ongsListener != null && ongsRef != null) {
            ongsRef.removeEventListener(ongsListener);
        }
    }
}
