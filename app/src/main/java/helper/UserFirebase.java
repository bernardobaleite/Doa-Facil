package helper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class UserFirebase {
    public static String getIdUser() {
        FirebaseAuth authentication = ConfigurationFirebase.getFirebaseAuthentication();
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public static FirebaseUser getCurrentUser(){
        FirebaseAuth user = ConfigurationFirebase.getFirebaseAuthentication();
        return user.getCurrentUser();

    }

    public static boolean updateTypeUser(String type){
        try{

            FirebaseUser user = getCurrentUser();
            UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                    .setDisplayName(type)
                    .build();
            user.updateProfile(profile);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }


    }

}
