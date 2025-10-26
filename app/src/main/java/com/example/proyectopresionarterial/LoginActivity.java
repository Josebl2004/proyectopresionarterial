package com.example.proyectopresionarterial;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Siempre mostrar la pantalla de inicio de sesión
        setContentView(R.layout.activity_login);

        // Referencia al campo de usuario
        TextInputEditText etUser = findViewById(R.id.etUser);

        // Acción simple de login: marca sesión como iniciada y guarda el nombre
        View btnLogin = findViewById(R.id.btnLogin);
        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                String input = "";
                if (etUser != null && etUser.getText() != null) {
                    input = etUser.getText().toString().trim();
                }

                // Derivar un nombre de visualización: si es email, usar lo anterior al '@'
                String displayName = input;
                int at = input.indexOf('@');
                if (at > 0) {
                    displayName = input.substring(0, at);
                }
                if (displayName.isEmpty()) {
                    displayName = "Usuario"; // Fallback mínimo
                }

                SessionManager.saveUserName(this, displayName);
                SessionManager.saveLoginState(this, true);
                goToMainAndFinish();
            });
        }
    }

    private void goToMainAndFinish() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }
}
