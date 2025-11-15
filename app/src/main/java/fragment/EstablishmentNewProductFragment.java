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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import activity.EstablishmentActivity;
import adapter.HintAdapter;
import helper.ConfigurationFirebase;
import helper.UserFirebase;
import model.Product;
import model.ReceivedDonation;
import model.ReceivedDonationItem;

// RE-ARCH: Fixing the root cause of the data corruption - capturing the productId from the Firebase key.
public class EstablishmentNewProductFragment extends Fragment {

    private Spinner spinnerProduct;
    private TextInputEditText editProductQuantity, editExpirationDate, editScheduledDate, editScheduledTime, textProductUnit;
    private Button buttonSaveDonation;

    private String idCurrentUser;
    private DatabaseReference firebaseRef;
    private List<Product> productCatalogList = new ArrayList<>();

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
        if (getActivity() instanceof EstablishmentActivity) {
            ((EstablishmentActivity) getActivity()).setToolbarTitle("Realizar Doação", EstablishmentActivity.TitleAlignment.CENTER);
        }
    }

    private void setupClickListeners() {
        View.OnClickListener dateClickListener = v -> {
            hideKeyboard(v);
            showDatePicker((TextInputEditText) v);
        };
        editExpirationDate.setOnClickListener(dateClickListener);
        editScheduledDate.setOnClickListener(dateClickListener);
        
        editScheduledTime.setOnClickListener(v -> {
            hideKeyboard(v);
            showTimePicker();
        });

        buttonSaveDonation.setOnClickListener(v -> validateDataAndSave());
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
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (dView, year, month, dayOfMonth) -> {
            String selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year);
            dateField.setText(selectedDate);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void showTimePicker() {
        if (getContext() == null) return;
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (tView, hourOfDay, minute) -> {
            String selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            editScheduledTime.setText(selectedTime);
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
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
                        // THE FIX: Capture the product ID from the snapshot's key.
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

    private void validateDataAndSave() {
        if (spinnerProduct.getSelectedItemPosition() == 0) { showMessage("Selecione o produto"); return; }
        String quantityStr = editProductQuantity.getText().toString();
        if (quantityStr.isEmpty()) { showMessage("Digite a quantidade"); return; }
        String expirationDate = editExpirationDate.getText().toString();
        if (expirationDate.isEmpty()) { showMessage("Selecione a data de validade do produto"); return; }
        String scheduledDate = editScheduledDate.getText().toString();
        if (scheduledDate.isEmpty()) { showMessage("Selecione a data de disponibilidade"); return; }
        String scheduledTime = editScheduledTime.getText().toString();
        if (scheduledTime.isEmpty()) { showMessage("Selecione a hora de disponibilidade"); return; }

        Product selectedProduct = null;
        String selectedProductName = spinnerProduct.getSelectedItem().toString();
        for (Product p : productCatalogList) {
            if (p.getProductName().equals(selectedProductName)) { selectedProduct = p; break; }        }
        if (selectedProduct == null) { showMessage("Erro: Produto selecionado inválido."); return; }

        ReceivedDonationItem donationItem = new ReceivedDonationItem();
        donationItem.setProductId(selectedProduct.getProductId());
        donationItem.setProductName(selectedProduct.getProductName());
        donationItem.setProductUnitType(selectedProduct.getProductUnitType());
        donationItem.setReceivedQuantity(Double.parseDouble(quantityStr));
        donationItem.setReceivedExpirationDate(expirationDate);

        ReceivedDonation newDonation = new ReceivedDonation();
        newDonation.setEstablishmentId(idCurrentUser);
        newDonation.setReceivedCreatedAt(System.currentTimeMillis());
        newDonation.setReceivedScheduledDateTime(scheduledDate + " " + scheduledTime);
        
        Map<String, ReceivedDonationItem> itemsMap = new HashMap<>();
        String itemKey = firebaseRef.child("received_donations").child(newDonation.getDonationId()).child("receivedItems").push().getKey();
        itemsMap.put(itemKey, donationItem);
        newDonation.setReceivedItems(itemsMap);

        newDonation.save();

        showMessage("Promessa de doação registrada com sucesso!");
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
        editScheduledDate = view.findViewById(R.id.editScheduledDate);
        editScheduledTime = view.findViewById(R.id.editScheduledTime);
        buttonSaveDonation = view.findViewById(R.id.buttonSaveDonation);
    }
}
