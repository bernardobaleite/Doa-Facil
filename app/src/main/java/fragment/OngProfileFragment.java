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

import activity.ConfigurationsOngActivity;
import activity.OngActivity;
import helper.ConfigurationFirebase;
import helper.UserFirebase;
import model.ProfileOng;

// RE-ARCH: Refactoring to use the new decoupled toolbar control from OngActivity.
public class OngProfileFragment extends Fragment {

    private static final String TAG = "OngProfileFragment";

    private TextView textName, textSubtitle, textLogout;
    private ShapeableImageView profileImage;
    private DatabaseReference firebaseRef;
    private String idCurrentUser;
    private ValueEventListener profileListener;
    private DatabaseReference ongProfileRef;

    public OngProfileFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ong_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeComponents(view);
        setupClickListeners();

        firebaseRef = ConfigurationFirebase.getFirebaseDatabase();
        idCurrentUser = UserFirebase.getIdUser();
        ongProfileRef = firebaseRef.child("ong_profiles").child(idCurrentUser);
    }

    @Override
    public void onResume() {
        super.onResume();
        // THE FIX: Set the toolbar title using the new decoupled method.
        if (getActivity() instanceof OngActivity) {
            ((OngActivity) getActivity()).setToolbarTitle("", OngActivity.TitleAlignment.CENTER);
        }
        loadProfileData(); // Load data when fragment is visible
    }

    @Override
    public void onPause() {
        super.onPause();
        detachProfileListener(); // Detach listener when fragment is not visible
    }

    private void initializeComponents(View view) {
        textName = view.findViewById(R.id.profile_ong_name);
        textSubtitle = view.findViewById(R.id.profile_ong_subtitle);
        textLogout = view.findViewById(R.id.profile_ong_logout);
        profileImage = view.findViewById(R.id.profile_ong_image);
    }

    private void setupClickListeners() {
        View.OnClickListener openConfigurations = v -> {
            Intent intent = new Intent(getActivity(), ConfigurationsOngActivity.class);
            startActivity(intent);
        };

        profileImage.setOnClickListener(openConfigurations);
        textName.setOnClickListener(openConfigurations);
        textSubtitle.setOnClickListener(openConfigurations);

        textLogout.setOnClickListener(v -> {
            if (getActivity() instanceof OngActivity) {
                detachProfileListener();
                ((OngActivity) getActivity()).signOutUser();
            }
        });
    }

    private void loadProfileData() {
        if (profileListener == null) { // Attach listener only if it's not already attached
            profileListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (isAdded() && dataSnapshot.exists()) {
                        ProfileOng profile = dataSnapshot.getValue(ProfileOng.class);
                        if (profile != null && profile.getOngName() != null && !profile.getOngName().isEmpty()) {
                            textName.setText(profile.getOngName());
                        } else {
                            textName.setText("Nome da ONG");
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
            ongProfileRef.addValueEventListener(profileListener);
        }
    }

    private void detachProfileListener() {
        if (profileListener != null && ongProfileRef != null) {
            ongProfileRef.removeEventListener(profileListener);
            profileListener = null;
        }
    }
}
