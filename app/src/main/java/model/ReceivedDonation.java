package model;

import com.google.firebase.database.DatabaseReference;

import java.util.Map;

import helper.ConfigurationFirebase;

// ARCHITECTURE: Adding default status and save method as per the user's insight.
public class ReceivedDonation {

    private String donationId;
    private String establishmentId;
    private long receivedCreatedAt;
    private String receivedScheduledDateTime;
    private String receivedStatus;
    private Map<String, ReceivedDonationItem> receivedItems;

    public ReceivedDonation() {
        // Set the default status for any new donation.
        this.receivedStatus = "Aguarde a coleta";
        
        // Generate a unique ID for the donation.
        DatabaseReference firebaseRef = ConfigurationFirebase.getFirebaseDatabase();
        this.donationId = firebaseRef.child("received_donations").push().getKey();
    }

    public void save() {
        DatabaseReference firebaseRef = ConfigurationFirebase.getFirebaseDatabase();
        DatabaseReference donationRef = firebaseRef.child("received_donations").child(this.getDonationId());
        donationRef.setValue(this);
    }

    // --- Getters and Setters ---

    public String getDonationId() {
        return donationId;
    }

    public void setDonationId(String donationId) {
        this.donationId = donationId;
    }

    public String getEstablishmentId() {
        return establishmentId;
    }

    public void setEstablishmentId(String establishmentId) {
        this.establishmentId = establishmentId;
    }

    public long getReceivedCreatedAt() {
        return receivedCreatedAt;
    }

    public void setReceivedCreatedAt(long receivedCreatedAt) {
        this.receivedCreatedAt = receivedCreatedAt;
    }

    public String getReceivedScheduledDateTime() {
        return receivedScheduledDateTime;
    }

    public void setReceivedScheduledDateTime(String receivedScheduledDateTime) {
        this.receivedScheduledDateTime = receivedScheduledDateTime;
    }

    public String getReceivedStatus() {
        return receivedStatus;
    }

    public void setReceivedStatus(String receivedStatus) {
        this.receivedStatus = receivedStatus;
    }

    public Map<String, ReceivedDonationItem> getReceivedItems() {
        return receivedItems;
    }

    public void setReceivedItems(Map<String, ReceivedDonationItem> receivedItems) {
        this.receivedItems = receivedItems;
    }
}
