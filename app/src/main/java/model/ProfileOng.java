package model;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;

import helper.ConfigurationFirebase;

public class ProfileOng {
    private String ongId;
    private String ongCnpj;
    private String ongName;
    private String ongAddress;
    private String ongArea;
    public ProfileOng() {
    }

    public void save() {
        DatabaseReference firebaseRef = ConfigurationFirebase.getFirebaseDatabase();
        DatabaseReference ongRef = firebaseRef.child("ong_profiles").child(getOngId());
        ongRef.setValue(this);
    }

    // É uma boa prática excluir o ID do usuário, pois ele já é a chave no banco de dados
    @Exclude
    public String getOngId() {
        return ongId;
    }

    public void setOngId(String ongId) {
        this.ongId = ongId;
    }

    public String getOngCnpj() {
        return ongCnpj;
    }

    public void setOngCnpj(String ongCnpj) {
        this.ongCnpj = ongCnpj;
    }

    public String getOngName() {
        return ongName;
    }

    public void setOngName(String ongName) {
        this.ongName = ongName;
    }

    public String getOngAddress() {
        return ongAddress;
    }

    public void setOngAddress(String ongAddress) {
        this.ongAddress = ongAddress;
    }

    public String getOngArea() {
        return ongArea;
    }

    public void setOngArea(String ongArea) {
        this.ongArea = ongArea;
    }
}
