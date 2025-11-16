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

import fragment.OngCartFragment;
import fragment.OngHomeFragment;
import fragment.OngListProductsFragment;
import fragment.OngOrdersFragment;
import fragment.OngProfileFragment;
import helper.ConfigurationFirebase;

public class OngActivity extends AppCompatActivity {
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
        setContentView(R.layout.activity_ong);

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
            bottomNavigationView.setSelectedItemId(R.id.nav_ong_home);
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

    public void hideToolbar() {
        if (toolbar != null) {
            toolbar.setVisibility(View.GONE);
        }
    }

    public void showToolbar() {
        if (toolbar != null) {
            toolbar.setVisibility(View.VISIBLE);
        }
    }

    private void setupBottomNavigationView(BottomNavigationView bottomNav) {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_ong_home) {
                loadFragment(new OngHomeFragment());
                return true;
            } else if (itemId == R.id.nav_ong_products) {
                loadFragment(new OngListProductsFragment());
                return true;
            } else if (itemId == R.id.nav_ong_cart) {
                loadFragment(new OngCartFragment());
                return true;
            } else if (itemId == R.id.nav_ong_orders) {
                loadFragment(new OngOrdersFragment());
                return true;
            } else if (itemId == R.id.nav_ong_account) {
                loadFragment(new OngProfileFragment());
                return true;
            }
            return false;
        });
    }
    
    public void navigateToCart() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_ong_cart);
        }
    }

    public void navigateToOrders() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_ong_orders);
        }
    }

    private void loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_ong, fragment)
                    .commit();
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
