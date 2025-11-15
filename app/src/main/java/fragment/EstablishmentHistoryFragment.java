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
public class EstablishmentHistoryFragment extends Fragment {

    private RecyclerView recyclerHistory;
    private EstablishmentProductAdapter adapter;
    private List<ReceivedDonation> historyList = new ArrayList<>();
    private DatabaseReference donationsRef;
    private ValueEventListener historyListener;
    private String idCurrentUser;
    private TextView textNoHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_establishment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerHistory = view.findViewById(R.id.recycler_establishment_history);
        textNoHistory = view.findViewById(R.id.text_no_history);
        idCurrentUser = UserFirebase.getIdUser();
        donationsRef = ConfigurationFirebase.getFirebaseDatabase().child("received_donations");

        recyclerHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerHistory.setHasFixedSize(true);
        
        // THE FIX #1: Use the correct constructor that only takes the Context.
        adapter = new EstablishmentProductAdapter(getContext());
        recyclerHistory.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof EstablishmentActivity) {
            ((EstablishmentActivity) getActivity()).setToolbarTitle("Histórico de Doações", EstablishmentActivity.TitleAlignment.CENTER);
        }
        listenForHistory();
    }

    private void listenForHistory() {
        if (historyListener != null) {
            donationsRef.removeEventListener(historyListener);
        }

        historyListener = donationsRef.orderByChild("establishmentId").equalTo(idCurrentUser)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        historyList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            ReceivedDonation donation = ds.getValue(ReceivedDonation.class);
                            if (donation != null) {
                                historyList.add(donation);
                            }
                        }
                        Collections.reverse(historyList); // Show newest first
                        // THE FIX #2: Use the setData method to update the adapter's data.
                        adapter.setData(historyList);
                        updateViewVisibility();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Falha ao carregar histórico.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateViewVisibility() {
        if (historyList.isEmpty()) {
            textNoHistory.setVisibility(View.VISIBLE);
            recyclerHistory.setVisibility(View.GONE);
        } else {
            textNoHistory.setVisibility(View.GONE);
            recyclerHistory.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (donationsRef != null && historyListener != null) {
            donationsRef.removeEventListener(historyListener);
        }
    }
}
