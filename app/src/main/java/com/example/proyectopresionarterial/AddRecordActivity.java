package com.example.proyectopresionarterial;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddRecordActivity extends AppCompatActivity {

    private AddRecordViewModel viewModel;
    private TextInputEditText etSystolic;
    private TextInputEditText etDiastolic;
    private TextInputEditText etHeartRate;
    private TextInputLayout tilSystolic;
    private TextInputLayout tilDiastolic;
    private TextInputLayout tilHeart;
    private RadioGroup rgCondition;
    private MaterialButton btnSave;
    private String selectedTime;
    private String selectedDate;
    private boolean isEditing = false;
    private long recordId = -1;

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

        setContentView(R.layout.activity_add_record);

        // Configurar Toolbar de navegación atrás
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24);
            toolbar.setNavigationOnClickListener(v -> confirmExit());
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(getString(R.string.title_new_measure));
            }
        }

        viewModel = new ViewModelProvider(this).get(AddRecordViewModel.class);

        // Inicializar vistas
        initializeViews();

        // Configurar fecha y hora actuales por defecto
        setupDateAndTime();

        // Comprobar si estamos editando un registro existente
        checkForEditMode();

        // Observar cambios del viewmodel
        setupViewModelObservers();

        // Configurar listeners para los botones y elementos interactivos
        setupListeners();
    }

    private void initializeViews() {
        etSystolic = findViewById(R.id.etSystolic);
        etDiastolic = findViewById(R.id.etDiastolic);
        etHeartRate = findViewById(R.id.etHeartRate);
        tilSystolic = findViewById(R.id.tilSystolic);
        tilDiastolic = findViewById(R.id.tilDiastolic);
        tilHeart = findViewById(R.id.tilHeartRate);
        rgCondition = findViewById(R.id.rgCondition);
        btnSave = findViewById(R.id.btnSave);

        // Selección por defecto (opcional): reposo
        RadioButton rbReposo = findViewById(R.id.rbReposo);
        if (rbReposo != null) rbReposo.setChecked(true);
    }

    private void setupDateAndTime() {
        // Configurar hora y fecha actuales
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        selectedTime = timeFormat.format(calendar.getTime());
        selectedDate = dateFormat.format(calendar.getTime());
    }

    private void checkForEditMode() {
        // Revisar si llegamos con un Intent para editar
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("RECORD_ID")) {
            recordId = intent.getLongExtra("RECORD_ID", -1);
            if (recordId != -1) {
                isEditing = true;
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.title_edit_measure);
                }
                // Cargar datos del registro (usando un método local para evitar problemas de compilación)
                loadRecordData(recordId);
            }
        }
    }

    // Método auxiliar para cargar datos de un registro
    private void loadRecordData(long id) {
        if (id <= 0) return;

        // Mostrar indicador de carga si es necesario
        // findViewById(R.id.progressLoading).setVisibility(View.VISIBLE);

        // Usar executor para operación en segundo plano
        new Thread(() -> {
            try {
                // Obtener el registro directamente desde la base de datos
                BloodPressureRecord record = AppDatabase.getInstance(this).bloodPressureDao().getById(id);

                // Actualizar UI en el hilo principal
                runOnUiThread(() -> {
                    // Ocultar indicador de carga si es necesario
                    // findViewById(R.id.progressLoading).setVisibility(View.GONE);

                    if (record != null) {
                        // Rellenar campos con datos del registro
                        etSystolic.setText(String.valueOf(record.getSystolic()));
                        etDiastolic.setText(String.valueOf(record.getDiastolic()));
                        etHeartRate.setText(String.valueOf(record.getHeartRate()));
                        selectedTime = record.getTime();
                        selectedDate = record.getDate();

                        // Condición
                        String condition = record.getCondition();
                        if (condition != null) {
                            if (condition.equalsIgnoreCase("reposo")) {
                                RadioButton rb = findViewById(R.id.rbReposo);
                                if (rb != null) rb.setChecked(true);
                            } else if (condition.equalsIgnoreCase("ejercicio")) {
                                RadioButton rb = findViewById(R.id.rbEjercicio);
                                if (rb != null) rb.setChecked(true);
                            } else if (condition.equalsIgnoreCase("estrés") ||
                                    condition.equalsIgnoreCase("estres")) {
                                RadioButton rb = findViewById(R.id.rbEstres);
                                if (rb != null) rb.setChecked(true);
                            }
                        }
                    } else {
                        // Mostrar error si el registro no existe
                        Snackbar.make(findViewById(android.R.id.content),
                                "No se encontró el registro", Snackbar.LENGTH_LONG).show();
                        finish();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    // Ocultar indicador de carga si es necesario
                    // findViewById(R.id.progressLoading).setVisibility(View.GONE);

                    // Mostrar error
                    Snackbar.make(findViewById(android.R.id.content),
                            "Error al cargar: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private void setupViewModelObservers() {
        viewModel.getErrorMessage().observe(this, msg -> {
            // mapeo básico a inputs si coincide
            if (msg == null) return;
            if (msg.toLowerCase().contains("sist")) tilSystolic.setError(msg);
            else if (msg.toLowerCase().contains("diast")) tilDiastolic.setError(msg);
            else if (msg.toLowerCase().contains("frecuencia")) tilHeart.setError(msg);
            else showMessage(msg);
            btnSave.setEnabled(true);
            findViewById(R.id.progressSave).setVisibility(View.GONE);
        });

        viewModel.getSaveSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                findViewById(R.id.progressSave).setVisibility(View.GONE);
                showMessage(getString(isEditing ? R.string.record_updated : R.string.record_saved));
                finish();
            } else {
                btnSave.setEnabled(true);
                findViewById(R.id.progressSave).setVisibility(View.GONE);
            }
        });

        viewModel.getRecord().observe(this, record -> {
            if (record != null && isEditing) {
                // Rellenar campos con datos del registro
                etSystolic.setText(String.valueOf(record.getSystolic()));
                etDiastolic.setText(String.valueOf(record.getDiastolic()));
                etHeartRate.setText(String.valueOf(record.getHeartRate()));
                selectedTime = record.getTime();
                selectedDate = record.getDate();

                // Condición
                String condition = record.getCondition();
                if (condition != null) {
                    if (condition.equalsIgnoreCase("reposo")) {
                        RadioButton rb = findViewById(R.id.rbReposo);
                        if (rb != null) rb.setChecked(true);
                    } else if (condition.equalsIgnoreCase("ejercicio")) {
                        RadioButton rb = findViewById(R.id.rbEjercicio);
                        if (rb != null) rb.setChecked(true);
                    } else if (condition.equalsIgnoreCase("estrés") ||
                               condition.equalsIgnoreCase("estres")) {
                        RadioButton rb = findViewById(R.id.rbEstres);
                        if (rb != null) rb.setChecked(true);
                    }
                }
            }
        });
    }

    private void setupListeners() {
        // Botón guardar
        btnSave.setOnClickListener(v -> {
            clearErrors(tilSystolic, tilDiastolic, tilHeart);
            String systolicStr = etSystolic.getText() != null ? etSystolic.getText().toString().trim() : "";
            String diastolicStr = etDiastolic.getText() != null ? etDiastolic.getText().toString().trim() : "";
            String heartRateStr = etHeartRate.getText() != null ? etHeartRate.getText().toString().trim() : "";
            String time = selectedTime;
            String date = selectedDate;

            String condition = null;
            int checkedId = rgCondition.getCheckedRadioButtonId();
            if (checkedId == R.id.rbReposo) condition = "reposo";
            else if (checkedId == R.id.rbEjercicio) condition = "ejercicio";
            else if (checkedId == R.id.rbEstres) condition = "estrés";

            if (validateInputsInline(systolicStr, diastolicStr, heartRateStr, time, date,
                    tilSystolic, tilDiastolic, tilHeart)) {
                btnSave.setEnabled(false);
                findViewById(R.id.progressSave).setVisibility(View.VISIBLE);

                if (isEditing) {
                    viewModel.updateRecord(recordId, systolicStr, diastolicStr, heartRateStr,
                                         condition, time, date, "");
                } else {
                    viewModel.saveRecord(systolicStr, diastolicStr, heartRateStr, condition,
                                        time, date, "");
                }
            }
        });
    }

    private void confirmExit() {
        // Si hemos modificado algo, confirmar salida
        if (hasChanges()) {
            new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.exit_title)
                .setMessage(R.string.exit_confirmation)
                .setPositiveButton(R.string.btn_exit, (dialog, which) -> finish())
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
        } else {
            finish();
        }
    }

    private boolean hasChanges() {
        // Verificar si se han hecho cambios en los campos
        if (etSystolic.getText() != null && !etSystolic.getText().toString().isEmpty()) return true;
        if (etDiastolic.getText() != null && !etDiastolic.getText().toString().isEmpty()) return true;
        if (etHeartRate.getText() != null && !etHeartRate.getText().toString().isEmpty()) return true;
        return false;
    }

    private void clearErrors(TextInputLayout... layouts) {
        for (TextInputLayout l : layouts) if (l != null) l.setError(null);
    }

    private void showMessage(String msg) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).show();
    }

    private boolean validateInputsInline(String sSys, String sDia, String sHr, String time, String date,
                                     TextInputLayout tilSys, TextInputLayout tilDia, TextInputLayout tilHr) {
        boolean ok = true;
        if (sSys.isEmpty()) { tilSys.setError(getString(R.string.error_empty_fields)); ok = false; }
        if (sDia.isEmpty()) { tilDia.setError(getString(R.string.error_empty_fields)); ok = false; }
        if (sHr.isEmpty()) { tilHr.setError(getString(R.string.error_empty_fields)); ok = false; }
        if (time.isEmpty() || date.isEmpty()) {
            showMessage(getString(R.string.error_invalid_datetime));
            ok = false;
        }
        if (!ok) return false;

        try {
            int sys = Integer.parseInt(sSys);
            if (sys < 50 || sys > 250) {
                tilSys.setError(getString(R.string.error_invalid_values));
                ok = false;
            }
        } catch (NumberFormatException e) {
            tilSys.setError(getString(R.string.error_invalid_values));
            ok = false;
        }

        try {
            int dia = Integer.parseInt(sDia);
            if (dia < 30 || dia > 150) {
                tilDia.setError(getString(R.string.error_invalid_values));
                ok = false;
            }
        } catch (NumberFormatException e) {
            tilDia.setError(getString(R.string.error_invalid_values));
            ok = false;
        }

        try {
            int hr = Integer.parseInt(sHr);
            if (hr < 30 || hr > 220) {
                tilHr.setError(getString(R.string.error_invalid_values));
                ok = false;
            }
        } catch (NumberFormatException e) {
            tilHr.setError(getString(R.string.error_invalid_values));
            ok = false;
        }

        return ok;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isEditing) {
            getMenuInflater().inflate(R.menu.menu_edit_record, menu);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            confirmExit();
            return true;
        } else if (item.getItemId() == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_title)
            .setMessage(R.string.delete_confirmation)
            .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                viewModel.deleteRecord(recordId);
                showMessage(getString(R.string.record_deleted));
                finish();
            })
            .setNegativeButton(R.string.btn_cancel, null)
            .show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Si estamos en modo de edición, refrescar los datos del registro
        if (isEditing && recordId > 0) {
            viewModel.loadRecord(recordId);
        }
    }
}
