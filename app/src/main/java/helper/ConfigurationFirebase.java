package helper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ConfigurationFirebase {

    private static DatabaseReference referenceDatabase;
    private static FirebaseAuth referenceAuthentication;




    public static DatabaseReference getFirebaseDatabase() {
        if (referenceDatabase == null) {
            referenceDatabase = FirebaseDatabase.getInstance().getReference();
        }
        return referenceDatabase;
    }

    public static FirebaseAuth getFirebaseAuthentication() {
        if (referenceAuthentication == null) {
            referenceAuthentication = FirebaseAuth.getInstance();
        }
        return referenceAuthentication;
    }
}
