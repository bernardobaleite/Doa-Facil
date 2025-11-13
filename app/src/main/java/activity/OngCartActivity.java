package activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.company.doafacil.R;

import fragment.OngCartFragment;

// RE-ARCH: The new home for the shopping cart, as a separate screen (Activity).
public class OngCartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ong_cart);

        // THE FIX: Use the correct toolbar ID from the included layout.
        Toolbar toolbar = findViewById(R.id.toolbar_layout);
        setSupportActionBar(toolbar);

        TextView toolbarTitleLeft = toolbar.findViewById(R.id.toolbar_title_left);
        TextView toolbarTitleCentered = toolbar.findViewById(R.id.toolbar_title_centered);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_24dp);
        }

        // Set the left-aligned title as per user's vision
        toolbarTitleLeft.setText("Carrinho");
        toolbarTitleLeft.setVisibility(View.VISIBLE);
        toolbarTitleCentered.setVisibility(View.GONE);

        // Load the existing cart fragment into this activity's container
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_cart, new OngCartFragment())
                    .commit();
        }
    }

    // Handle the back arrow click
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Close this activity and return to the previous one
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
