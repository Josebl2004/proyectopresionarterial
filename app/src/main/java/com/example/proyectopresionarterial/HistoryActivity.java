package com.example.proyectopresionarterial;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity implements HistoryAdapter.OnRecordClickListener {

    private HistoryViewModel viewModel;

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

        setContentView(R.layout.activity_history);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24);
            toolbar.setNavigationOnClickListener(v -> finish());
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(getString(R.string.menu_history_title));
            }
        }

        // Inicializar ViewModel a nivel de actividad
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
    }

    @Override
    public void onRecordClick(BloodPressureRecord record) {
        // Abrir diálogo para ver detalles y opciones
        showRecordDialog(record);
    }

    private void showRecordDialog(BloodPressureRecord record) {
        if (record == null) return;

        View dialogView = getLayoutInflater().inflate(R.layout.bottom_sheet_record_details, null);

        // Configurar vista con datos del registro
        TextView tvSystolic = dialogView.findViewById(R.id.tvDetailsSystolic);
        TextView tvDiastolic = dialogView.findViewById(R.id.tvDetailsDiastolic);
        TextView tvHeartRate = dialogView.findViewById(R.id.tvDetailsHeartRate);
        TextView tvTime = dialogView.findViewById(R.id.tvDetailsTime);
        TextView tvCondition = dialogView.findViewById(R.id.tvDetailsCondition);
        TextView tvClassification = dialogView.findViewById(R.id.tvDetailsClassification);

        // Rellenar datos
        tvSystolic.setText(String.valueOf(record.getSystolic()));
        tvDiastolic.setText(String.valueOf(record.getDiastolic()));
        tvHeartRate.setText(String.valueOf(record.getHeartRate()));
        tvTime.setText(record.getTime());
        tvCondition.setText(record.getCondition());

        String classification = record.getClassification();
        if (classification == null || classification.isEmpty()) {
            classification = ClassificationHelper.classify(record.getSystolic(), record.getDiastolic());
        }
        tvClassification.setText(classification);

        // Configurar botones
        MaterialButton btnShare = dialogView.findViewById(R.id.btnShareRecord);
        MaterialButton btnEdit = dialogView.findViewById(R.id.btnEditRecord);
        MaterialButton btnDelete = dialogView.findViewById(R.id.btnDeleteRecord);

        btnShare.setOnClickListener(v -> shareRecord(record));
        btnEdit.setOnClickListener(v -> editRecord(record));
        btnDelete.setOnClickListener(v -> confirmDelete(record));

        // Mostrar diálogo
        new MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setNegativeButton(R.string.btn_close, null)
            .show();
    }

    private void shareRecord(BloodPressureRecord record) {
        String shareText = String.format(getString(R.string.share_record_template),
                record.getSystolic(), record.getDiastolic(), record.getHeartRate(),
                record.getDate(), record.getTime(),
                record.getClassification() != null ? record.getClassification() :
                    ClassificationHelper.classify(record.getSystolic(), record.getDiastolic()));

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)));
    }

    private void editRecord(BloodPressureRecord record) {
        Intent intent = new Intent(this, AddRecordActivity.class);
        intent.putExtra("RECORD_ID", record.getId());
        startActivity(intent);
    }

    private void confirmDelete(BloodPressureRecord record) {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_record_title)
            .setMessage(R.string.delete_record_confirmation)
            .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                viewModel.deleteRecord(record);
                Snackbar.make(findViewById(android.R.id.content), R.string.record_deleted, Snackbar.LENGTH_SHORT).show();
            })
            .setNegativeButton(R.string.btn_cancel, null)
            .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_filter) {
            showFilterDialog();
            return true;
        } else if (item.getItemId() == R.id.action_export) {
            exportData();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showFilterDialog() {
        String[] options = {
            getString(R.string.filter_normal),
            getString(R.string.filter_elevated),
            getString(R.string.filter_hypertension),
            getString(R.string.filter_date_asc),
            getString(R.string.filter_date_desc),
            getString(R.string.filter_value_asc),
            getString(R.string.filter_value_desc)
        };

        boolean[] checkedItems = {
            viewModel.isFilterNormal(),
            viewModel.isFilterElevated(),
            viewModel.isFilterHypertension(),
            viewModel.isOrderByDateAsc(),
            viewModel.isOrderByDateDesc(),
            viewModel.isOrderByValueAsc(),
            viewModel.isOrderByValueDesc()
        };

        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.filter_title)
            .setMultiChoiceItems(options, checkedItems, (dialog, which, isChecked) -> {
                // Manejar exclusividad de ciertos filtros
                if (which >= 3) { // Filtros de ordenamiento
                    for (int i = 3; i < checkedItems.length; i++) {
                        if (i != which) {
                            checkedItems[i] = false;
                            // Actualizar diálogo
                            ((androidx.appcompat.app.AlertDialog)dialog)
                                .getListView().setItemChecked(i, false);
                        }
                    }
                }
                checkedItems[which] = isChecked;
            })
            .setPositiveButton(R.string.btn_apply, (dialog, which) -> {
                // Aplicar filtros
                viewModel.setFilters(
                    checkedItems[0], // normal
                    checkedItems[1], // elevada
                    checkedItems[2], // hipertensión
                    checkedItems[3], // fecha asc
                    checkedItems[4], // fecha desc
                    checkedItems[5], // valor asc
                    checkedItems[6]  // valor desc
                );
                // Notificar al fragmento que se han actualizado los filtros
                viewModel.loadRecords(HistoryViewModel.FILTER_ALL, new ArrayList<>());
            })
            .setNegativeButton(R.string.btn_cancel, null)
            .setNeutralButton(R.string.btn_reset, (dialog, which) -> {
                viewModel.resetFilters();
                // Notificar al fragmento que se han reseteado los filtros
                viewModel.loadRecords(HistoryViewModel.FILTER_ALL, new ArrayList<>());
            })
            .show();
    }

    private void exportData() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.export_title)
            .setItems(new String[]{
                    getString(R.string.export_csv),
                    getString(R.string.export_pdf),
                    getString(R.string.export_share)
            }, (dialog, which) -> {
                switch (which) {
                    case 0:
                        viewModel.exportAsCsv(this);
                        break;
                    case 1:
                        viewModel.exportAsPdf(this);
                        break;
                    case 2:
                        viewModel.shareCurrentView(this);
                        break;
                }
            })
            .setNegativeButton(R.string.btn_cancel, null)
            .show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Recargar los datos cuando la actividad vuelve a primer plano
        viewModel.refreshData();

        // Asegurarse de que se actualice la lista de registros
        if (getSupportFragmentManager().findFragmentById(R.id.fragment_container_history) instanceof HistoryFragment) {
            HistoryFragment fragment = (HistoryFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container_history);
            if (fragment != null) {
                fragment.refreshData();
            }
        }
    }
}
