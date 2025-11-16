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

import fragment.EstablishmentHistoryFragment;
import fragment.EstablishmentListProductsFragment;
import fragment.EstablishmentNewProductFragment;
import fragment.EstablishmentProfileFragment;
import helper.ConfigurationFirebase;

public class EstablishmentActivity extends AppCompatActivity {

    private FirebaseAuth authentication;
    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;
    private TextView toolbarTitleLeft, toolbarTitleCentered;

    public enum TitleAlignment {
        LEFT,
        CENTER
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_establishment);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbarTitleLeft = toolbar.findViewById(R.id.toolbar_title_left);
        toolbarTitleCentered = toolbar.findViewById(R.id.toolbar_title_centered);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }

        authentication = ConfigurationFirebase.getFirebaseAuthentication();
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigationView(bottomNavigationView);

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_establishment_products);
        }
    }

    public void setToolbarTitle(String title, TitleAlignment alignment) {
        if (toolbar == null || toolbarTitleLeft == null || toolbarTitleCentered == null) return;

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

    private void setupBottomNavigationView(BottomNavigationView bottomNav) {
        bottomNav.inflateMenu(R.menu.bottom_nav_menu_establishment);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_establishment_products) {
                selectedFragment = new EstablishmentListProductsFragment();
            } else if (itemId == R.id.nav_add_product) {
                selectedFragment = new EstablishmentNewProductFragment();
            } else if (itemId == R.id.nav_establishment_history) {
                selectedFragment = new EstablishmentHistoryFragment();
            } else if (itemId == R.id.nav_establishment_profile) {
                selectedFragment = new EstablishmentProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_establishment, fragment)
                    .commit();
        }
    }

    public void navigateToMyProducts() {
        loadFragment(new EstablishmentListProductsFragment());
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_establishment_products);
        }
    }

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
