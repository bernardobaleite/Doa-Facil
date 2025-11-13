package fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.company.doafacil.R;

import activity.AdministratorActivity;

// RE-ARCH: Refactoring to use the new decoupled toolbar control from AdministratorActivity.
public class AdminProfileFragment extends Fragment {

    public AdminProfileFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set the static name for the admin profile
        TextView adminName = view.findViewById(R.id.profile_admin_name);
        if (adminName != null) {
            adminName.setText("Administrador");
        }

        // Find the TextView for logout
        TextView buttonLogout = view.findViewById(R.id.profile_admin_logout);
        buttonLogout.setOnClickListener(v -> {
            if (getActivity() instanceof AdministratorActivity) {
                ((AdministratorActivity) getActivity()).signOutUser();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // THE FIX: Set the toolbar title using the new decoupled method. Per original logic, the title is empty.
        if (getActivity() instanceof AdministratorActivity) {
            ((AdministratorActivity) getActivity()).setToolbarTitle("", AdministratorActivity.TitleAlignment.CENTER);
        }
    }
}
