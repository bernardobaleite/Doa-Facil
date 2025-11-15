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

import activity.EstablishmentActivity;
import adapter.EstablishmentProductAdapter;
import helper.ConfigurationFirebase;
import helper.UserFirebase;
import model.ReceivedDonation;

// REBUILD: Final fix for the Doador flow - using the unified adapter constructor and setData method.
public class EstablishmentListProductsFragment extends Fragment {

    private RecyclerView recyclerProducts;
    private EstablishmentProductAdapter adapter;
    private List<ReceivedDonation> donationList = new ArrayList<>();
    private DatabaseReference donationsRef;
    private ValueEventListener donationsListener;
    private String idCurrentUser;
    private TextView textNoProducts;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_establishment_list_products, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerProducts = view.findViewById(R.id.recycler_establishment_products);
        textNoProducts = view.findViewById(R.id.text_no_products);
        idCurrentUser = UserFirebase.getIdUser();
        donationsRef = ConfigurationFirebase.getFirebaseDatabase().child("received_donations");

        recyclerProducts.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerProducts.setHasFixedSize(true);
        
        // THE FIXo #1: Use the correct constructor that only takes the Context.
        adapter = new EstablishmentProductAdapter(getContext());
        recyclerProducts.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof EstablishmentActivity) {
            ((EstablishmentActivity) getActivity()).setToolbarTitle("Doações Pendentes", EstablishmentActivity.TitleAlignment.CENTER);
        }
        listenForDonations();
    }

    private void listenForDonations() {
        if (donationsListener != null) {
            donationsRef.removeEventListener(donationsListener);
        }

        donationsListener = donationsRef.orderByChild("establishmentId").equalTo(idCurrentUser)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        donationList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            ReceivedDonation donation = ds.getValue(ReceivedDonation.class);
                            if (donation != null && "Aguarde a coleta".equals(donation.getReceivedStatus())) {
                                donationList.add(donation);
                            }
                        }
                        Collections.reverse(donationList); // Show newest first
                        // THE FIX #2: Use the setData method to update the adapter's data.
                        adapter.setData(donationList);
                        updateViewVisibility();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Falha ao carregar doações.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateViewVisibility() {
        if (donationList.isEmpty()) {
            textNoProducts.setVisibility(View.VISIBLE);
            recyclerProducts.setVisibility(View.GONE);
        } else {
            textNoProducts.setVisibility(View.GONE);
            recyclerProducts.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (donationsRef != null && donationsListener != null) {
            donationsRef.removeEventListener(donationsListener);
        }
    }
}
