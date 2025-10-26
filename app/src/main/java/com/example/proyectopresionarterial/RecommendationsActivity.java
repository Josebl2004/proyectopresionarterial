package com.example.proyectopresionarterial;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

/**
 * Pantalla detallada de Recomendaciones IA, basada en el último registro
 * y enriquecida con consejos rápidos y aviso de emergencia.
 */
public class RecommendationsActivity extends AppCompatActivity {

    private BloodPressureRecord lastRecord; // para reintento
    private String currentRecommendation;
    private MaterialButton btnMoreTips;
    private MaterialCardView cardEmergency;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SessionManager.isLoggedIn(this)) {
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
            return;
        }

        setContentView(R.layout.activity_recommendations);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        MaterialCardView cardSummary = findViewById(R.id.cardSummary);
        TextView tvAssistantTitle = findViewById(R.id.tvAssistantTitle);
        TextView tvAssistantSub = findViewById(R.id.tvAssistantSub);
        TextView tvRecBody = findViewById(R.id.tvRecBody);
        TextView tvLastTitle = findViewById(R.id.tvLastTitle);
        TextView tvLastValues = findViewById(R.id.tvLastValues);
        TextView tvLastPulse = findViewById(R.id.tvLastPulse);
        TextView tvBadge = findViewById(R.id.tvBadge);
        ImageView ivBadgeDot = findViewById(R.id.ivBadgeDot);
        CircularProgressIndicator progress = findViewById(R.id.progressRec);
        MaterialButton btnRetry = findViewById(R.id.btnRetry);
        btnMoreTips = findViewById(R.id.btnMore);
        cardEmergency = findViewById(R.id.cardEmergency);

        // Configuración de botones
        setupButtonListeners();

        tvAssistantTitle.setText(getString(R.string.assistant_title));
        tvAssistantSub.setText(getString(R.string.assistant_subtitle));

        RecommendationViewModel recVm = new ViewModelProvider(this).get(RecommendationViewModel.class);
        MainViewModel mainVm = new ViewModelProvider(this).get(MainViewModel.class);

        // Renderizar última medición + pedir recomendación
        mainVm.getLatest().observe(this, record -> {
            lastRecord = record;
            btnRetry.setVisibility(View.GONE);
            if (record == null) {
                progress.setVisibility(View.GONE);
                tvRecBody.setText(getString(R.string.api_recommendations_empty));
                tvLastTitle.setText(getString(R.string.last_measure));
                tvLastValues.setText("—");
                tvLastPulse.setText("—");
                styleBadge(cardSummary, tvBadge, ivBadgeDot, "");
                updateEmergencyVisibility("");
                return;
            }
            try {
                tvLastTitle.setText(getString(R.string.last_measure));
                tvLastValues.setText(getString(R.string.values_today, record.getSystolic(), record.getDiastolic(), record.getHeartRate()));
                tvLastPulse.setText(getString(R.string.meta_today, record.getDate(), record.getTime(), record.getCondition()));
                String cls = record.getClassification();
                if (cls == null || cls.isEmpty()) cls = ClassificationHelper.classify(record.getSystolic(), record.getDiastolic());
                styleBadge(cardSummary, tvBadge, ivBadgeDot, cls);
                tvRecBody.setText(getString(R.string.api_recommendations_loading));
                progress.setVisibility(View.VISIBLE);
                recVm.fetchForRecord(record);
                updateEmergencyVisibility(cls);
            } catch (Exception e) {
                progress.setVisibility(View.GONE);
                tvRecBody.setText(getString(R.string.api_recommendations_error_detail));
                btnRetry.setVisibility(View.VISIBLE);
            }
        });

        btnRetry.setOnClickListener(v -> {
            if (lastRecord != null) {
                tvRecBody.setText(getString(R.string.api_recommendations_loading));
                progress.setVisibility(View.VISIBLE);
                btnRetry.setVisibility(View.GONE);
                recVm.fetchForRecord(lastRecord);
            }
        });

        recVm.getRecommendation().observe(this, rec -> {
            if (rec != null && !rec.trim().isEmpty()) {
                currentRecommendation = rec.trim();
                tvRecBody.setText(currentRecommendation);
                progress.setVisibility(View.GONE);
                btnRetry.setVisibility(View.GONE);
            } else if (progress.getVisibility() == View.VISIBLE) {
                // sigue cargando o vacío
                tvRecBody.setText(getString(R.string.api_recommendations_empty));
                progress.setVisibility(View.GONE);
            }
        });
        recVm.getError().observe(this, err -> {
            if (err != null && !err.isEmpty()) {
                progress.setVisibility(View.GONE);
                tvRecBody.setText(getString(R.string.api_recommendations_error));
                btnRetry.setVisibility(lastRecord != null ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Recargar los datos cuando la actividad vuelve a primer plano
        MainViewModel mainVm = new ViewModelProvider(this).get(MainViewModel.class);
        mainVm.refreshData();

        // Forzar actualización de recomendaciones si existe una medición reciente
        if (lastRecord != null) {
            RecommendationViewModel recVm = new ViewModelProvider(this).get(RecommendationViewModel.class);
            recVm.fetchForRecord(lastRecord);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Forzando la invalidación de caché usando un método alternativo
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_recommendations, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_share) {
            shareRecommendation();
            return true;
        } else if (id == R.id.action_save) {
            saveRecommendationAsPdf();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupButtonListeners() {
        btnMoreTips.setOnClickListener(v -> showMoreTipsDialog());

        // La funcionalidad de llamada de emergencia se maneja en el layout directamente o a través de un botón diferente
        MaterialButton btnEmergencyAction = findViewById(R.id.btnEmergencyCall);
        if (btnEmergencyAction != null) {
            btnEmergencyAction.setOnClickListener(v -> callEmergencyNumber());
        }
    }

    private void showMoreTipsDialog() {
        String[] tips = {
            "Reduzca la ingesta de sodio a menos de 2.300 mg por día",
            "Realice al menos 150 minutos de actividad física moderada por semana",
            "Limite el consumo de alcohol a no más de 1-2 bebidas por día",
            "Mantenga un peso saludable, el sobrepeso aumenta la presión",
            "Limite la cafeína a 2-3 tazas de café por día",
            "Reduzca el estrés con técnicas de relajación como meditación",
            "Asegúrese de dormir entre 7-8 horas cada noche",
            "Consuma más frutas, verduras y granos enteros",
            "Limite los alimentos procesados y comida rápida"
        };

        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.quick_tips_title)
            .setItems(tips, (dialog, which) -> {
                // Opcional: hacer algo al seleccionar un tip
                Snackbar.make(btnMoreTips, tips[which], Snackbar.LENGTH_LONG).show();
            })
            .setPositiveButton(R.string.btn_close, null)
            .show();
    }

    private void callEmergencyNumber() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.emergency_title)
            .setMessage(R.string.emergency_call_confirmation)
            .setPositiveButton(R.string.btn_call, (dialog, which) -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:911"));
                startActivity(intent);
            })
            .setNegativeButton(R.string.btn_cancel, null)
            .show();
    }

    private void shareRecommendation() {
        if (currentRecommendation == null || lastRecord == null) {
            Toast.makeText(this, R.string.error_nothing_to_share, Toast.LENGTH_SHORT).show();
            return;
        }

        String shareText = String.format(getString(R.string.share_template),
                lastRecord.getSystolic() + "/" + lastRecord.getDiastolic(),
                lastRecord.getHeartRate(),
                currentRecommendation);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)));
    }

    private void saveRecommendationAsPdf() {
        // Esta función necesitaría una implementación con una biblioteca PDF
        Toast.makeText(this, R.string.feature_coming_soon, Toast.LENGTH_SHORT).show();
    }

    private void styleBadge(MaterialCardView card, TextView tvBadge, ImageView ivDot, String classification) {
        int bgColor = ContextCompat.getColor(this, R.color.bp_green);
        int dotColor = bgColor;
        String badgeText = getString(R.string.category_normal);
        String c = classification == null ? "" : classification.toLowerCase();
        if (c.contains("hipertensión") || c.contains("hipertension")) {
            bgColor = ContextCompat.getColor(this, R.color.bp_red);
            dotColor = bgColor;
            badgeText = getString(R.string.category_hipertension1).split(" ")[0]; // "Hipertensión"
        } else if (c.contains("elevada")) {
            bgColor = ContextCompat.getColor(this, R.color.bp_yellow);
            dotColor = bgColor;
            badgeText = getString(R.string.category_elevada);
        }
        // fondo claro de card segun estado
        card.setStrokeColor(bgColor);
        int overlay = android.graphics.Color.argb(28, android.graphics.Color.red(bgColor), android.graphics.Color.green(bgColor), android.graphics.Color.blue(bgColor));
        card.setCardBackgroundColor(overlay);
        tvBadge.setText(badgeText);
        tvBadge.setTextColor(bgColor);
        ivDot.setColorFilter(dotColor);
    }

    private void updateEmergencyVisibility(String classification) {
        String c = classification == null ? "" : classification.toLowerCase();
        if (c.contains("hipertensión") || c.contains("hipertension")) {
            cardEmergency.setVisibility(View.VISIBLE);
        } else {
            cardEmergency.setVisibility(View.GONE);
        }
    }
}
