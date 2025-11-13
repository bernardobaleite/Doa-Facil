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
import model.ProfileEstablishment;

public class ConfigurationsEstablishmentActivity extends AppCompatActivity {

    private EditText editEstablishmentCnpj, editEstablishmentName, editEstablishmentAddress, editEstablishmentContact;
    private DatabaseReference firebaseRef;
    private String idCurrentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_establishment_configurations);

        initializeComponents();
        firebaseRef = ConfigurationFirebase.getFirebaseDatabase();
        idCurrentUser = UserFirebase.getIdUser();

        Toolbar toolbar = findViewById(R.id.toolbar_layout);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        loadEstablishmentProfileData();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void loadEstablishmentProfileData() {
        DatabaseReference establishmentRef = firebaseRef.child("establishment_profiles").child(idCurrentUser);
        establishmentRef.addValueEventListener(new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    ProfileEstablishment profileEstablishment = dataSnapshot.getValue(ProfileEstablishment.class);
                    if (profileEstablishment != null) {
                        editEstablishmentCnpj.setText(profileEstablishment.getEstablishmentCnpj());
                        editEstablishmentName.setText(profileEstablishment.getEstablishmentName());
                        editEstablishmentAddress.setText(profileEstablishment.getEstablishmentAddress());
                        editEstablishmentContact.setText(profileEstablishment.getEstablishmentContact());
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ConfigurationsEstablishmentActivity.this, "Falha ao carregar perfil.", Toast.LENGTH_SHORT).show();

            }
        });
    }

    public void validateDataEstablishment(View view) {
        String establishmentCnpj = editEstablishmentCnpj.getText().toString();
        String establishmentName = editEstablishmentName.getText().toString();
        String establishmentAddress = editEstablishmentAddress.getText().toString();
        String establishmentContact = editEstablishmentContact.getText().toString();

        if (establishmentCnpj.isEmpty() || establishmentName.isEmpty() || establishmentAddress.isEmpty() || establishmentContact.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            return;
        }

        ProfileEstablishment profileEstablishment = new ProfileEstablishment();
        profileEstablishment.setEstablishmentId(idCurrentUser);
        profileEstablishment.setEstablishmentCnpj(establishmentCnpj);
        profileEstablishment.setEstablishmentName(establishmentName);
        profileEstablishment.setEstablishmentAddress(establishmentAddress);
        profileEstablishment.setEstablishmentContact(establishmentContact);
        profileEstablishment.save();
        Toast.makeText(this, "Perfil salvo com sucesso!", Toast.LENGTH_SHORT).show();
        finish();
    }


    private void initializeComponents() {
        editEstablishmentCnpj = findViewById(R.id.edit_establishment_cnpj);
        editEstablishmentName = findViewById(R.id.edit_establishment_name);
        editEstablishmentAddress = findViewById(R.id.edit_establishment_address);
        // This ID will need to be changed in the XML layout file next.
        editEstablishmentContact = findViewById(R.id.edit_establishment_contact);
    }
}
