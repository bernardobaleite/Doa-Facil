package fragment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.company.doafacil.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import activity.EstablishmentActivity;
import adapter.HintAdapter;
import helper.ConfigurationFirebase;
import helper.UserFirebase;
import model.Product;
import model.StockItem;

// RE-ARCH: Refactoring to use the new decoupled toolbar control from EstablishmentActivity.
public class EstablishmentNewProductFragment extends Fragment {

    private Spinner spinnerProduct;
    private TextInputEditText editProductQuantity, editExpirationDate, editPickupDate, editPickupTime, textProductUnit;
    private Button buttonSaveProduct;

    private String idCurrentUser;
    private DatabaseReference firebaseRef;
    private List<Product> productCatalogList = new ArrayList<>();

    public EstablishmentNewProductFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_establishment_new_product, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeComponents(view);
        idCurrentUser = UserFirebase.getIdUser();
        firebaseRef = ConfigurationFirebase.getFirebaseDatabase();

        loadProductCatalog();
        setupClickListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        // THE FIX: Set the toolbar title using the new decoupled method.
        if (getActivity() instanceof EstablishmentActivity) {
            ((EstablishmentActivity) getActivity()).setToolbarTitle("Realize uma doação", EstablishmentActivity.TitleAlignment.CENTER);
        }
    }

    private void setupClickListeners() {
        View.OnClickListener dateClickListener = v -> {
            hideKeyboard(v);
            showDatePicker((TextInputEditText) v);
        };
        editPickupDate.setOnClickListener(dateClickListener);
        editExpirationDate.setOnClickListener(dateClickListener);

        editPickupTime.setOnClickListener(v -> {
            hideKeyboard(v);
            showTimePicker();
        });

        buttonSaveProduct.setOnClickListener(v -> validateDataProduct());
    }

    private void hideKeyboard(View view) {
        if (getContext() != null) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showDatePicker(TextInputEditText dateField) {
        if (getContext() == null) return;
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (dView, year1, month1, dayOfMonth) -> {
            String selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month1 + 1, year1);
            dateField.setText(selectedDate);
        }, year, month, day);
        datePickerDialog.show();
    }

    private void showTimePicker() {
        if (getContext() == null) return;
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (tView, hourOfDay, minute1) -> {
            String selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1);
            editPickupTime.setText(selectedTime);
        }, hour, minute, true);
        timePickerDialog.show();
    }

    private void loadProductCatalog() {
        DatabaseReference catalogRef = firebaseRef.child("establishment_product_catalog");
        catalogRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                productCatalogList.clear();
                List<String> productNames = new ArrayList<>();
                productNames.add("Selecione o produto");

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Product product = ds.getValue(Product.class);
                    if (product != null && product.getProductName() != null) {
                        product.setProductId(ds.getKey());
                        productCatalogList.add(product);
                        productNames.add(product.getProductName());
                    }
                }
                Collections.sort(productNames.subList(1, productNames.size()));
                setupProductSpinner(productNames);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showMessage("Falha ao carregar o catálogo: " + databaseError.getMessage());
            }
        });
    }

    private void setupProductSpinner(List<String> productNames) {
        if (getContext() == null) return;
        HintAdapter productAdapter = new HintAdapter(getContext(), android.R.layout.simple_spinner_item, productNames);
        productAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProduct.setAdapter(productAdapter);

        spinnerProduct.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    String selectedProductName = (String) parent.getItemAtPosition(position);
                    updateUnitForSelectedProduct(selectedProductName);
                } else {
                    textProductUnit.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                textProductUnit.setText("");
            }
        });
    }

    private void updateUnitForSelectedProduct(String productName) {
        for (Product product : productCatalogList) {
            if (product.getProductName().equals(productName)) {
                textProductUnit.setText(product.getProductUnitType());
                return;
            }
        }
        textProductUnit.setText("");
    }

    private void validateDataProduct() {
        if (spinnerProduct.getSelectedItemPosition() == 0) {
            showMessage("Selecione o produto");
            return;
        }

        String quantityStr = editProductQuantity.getText().toString();
        if (quantityStr.isEmpty()) {
            showMessage("Digite a quantidade");
            return;
        }

        String expirationDate = editExpirationDate.getText().toString();
        if (expirationDate.isEmpty()) {
            showMessage("Selecione a data de validade");
            return;
        }

        String pickupDate = editPickupDate.getText().toString();
        String pickupTime = editPickupTime.getText().toString();

        String selectedProductName = spinnerProduct.getSelectedItem().toString();
        String productId = null;

        for (Product product : productCatalogList) {
            if (product.getProductName().equals(selectedProductName)) {
                productId = product.getProductId();
                break;
            }
        }

        if (productId == null) {
            showMessage("Erro: ID do produto não encontrado.");
            return;
        }

        StockItem stockItem = new StockItem();
        stockItem.setEstablishmentId(idCurrentUser);
        stockItem.setProductId(productId);
        stockItem.setStockItemQuantity(Double.parseDouble(quantityStr));
        stockItem.setStockItemExpirationDate(expirationDate);
        stockItem.setStockItemPickupDate(pickupDate);
        stockItem.setStockItemPickupTime(pickupTime);

        stockItem.save();

        showMessage("Doação registrada! Aguardando aprovação.");

        if (getActivity() instanceof EstablishmentActivity) {
            ((EstablishmentActivity) getActivity()).navigateToMyProducts();
        }
    }

    private void showMessage(String text) {
        if (getContext() != null) {
            Toast.makeText(getContext(), text, Toast.LENGTH_LONG).show();
        }
    }

    private void initializeComponents(View view) {
        spinnerProduct = view.findViewById(R.id.spinnerProduct);
        editProductQuantity = view.findViewById(R.id.editProductQuantity);
        textProductUnit = view.findViewById(R.id.textProductUnit);
        editExpirationDate = view.findViewById(R.id.editExpirationDate);
        editPickupDate = view.findViewById(R.id.editPickupDate);
        editPickupTime = view.findViewById(R.id.editPickupTime);
        buttonSaveProduct = view.findViewById(R.id.buttonSaveProduct);
    }
}
