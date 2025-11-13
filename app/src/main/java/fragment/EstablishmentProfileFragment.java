package fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.company.doafacil.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import activity.ConfigurationsEstablishmentActivity;
import activity.EstablishmentActivity;
import helper.ConfigurationFirebase;
import helper.UserFirebase;
import model.ProfileEstablishment;

// RE-ARCH: Refactoring to use the new decoupled toolbar control from EstablishmentActivity.
public class EstablishmentProfileFragment extends Fragment {

    private static final String TAG = "EstablishmentProfile";

    private TextView textName, textSubtitle, textLogout;
    private ShapeableImageView profileImage;
    private DatabaseReference firebaseRef;
    private String idCurrentUser;
    private ValueEventListener profileListener;
    private DatabaseReference establishmentProfileRef;

    public EstablishmentProfileFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_establishment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeComponents(view);
        setupClickListeners();

        firebaseRef = ConfigurationFirebase.getFirebaseDatabase();
        idCurrentUser = UserFirebase.getIdUser();
        establishmentProfileRef = firebaseRef.child("establishment_profiles").child(idCurrentUser);
    }

    @Override
    public void onResume() {
        super.onResume();
        // THE FIX: Set the toolbar title using the new decoupled method.
        if (getActivity() instanceof EstablishmentActivity) {
            ((EstablishmentActivity) getActivity()).setToolbarTitle("", EstablishmentActivity.TitleAlignment.CENTER);
        }
        loadProfileData();
    }

    @Override
    public void onPause() {
        super.onPause();
        detachProfileListener();
    }

    private void initializeComponents(View view) {
        textName = view.findViewById(R.id.profile_establishment_name);
        textSubtitle = view.findViewById(R.id.profile_establishment_subtitle);
        textLogout = view.findViewById(R.id.profile_establishment_logout);
        profileImage = view.findViewById(R.id.profile_establishment_image);
    }

    private void setupClickListeners() {
        View.OnClickListener openConfigurations = v -> {
            Intent intent = new Intent(getActivity(), ConfigurationsEstablishmentActivity.class);
            startActivity(intent);
        };

        profileImage.setOnClickListener(openConfigurations);
        textName.setOnClickListener(openConfigurations);
        textSubtitle.setOnClickListener(openConfigurations);

        textLogout.setOnClickListener(v -> {
            if (getActivity() instanceof EstablishmentActivity) {
                detachProfileListener();
                ((EstablishmentActivity) getActivity()).signOutUser();
            }
        });
    }

    private void loadProfileData() {
        if (profileListener == null) { 
            profileListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (isAdded() && dataSnapshot.exists()) {
                        ProfileEstablishment profile = dataSnapshot.getValue(ProfileEstablishment.class);
                        if (profile != null && profile.getEstablishmentName() != null && !profile.getEstablishmentName().isEmpty()) {
                            textName.setText(profile.getEstablishmentName());
                        } else {
                            textName.setText("Nome do Estabelecimento");
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    if (isAdded()) {
                        Log.e(TAG, "Carregamento do perfil no Firebase cancelado: " + databaseError.getMessage());
                    }
                }
            };
            establishmentProfileRef.addValueEventListener(profileListener);
        }
    }

    private void detachProfileListener() {
        if (profileListener != null && establishmentProfileRef != null) {
            establishmentProfileRef.removeEventListener(profileListener);
            profileListener = null;
        }
    }
}
