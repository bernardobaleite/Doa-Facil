package model;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;

import helper.ConfigurationFirebase;

public class ProfileEstablishment {
    private String establishmentId;      // ID do usuário do Firebase Authentication
    private String establishmentCnpj;        // CNPJ do estabelecimento
    private String establishmentName;        // Nome do estabelecimento
    private String establishmentAddress;     // Endereço
    private String establishmentContact;        // Contato do estabelecimento

    public ProfileEstablishment() {
    }

    public void save() {
        DatabaseReference firebaseRef = ConfigurationFirebase.getFirebaseDatabase();
        DatabaseReference establishmentRef = firebaseRef.child("establishment_profiles").child(getEstablishmentId());
        establishmentRef.setValue(this);
    }

    // É uma boa prática excluir o ID do usuário, pois ele já é a chave no banco de dados
    @Exclude
    public String getEstablishmentId() {
        return establishmentId;
    }

    public void setEstablishmentId(String establishmentId) {
        this.establishmentId = establishmentId;
    }

    public String getEstablishmentCnpj() {
        return establishmentCnpj;
    }

    public void setEstablishmentCnpj(String establishmentCnpj) {
        this.establishmentCnpj = establishmentCnpj;
    }

    public String getEstablishmentName() {
        return establishmentName;
    }

    public void setEstablishmentName(String establishmentName) {
        this.establishmentName = establishmentName;
    }

    public String getEstablishmentAddress() {
        return establishmentAddress;
    }

    public void setEstablishmentAddress(String establishmentAddress) {
        this.establishmentAddress = establishmentAddress;
    }

    public String getEstablishmentContact() {
        return establishmentContact;
    }

    public void setEstablishmentContact(String establishmentContact) {
        this.establishmentContact = establishmentContact;
    }
}
