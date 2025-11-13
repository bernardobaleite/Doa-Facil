package activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.company.doafacil.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import fragment.AdminApprovalsFragment;
import fragment.AdminDonationsFragment;
import fragment.AdminProfileFragment;
import helper.ConfigurationFirebase;

// RE-ARCH: Implementing the flexible, decoupled toolbar control abstraction.
public class AdministratorActivity extends AppCompatActivity {

    private FirebaseAuth authentication;
    private Toolbar toolbar;
    private TextView toolbarTitleLeft, toolbarTitleCentered;

    public enum TitleAlignment {
        LEFT,
        CENTER
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_administrator);

        toolbar = findViewById(R.id.toolbar_layout);
        setSupportActionBar(toolbar);

        toolbarTitleLeft = toolbar.findViewById(R.id.toolbar_title_left);
        toolbarTitleCentered = toolbar.findViewById(R.id.toolbar_title_centered);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        authentication = ConfigurationFirebase.getFirebaseAuthentication();

        BottomNavigationView bottomNav = findViewById(R.id.admin_bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_admin_approvals);
        }
    }

    public void setToolbarTitle(String title, TitleAlignment alignment) {
        if (toolbar == null || toolbarTitleLeft == null || toolbarTitleCentered == null) return;
        toolbar.setVisibility(View.VISIBLE);

        if (title.isEmpty()) {
             toolbarTitleLeft.setVisibility(View.GONE);
             toolbarTitleCentered.setVisibility(View.GONE);
             return;
        }

        if (alignment == TitleAlignment.LEFT) {
            toolbarTitleLeft.setText(title);
            toolbarTitleLeft.setVisibility(View.VISIBLE);
            toolbarTitleCentered.setVisibility(View.GONE);
        } else { // CENTER
            toolbarTitleCentered.setText(title);
            toolbarTitleCentered.setVisibility(View.VISIBLE);
            toolbarTitleLeft.setVisibility(View.GONE);
        }
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener = item -> {
        Fragment selectedFragment = null;

        int itemId = item.getItemId();
        if (itemId == R.id.nav_admin_approvals) {
            selectedFragment = new AdminApprovalsFragment();
        } else if (itemId == R.id.nav_admin_donations) {
            selectedFragment = new AdminDonationsFragment();
        } else if (itemId == R.id.nav_admin_profile) {
            selectedFragment = new AdminProfileFragment();
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.admin_fragment_container, selectedFragment).commit();
        }

        return true;
    };

    public void signOutUser() {
        try {
            if (authentication != null) {
                authentication.signOut();
                Intent intent = new Intent(this, AuthenticationActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
