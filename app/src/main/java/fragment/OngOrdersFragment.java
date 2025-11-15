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
import java.util.List;

import activity.OngActivity;
import adapter.OngOrdersAdapter;
import helper.ConfigurationFirebase;
import helper.UserFirebase;
import model.Order;

// RE-ARCH: Making the "My Orders" screen functional.
public class OngOrdersFragment extends Fragment {

    private RecyclerView recyclerOrders;
    private TextView textNoOrders;
    private OngOrdersAdapter adapter;
    private List<Order> orders = new ArrayList<>();
    
    private DatabaseReference ordersRef;
    private ValueEventListener ordersListener;

    public OngOrdersFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ong_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerOrders = view.findViewById(R.id.recycler_orders);
        textNoOrders = view.findViewById(R.id.text_no_orders);

        // Setup RecyclerView
        recyclerOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OngOrdersAdapter(orders, getContext());
        recyclerOrders.setAdapter(adapter);

        // Get current user and start listening for orders
        String currentOngId = UserFirebase.getIdUser();
        if (currentOngId != null) {
            ordersRef = ConfigurationFirebase.getFirebaseDatabase().child("orders").child(currentOngId);
            listenForOrders();
        }
    }

    private void listenForOrders() {
        ordersListener = ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orders.clear();
                if (!snapshot.exists()) {
                    updateViewVisibility();
                    return;
                }

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Order order = ds.getValue(Order.class);
                    if (order != null) {
                        orders.add(order);
                    }
                }

                // THE FIX: Use the correct getter for the creation timestamp.
                Collections.sort(orders, (o1, o2) -> Long.compare(o2.getOrderCreatedAt(), o1.getOrderCreatedAt()));
                adapter.setData(orders);
                updateViewVisibility();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if(getContext() != null) {
                    Toast.makeText(getContext(), "Falha ao carregar pedidos.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateViewVisibility() {
        if (orders.isEmpty()) {
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
        if (getActivity() instanceof OngActivity) {
            ((OngActivity) getActivity()).setToolbarTitle("Meus Pedidos", OngActivity.TitleAlignment.CENTER);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (ordersListener != null && ordersRef != null) {
            ordersRef.removeEventListener(ordersListener);
        }
    }
}
