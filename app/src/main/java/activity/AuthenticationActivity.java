package activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.company.doafacil.R;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

import helper.ConfigurationFirebase;
import helper.UserFirebase;

public class AuthenticationActivity extends AppCompatActivity {
    private Button buttonAccess;
    private EditText emailField, passwordField, editAdminPassword;
    private MaterialButtonToggleGroup toggleGroupAccessType, toggleGroupUserType;
    private FirebaseAuth authentication;
    private TextView textAdminLogin;
    private ConstraintLayout adminInputLayout;
    private ImageButton buttonAdminConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.yellow));
        new WindowInsetsControllerCompat(window, window.getDecorView()).setAppearanceLightStatusBars(true);

        initializeComponents();
        authentication = ConfigurationFirebase.getFirebaseAuthentication();
        authentication.signOut();

        verifyCurrentUser();

        setupToggleListeners();
        setupAuthButtonListener();
        setupAdminLoginInteraction();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void setupToggleListeners() {
        toggleGroupAccessType.check(R.id.button_login);
        toggleGroupUserType.check(R.id.button_ong);

        toggleGroupAccessType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                toggleGroupUserType.setVisibility(checkedId == R.id.button_register ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void setupAuthButtonListener() {
        buttonAccess.setOnClickListener(v -> {
            String email = emailField.getText().toString();
            String password = passwordField.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(AuthenticationActivity.this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (toggleGroupAccessType.getCheckedButtonId() == R.id.button_register) {
                registerUser(email, password);
            } else {
                loginUser(email, password);
            }
        });
    }

    private void setupAdminLoginInteraction() {
        textAdminLogin.setOnClickListener(v -> showAdminPasswordEditor());

        editAdminPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                if (adminInputLayout.getVisibility() == View.VISIBLE) {
                    new Handler(Looper.getMainLooper()).postDelayed(this::hideAdminPasswordEditor, 100);
                }
            }
        });

        buttonAdminConfirm.setOnClickListener(v -> checkAdminPassword());

        editAdminPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                checkAdminPassword();
                return true;
            }
            return false;
        });
    }

    private void showAdminPasswordEditor() {
        textAdminLogin.setVisibility(View.GONE);
        adminInputLayout.setVisibility(View.VISIBLE);
        editAdminPassword.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editAdminPassword, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideAdminPasswordEditor() {
        if (adminInputLayout != null) {
            adminInputLayout.setVisibility(View.INVISIBLE);
            textAdminLogin.setVisibility(View.VISIBLE);
        }
    }

    private void checkAdminPassword() {
        String adminPassword = editAdminPassword.getText().toString().trim();
        String adminEmail = "admin@doafacil.com";

        if(adminPassword.isEmpty()){
            Toast.makeText(this, "Digite a senha do administrador", Toast.LENGTH_SHORT).show();
            return;
        }

        authentication.signInWithEmailAndPassword(adminEmail, adminPassword)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        Toast.makeText(this, "Administrador autenticado com sucesso", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, AdministratorActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Senha do administrador incorreta.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void registerUser(String email, String password) {
        authentication.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String typeUser = getTypeUser();
                UserFirebase.updateTypeUser(typeUser);
                openMainScreen(typeUser);
            } else {
                handleAuthError(task);
            }
        });
    }

    private void loginUser(String email, String password) {
        authentication.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String typeUser = task.getResult().getUser().getDisplayName();
                openMainScreen(typeUser);
            } else {
                handleAuthError(task);
            }
        });
    }

    private void handleAuthError(Task<AuthResult> task) {
        String errorException;
        try {
            throw task.getException();
        } catch (FirebaseAuthWeakPasswordException e) {
            errorException = "Digite uma senha mais forte!";
        } catch (FirebaseAuthInvalidCredentialsException e) {
            errorException = "Email ou senha inválido(s)!";
        } catch (FirebaseAuthUserCollisionException e) {
            errorException = "Usuário já cadastrado!";
        } catch (Exception e) {
            errorException = "Erro ao autenticar: " + e.getMessage();
            e.printStackTrace();
        } 
        Toast.makeText(AuthenticationActivity.this, errorException, Toast.LENGTH_SHORT).show();
    }

    private void verifyCurrentUser() {
        FirebaseUser currentUser = authentication.getCurrentUser();
        if (currentUser != null && currentUser.getDisplayName() != null) {
            openMainScreen(currentUser.getDisplayName());
        }
    }

    private String getTypeUser() {
        return toggleGroupUserType.getCheckedButtonId() == R.id.button_establishment ? "E" : "O";
    }

    private void openMainScreen(String typeUser) {
        if ("E".equals(typeUser)) {
            startActivity(new Intent(getApplicationContext(), EstablishmentActivity.class));
        } else if ("O".equals(typeUser)) {
            startActivity(new Intent(getApplicationContext(), OngActivity.class));
        }
        finish();
    }

    private void initializeComponents() {
        toggleGroupAccessType = findViewById(R.id.toggle_group_access_type);
        toggleGroupUserType = findViewById(R.id.toggle_group_user_type);
        emailField = findViewById(R.id.edit_email_field);
        passwordField = findViewById(R.id.edit_password_field);
        buttonAccess = findViewById(R.id.button_access);
        textAdminLogin = findViewById(R.id.text_admin_login);
        editAdminPassword = findViewById(R.id.edit_admin_password);
        adminInputLayout = findViewById(R.id.admin_input_layout);
        buttonAdminConfirm = findViewById(R.id.button_admin_confirm);
        authentication = ConfigurationFirebase.getFirebaseAuthentication();
    }
}
