package com.example.proyectopresionarterial;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditProfileActivity extends AppCompatActivity {

    private UserProfileViewModel vm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bloquear acceso si no hay sesión
        if (!SessionManager.isLoggedIn(this)) {
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
            return;
        }

        setContentView(R.layout.activity_edit_profile);

        // Configurar Toolbar como ActionBar con flecha atrás
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setTitle("Perfil");
            }
        }

        TextInputEditText etWeight = findViewById(R.id.etWeightLbs);
        TextInputEditText etHeight = findViewById(R.id.etHeightCm);
        TextInputEditText etDob = findViewById(R.id.etDateOfBirth);
        Spinner spGender = findViewById(R.id.spGender);
        MaterialButton btnSave = findViewById(R.id.btnSaveProfile);

        // Date picker
        etDob.setInputType(InputType.TYPE_NULL);
        etDob.setFocusable(false);
        etDob.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog dp = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar c = Calendar.getInstance();
                c.set(year, month, dayOfMonth, 0, 0, 0);
                String d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
                etDob.setText(d);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dp.getDatePicker().setMaxDate(System.currentTimeMillis());
            dp.show();
        });

        // Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.gender_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGender.setAdapter(adapter);

        vm = new ViewModelProvider(this).get(UserProfileViewModel.class);
        vm.getProfile().observe(this, profile -> {
            if (profile != null) {
                if (profile.weightLbs != null) etWeight.setText(String.valueOf(profile.weightLbs));
                if (profile.heightCm != null) etHeight.setText(String.valueOf(profile.heightCm));
                if (profile.dateOfBirth != null) etDob.setText(profile.dateOfBirth);
                if (profile.gender != null) {
                    int pos = adapter.getPosition(profile.gender);
                    if (pos >= 0) spGender.setSelection(pos);
                }
            }
        });

        btnSave.setOnClickListener(v -> {
            String wStr = etWeight.getText() != null ? etWeight.getText().toString().trim() : "";
            String hStr = etHeight.getText() != null ? etHeight.getText().toString().trim() : "";
            String dob = etDob.getText() != null ? etDob.getText().toString().trim() : "";
            String gender = (String) spGender.getSelectedItem();
            if (validateProfileInputs(wStr, hStr, dob)) {
                vm.saveProfile(wStr, hStr, dob, gender);
            }
        });
    }

    private void showMessage(String msg) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).show();
    }

    private boolean validateProfileInputs(String wStr, String hStr, String dob) {
        Float weight = null;
        Integer height = null;
        try {
            if (!wStr.isEmpty()) weight = Float.parseFloat(wStr);
        } catch (NumberFormatException ignore) {}
        try {
            if (!hStr.isEmpty()) height = Integer.parseInt(hStr);
        } catch (NumberFormatException ignore) {}
        if (weight == null || weight < 45.0f || weight > 660.0f) {
            showMessage(getString(R.string.error_weight_range));
            return false;
        }
        if (height == null || height < 100 || height > 250) {
            showMessage(getString(R.string.error_height_range));
            return false;
        }
        if (dob.isEmpty()) {
            showMessage(getString(R.string.error_dob_required));
            return false;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Recargar los datos del perfil cuando la actividad vuelve a primer plano
        vm.refreshProfile();
    }
}
