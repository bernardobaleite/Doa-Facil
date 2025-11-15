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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import activity.AdministratorActivity;
import adapter.AdminApprovalAdapter;
import helper.ConfigurationFirebase;
import model.ProfileEstablishment;
import model.ReceivedDonation;
import model.StockItem;

// RE-DESIGN: Changing the screen title as per user's final specification.
public class AdminApprovalsFragment extends Fragment {

    public static class AdminApprovalItem {
        public final ReceivedDonation donation;
        public final String establishmentName;
        public final String establishmentAddress;
        public final String effectiveStatus;

        public AdminApprovalItem(ReceivedDonation donation, String establishmentName, String establishmentAddress, String effectiveStatus) {
            this.donation = donation;
            this.establishmentName = establishmentName;
            this.establishmentAddress = establishmentAddress;
            this.effectiveStatus = effectiveStatus;
        }
    }

    private RecyclerView recyclerApproval;
    private AdminApprovalAdapter adapter;
    private DatabaseReference databaseRef;
    
    private ValueEventListener donationsListener, stockItemsListener, profilesListener;
    private DatabaseReference donationsRef, stockItemsRef, profilesRef;

    private final List<ReceivedDonation> allDonations = new ArrayList<>();
    private final Map<String, StockItem> stockItemsByDonationId = new HashMap<>();
    private final Map<String, ProfileEstablishment> establishmentProfileMap = new HashMap<>();

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
        
        donationsRef = databaseRef.child("received_donations");
        stockItemsRef = databaseRef.child("stock_items");
        profilesRef = databaseRef.child("establishment_profiles");

        recyclerApproval.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerApproval.setHasFixedSize(true);
        
        adapter = new AdminApprovalAdapter(new ArrayList<>(), getContext());
        recyclerApproval.setAdapter(adapter);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof AdministratorActivity) {
            // THE FIX: Set the correct title for the screen.
            ((AdministratorActivity) getActivity()).setToolbarTitle("Painel de Coletas", AdministratorActivity.TitleAlignment.CENTER);
        }
        loadAllData();
    }

    private void loadAllData() {
        profilesListener = profilesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                establishmentProfileMap.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ProfileEstablishment profile = ds.getValue(ProfileEstablishment.class);
                    if (profile != null) establishmentProfileMap.put(ds.getKey(), profile);
                }
                combineDataAndRefreshUI();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError e) { showToast("Falha ao carregar perfis."); }
        });

        stockItemsListener = stockItemsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                stockItemsByDonationId.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    StockItem item = ds.getValue(StockItem.class);
                    if (item != null && item.getDonationId() != null) {
                        stockItemsByDonationId.put(item.getDonationId(), item);
                    }
                }
                combineDataAndRefreshUI();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError e) { showToast("Falha ao carregar estoque."); }
        });

        donationsListener = donationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allDonations.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    allDonations.add(ds.getValue(ReceivedDonation.class));
                }
                combineDataAndRefreshUI();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError e) { showToast("Falha ao carregar doações."); }
        });
    }

    private void combineDataAndRefreshUI() {
        List<AdminApprovalItem> processedList = new ArrayList<>();
        for (ReceivedDonation donation : allDonations) {
            if (donation == null) continue;

            String effectiveStatus = donation.getReceivedStatus();
            
            if ("Doação coletada".equals(effectiveStatus)) {
                StockItem correspondingStockItem = stockItemsByDonationId.get(donation.getDonationId());
                if (correspondingStockItem != null && "Removido do estoque".equals(correspondingStockItem.getStockItemStatus())) {
                    effectiveStatus = "Removido do estoque";
                }
            }

            ProfileEstablishment profile = establishmentProfileMap.get(donation.getEstablishmentId());
            String establishmentName = (profile != null) ? profile.getEstablishmentName() : "Nome Desconhecido";
            String establishmentAddress = (profile != null) ? profile.getEstablishmentAddress() : "";

            processedList.add(new AdminApprovalItem(donation, establishmentName, establishmentAddress, effectiveStatus));
        }

        Collections.reverse(processedList); 
        adapter.setData(processedList);
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (donationsListener != null) donationsRef.removeEventListener(donationsListener);
        if (stockItemsListener != null) stockItemsRef.removeEventListener(stockItemsListener);
        if (profilesListener != null) profilesRef.removeEventListener(profilesListener);
    }
}
