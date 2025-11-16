package activity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.company.doafacil.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import helper.ConfigurationFirebase;
import helper.UserFirebase;
import model.ProfileOng;

public class ConfigurationsOngActivity extends AppCompatActivity {

    private EditText editOngCnpj, editOngName, editOngAddress, editOngArea;
    private DatabaseReference firebaseRef;
    private String idCurrentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ong_configurations);

        initializeComponents();
        firebaseRef = ConfigurationFirebase.getFirebaseDatabase();
        idCurrentUser = UserFirebase.getIdUser();

        Toolbar toolbar = findViewById(R.id.toolbar_layout);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        loadOngProfileData();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void loadOngProfileData() {
        DatabaseReference ongRef = firebaseRef.child("ong_profiles").child(idCurrentUser);
        ongRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ProfileOng profile = dataSnapshot.getValue(ProfileOng.class);
                    if (profile != null) {
                        editOngCnpj.setText(profile.getOngCnpj());
                        editOngName.setText(profile.getOngName());
                        editOngAddress.setText(profile.getOngAddress());
                        editOngArea.setText(profile.getOngArea());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ConfigurationsOngActivity.this, "Falha: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void validateDataOng(View view) {
        String ongCnpj = editOngCnpj.getText().toString();
        String ongName = editOngName.getText().toString();
        String ongAddress = editOngAddress.getText().toString();
        String ongArea = editOngArea.getText().toString();

        if (ongCnpj.isEmpty() || ongName.isEmpty() || ongAddress.isEmpty() || ongArea.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            return;
        }

        ProfileOng profileOng = new ProfileOng();
        profileOng.setOngId(idCurrentUser);
        profileOng.setOngCnpj(ongCnpj);
        profileOng.setOngName(ongName);
        profileOng.setOngAddress(ongAddress);
        profileOng.setOngArea(ongArea);
        profileOng.save();
        Toast.makeText(this, "Perfil salvo com sucesso!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void initializeComponents() {
        editOngCnpj = findViewById(R.id.edit_ong_cnpj);
        editOngName = findViewById(R.id.edit_ong_name);
        editOngAddress = findViewById(R.id.edit_ong_address);
        editOngArea = findViewById(R.id.edit_ong_area);
    }
}
