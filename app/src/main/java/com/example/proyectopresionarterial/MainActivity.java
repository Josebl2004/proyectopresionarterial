package com.example.proyectopresionarterial;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private MainViewModel mainViewModel;
    private RecommendationViewModel recViewModel;
    private MaterialCardView cardToday;
    private TextView tvApiBody;
    private TextView tvEmpty;
    private MaterialButton fabAdd;
    private MaterialButton btnEditProfile;
    private BottomNavigationView bottomNavigation;
    // Guardar última medición mostrada
    private BloodPressureRecord lastRecordDisplayed;

    // Nuevas vistas para funcionalidades mejoradas
    private TextView tvMeasurementsCount;
    private TextView tvAvgSystolic;
    private TextView tvAvgDiastolic;
    private TextView tvAvgHeartRate;
    private MaterialCardView cardQuickSummary;
    private MaterialCardView cardTrendChart;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verificar sesión
        if (!SessionManager.isLoggedIn(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // Configurar Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Inicializar vistas y configurar
        initializeViews();
        setupClickListeners();
        setupViewModels();
        setupBottomNavigation();
    }

    private void initializeViews() {
        // Referencias a vistas críticas
        tvEmpty = findViewById(R.id.tvEmptyToday);
        cardToday = findViewById(R.id.cardToday);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        fabAdd = findViewById(R.id.fabAdd);
        tvApiBody = findViewById(R.id.tvApiBody);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Nuevas vistas para resumen
        tvMeasurementsCount = findViewById(R.id.tvMeasurementsCount);
        tvAvgSystolic = findViewById(R.id.tvAvgSystolic);
        tvAvgDiastolic = findViewById(R.id.tvAvgDiastolic);
        tvAvgHeartRate = findViewById(R.id.tvAvgHeartRate);
        cardQuickSummary = findViewById(R.id.cardQuickSummary);
        cardTrendChart = findViewById(R.id.cardTrendChart);

        TextView tvWelcome = findViewById(R.id.tvWelcome);
        TextView tvTodayDate = findViewById(R.id.tvTodayDate);
        ImageButton btnLogout = findViewById(R.id.btnLogout);

        // Configurar bienvenida
        if (tvWelcome != null) {
            String userName = SessionManager.getUserName(this);
            tvWelcome.setText(getString(R.string.welcome_text, capitalizeWords(userName)));
        }

        // Configurar fecha
        if (tvTodayDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d 'de' MMMM", new Locale("es"));
            String fecha = sdf.format(new Date());
            tvTodayDate.setText(fecha.substring(0, 1).toUpperCase() + fecha.substring(1));
        }

        // Configurar logout
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutDialog());
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigation == null) return;

        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_records) {
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            } else if (itemId == R.id.nav_analysis) {
                startActivity(new Intent(this, StatsActivity.class));
                return true;
            } else if (itemId == R.id.nav_settings) {
                showSettingsDialog();
                return true;
            }
            return false;
        });
    }

    private void setupClickListeners() {
        // Botón nueva medición
        if (fabAdd != null) {
            fabAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddRecordActivity.class))
            );
        }

        // Tarjeta de historial
        MaterialCardView cardHistory = findViewById(R.id.cardHistory);
        if (cardHistory != null) {
            cardHistory.setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class))
            );
        }

        // Tarjeta de estadísticas
        MaterialCardView cardStats = findViewById(R.id.cardStats);
        if (cardStats != null) {
            cardStats.setOnClickListener(v ->
                startActivity(new Intent(this, StatsActivity.class))
            );
        }

        // Botón editar perfil
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class))
            );
        }

        // Botón más consejos
        MaterialButton btnMoreTips = findViewById(R.id.btnMoreTips);
        if (btnMoreTips != null) {
            btnMoreTips.setOnClickListener(v ->
                startActivity(new Intent(this, RecommendationsActivity.class))
            );
        }

        // Click en tarjeta de medición y sus componentes
        if (cardToday != null) {
            cardToday.setOnClickListener(v -> showRecordDetails());

            // Configuración de clics en componentes dentro de la tarjeta
            View includeLastMeasure = findViewById(R.id.includeLastMeasure);
            if (includeLastMeasure != null) {
                TextView tvStatusToday = includeLastMeasure.findViewById(R.id.tvStatusToday);
                TextView tvValuesToday = includeLastMeasure.findViewById(R.id.tvValuesToday);
                TextView tvMetaToday = includeLastMeasure.findViewById(R.id.tvMetaToday);
                MaterialButton btnStatusChip = includeLastMeasure.findViewById(R.id.btnStatusChip);

                // Hacer que cada elemento sea clickeable y muestre detalles
                if (tvStatusToday != null) {
                    tvStatusToday.setOnClickListener(v -> showRecordDetails());
                }

                if (tvValuesToday != null) {
                    tvValuesToday.setOnClickListener(v -> showRecordDetails());
                }

                if (tvMetaToday != null) {
                    tvMetaToday.setOnClickListener(v -> showRecordDetails());
                }

                // El chip de estado muestra información específica sobre la clasificación
                if (btnStatusChip != null) {
                    btnStatusChip.setOnClickListener(v -> showClassificationInfo());
                }

                // Configurar el botón "Ver más en Tendencia últimos 7 días" para navegar a estadísticas
                MaterialButton btnViewMore = includeLastMeasure.findViewById(R.id.btnViewMore);
                if (btnViewMore != null) {
                    btnViewMore.setOnClickListener(v ->
                        startActivity(new Intent(this, StatsActivity.class))
                    );
                }
            }
        }

        // Nuevas acciones rápidas
        MaterialCardView cardExport = findViewById(R.id.cardExport);
        if (cardExport != null) {
            cardExport.setOnClickListener(v -> showExportDialog());
        }

        MaterialCardView cardCompare = findViewById(R.id.cardCompare);
        if (cardCompare != null) {
            cardCompare.setOnClickListener(v -> showCompareDialog());
        }

        MaterialCardView cardReminder = findViewById(R.id.cardReminder);
        if (cardReminder != null) {
            cardReminder.setOnClickListener(v -> showReminderDialog());
        }

        MaterialCardView cardGoals = findViewById(R.id.cardGoals);
        if (cardGoals != null) {
            cardGoals.setOnClickListener(v -> showGoalsDialog());
        }
    }

    private void setupViewModels() {
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        recViewModel = new ViewModelProvider(this).get(RecommendationViewModel.class);

        // Banner de salud
        ImageView ivHealthBanner = findViewById(R.id.ivHealthBanner);
        if (ivHealthBanner != null) {
            mainViewModel.getHealthImageUrl().observe(this, url -> {
                if (url != null && !url.isEmpty()) {
                    Glide.with(this)
                        .load(url)
                        .centerCrop()
                        .placeholder(R.drawable.ic_health_illustration)
                        .error(R.drawable.ic_health_illustration)
                        .into(ivHealthBanner);
                } else {
                    ivHealthBanner.setImageResource(R.drawable.ic_health_illustration);
                }
            });
            mainViewModel.fetchHealthImage();
        }

        // Recomendaciones
        if (tvApiBody != null) {
            recViewModel.getRecommendation().observe(this, rec -> {
                if (rec != null && !rec.trim().isEmpty()) {
                    tvApiBody.setText(rec.trim());
                } else {
                    tvApiBody.setText(getString(R.string.api_recommendations_empty));
                }
            });

            recViewModel.getError().observe(this, err -> {
                if (err != null && !err.isEmpty()) {
                    tvApiBody.setText(getString(R.string.api_recommendations_error));
                }
            });
        }

        // Última medición preferida (hoy si existe; si no, última global)
        mainViewModel.getLatestPreferred().observe(this, record -> {
            if (tvEmpty == null || cardToday == null) return;

            if (record == null) {
                tvEmpty.setVisibility(View.VISIBLE);
                cardToday.setVisibility(View.GONE);
                lastRecordDisplayed = null;
                if (tvApiBody != null) {
                    tvApiBody.setText(getString(R.string.api_recommendations_empty));
                }
                return;
            }

            tvEmpty.setVisibility(View.GONE);
            cardToday.setVisibility(View.VISIBLE);
            lastRecordDisplayed = record;
            updateRecordCard(record);

            // Obtener recomendaciones
            if (tvApiBody != null) {
                tvApiBody.setText(getString(R.string.api_recommendations_loading));
            }
            recViewModel.fetchForRecord(record);
        });

        // Perfil del usuario
        TextView tvProfileHint = findViewById(R.id.tvProfileHint);
        TextView tvBmiAge = findViewById(R.id.tvBmiAge);

        if (tvProfileHint != null) {
            tvProfileHint.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class))
            );
        }

        UserProfileViewModel profileVm = new ViewModelProvider(this).get(UserProfileViewModel.class);
        profileVm.getProfile().observe(this, profile -> {
            if (tvProfileHint == null || tvBmiAge == null || btnEditProfile == null) return;

            if (HealthUtils.isProfileComplete(profile)) {
                tvProfileHint.setVisibility(View.GONE);
                btnEditProfile.setVisibility(View.VISIBLE);
                tvBmiAge.setVisibility(View.VISIBLE);

                float bmi = HealthUtils.calculateBmi(profile.weightLbs, profile.heightCm);
                int age = HealthUtils.calculateAge(profile.dateOfBirth);
                String bmiCategory = HealthUtils.getBmiCategory(bmi);
                tvBmiAge.setText(getString(R.string.bmi_age, bmi, bmiCategory, age));
            } else {
                tvProfileHint.setVisibility(View.VISIBLE);
                btnEditProfile.setVisibility(View.VISIBLE);
                tvBmiAge.setVisibility(View.GONE);
            }
        });

        // Estadísticas semanales mejoradas
        loadWeeklyStats();
    }

    private void loadWeeklyStats() {
        StatsViewModel statsVm = new ViewModelProvider(this).get(StatsViewModel.class);

        // Cambiar a una observación de LiveData más reciente que se actualice en tiempo real
        statsVm.getLast7Days().observe(this, stats -> {
            if (stats == null || stats.isEmpty()) {
                if (cardQuickSummary != null) cardQuickSummary.setVisibility(View.GONE);
                return;
            }

            // Mostrar resumen
            if (cardQuickSummary != null) cardQuickSummary.setVisibility(View.VISIBLE);

            // Calcular promedios
            int avgSys = 0, avgDia = 0, avgHr = 0;
            for (BloodPressureRecord rec : stats) {
                avgSys += rec.getSystolic();
                avgDia += rec.getDiastolic();
                avgHr += rec.getHeartRate();
            }
            int count = stats.size();
            avgSys /= count > 0 ? count : 1;  // Prevenir división por cero
            avgDia /= count > 0 ? count : 1;
            avgHr /= count > 0 ? count : 1;

            // Actualizar vistas
            if (tvMeasurementsCount != null) {
                tvMeasurementsCount.setText(getString(R.string.measurements_count, count));
            }
            if (tvAvgSystolic != null) {
                tvAvgSystolic.setText(getString(R.string.avg_systolic, avgSys));
            }
            if (tvAvgDiastolic != null) {
                tvAvgDiastolic.setText(getString(R.string.avg_diastolic, avgDia));
            }
            if (tvAvgHeartRate != null) {
                tvAvgHeartRate.setText(getString(R.string.avg_heart_rate, avgHr));
            }

            // Mostrar gráfico si hay suficientes datos
            updateTrendChart(stats);
        });
    }

    private void updateTrendChart(List<BloodPressureRecord> records) {
        TextView tvTrendPlaceholder = findViewById(R.id.tvTrendPlaceholder);
        com.github.mikephil.charting.charts.LineChart lineChart = findViewById(R.id.lineChartTrend);

        if (tvTrendPlaceholder == null || lineChart == null) return;

        if (records.size() >= 3) {
            // Ocultar el placeholder y mostrar el gráfico real
            tvTrendPlaceholder.setVisibility(View.GONE);
            lineChart.setVisibility(View.VISIBLE);

            // Configurar datos para el gráfico
            List<com.github.mikephil.charting.data.Entry> entriesSystolic = new java.util.ArrayList<>();
            List<com.github.mikephil.charting.data.Entry> entriesDiastolic = new java.util.ArrayList<>();
            List<com.github.mikephil.charting.data.Entry> entriesHeartRate = new java.util.ArrayList<>();

            // Ordenar registros por fecha (del más antiguo al más reciente) de forma segura
            records.sort((r1, r2) -> {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                try {
                    java.util.Date d1 = sdf.parse(r1.getDate());
                    java.util.Date d2 = sdf.parse(r2.getDate());
                    if (d1 == null && d2 == null) return 0;
                    if (d1 == null) return -1;
                    if (d2 == null) return 1;
                    return d1.compareTo(d2);
                } catch (Exception e) {
                    return 0;
                }
            });

            // Array para almacenar las fechas para el eje X
            final String[] dates = new String[records.size()];

            // Convertir registros a entradas para el gráfico (con parseo seguro de fechas)
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("dd/MM", java.util.Locale.US);

            for (int i = 0; i < records.size(); i++) {
                BloodPressureRecord record = records.get(i);
                entriesSystolic.add(new com.github.mikephil.charting.data.Entry(i, record.getSystolic()));
                entriesDiastolic.add(new com.github.mikephil.charting.data.Entry(i, record.getDiastolic()));
                entriesHeartRate.add(new com.github.mikephil.charting.data.Entry(i, record.getHeartRate()));

                // Guardar fecha para mostrar en eje X (formato corto)
                String rawDate = record.getDate();
                java.util.Date date = null;
                try {
                    date = inputFormat.parse(rawDate);
                } catch (Exception e) {
                    // ignore, fallback to rawDate
                }
                dates[i] = date != null ? outputFormat.format(date) : (rawDate != null ? rawDate : "");
            }

            // Crear conjuntos de datos
            com.github.mikephil.charting.data.LineDataSet setSystolic = new com.github.mikephil.charting.data.LineDataSet(entriesSystolic, "Sistólica");
            setSystematicStyle(setSystolic, ContextCompat.getColor(this, android.R.color.holo_red_light));

            com.github.mikephil.charting.data.LineDataSet setDiastolic = new com.github.mikephil.charting.data.LineDataSet(entriesDiastolic, "Diastólica");
            setSystematicStyle(setDiastolic, ContextCompat.getColor(this, android.R.color.holo_blue_light));

            com.github.mikephil.charting.data.LineDataSet setHeartRate = new com.github.mikephil.charting.data.LineDataSet(entriesHeartRate, "Pulso");
            setSystematicStyle(setHeartRate, ContextCompat.getColor(this, android.R.color.holo_green_light));
            setHeartRate.setDrawFilled(false); // El pulso sin relleno para mejor visualización

            // Combinar los conjuntos de datos
            com.github.mikephil.charting.data.LineData lineData = new com.github.mikephil.charting.data.LineData(setSystolic, setDiastolic, setHeartRate);
            lineChart.setData(lineData);

            // Configuraciones adicionales del gráfico
            lineChart.getDescription().setEnabled(false);
            lineChart.getAxisRight().setEnabled(false);
            lineChart.setExtraOffsets(10, 10, 10, 10);

            // Personalizar el eje X para mostrar fechas
            com.github.mikephil.charting.components.XAxis xAxis = lineChart.getXAxis();
            xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    int index = (int) value;
                    if (index >= 0 && index < dates.length) {
                        return dates[index];
                    } else {
                        return "";
                    }
                }
            });

            // Personalizar tooltip para mostrar la fecha completa
            // Instanciar StatsMarkerView con el layout de marcador y pasar el array de fechas
            StatsMarkerView marker = new StatsMarkerView(this, R.layout.marker_view_stats, dates);
            lineChart.setMarker(marker);

            // Mejoras visuales adicionales
            lineChart.setDrawGridBackground(false);
            lineChart.getAxisLeft().setDrawGridLines(false);
            xAxis.setDrawGridLines(false);
            lineChart.getLegend().setEnabled(true);
            lineChart.getLegend().setForm(com.github.mikephil.charting.components.Legend.LegendForm.LINE);
            lineChart.getLegend().setTextSize(12f);
            lineChart.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP);
            lineChart.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);

            // Animación y actualización
            lineChart.animateX(1000);
            lineChart.invalidate(); // Refrescar el gráfico
        } else {
            // No hay suficientes datos para mostrar la gráfica
            tvTrendPlaceholder.setVisibility(View.VISIBLE);
            lineChart.setVisibility(View.GONE);
            tvTrendPlaceholder.setText("\uD83D\uDCCA Gráfico de tendencia\n(Se mostrará después de 3+ mediciones)");
        }
    }

    private void setSystematicStyle(com.github.mikephil.charting.data.LineDataSet dataSet, int color) {
        dataSet.setColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(color);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(color);
        dataSet.setFillAlpha(50);
        dataSet.setMode(com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER);
    }

    // Nuevos métodos para funcionalidades adicionales

    private void showExportDialog() {
        String[] formats = {
            getString(R.string.export_csv),
            getString(R.string.export_pdf),
            getString(R.string.export_json)
        };

        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.export_dialog_title)
            .setMessage(R.string.export_dialog_message)
            .setItems(formats, (dialog, which) -> {
                String format = which == 0 ? "CSV" : which == 1 ? "PDF" : "JSON";
                exportData(format);
            })
            .setNegativeButton(R.string.btn_cancel, null)
            .show();
    }

    private void exportData(String format) {
        if ("PDF".equalsIgnoreCase(format)) {
            exportPdfReport();
            return;
        }
        // Simulación de exportación para otros formatos
        Snackbar.make(fabAdd,
            getString(R.string.export_success) + " (" + format + ")",
            Snackbar.LENGTH_LONG).show();

        // TODO: Implementar exportación real a archivos para otros formatos
    }

    private void exportPdfReport() {
        try {
            Snackbar.make(fabAdd, "Generando PDF…", Snackbar.LENGTH_SHORT).show();
        } catch (Exception ignored) {}

        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Obtener últimas 3 mediciones
                BloodPressureDao dao = AppDatabase.getInstance(getApplication()).bloodPressureDao();
                java.util.List<BloodPressureRecord> last3 = dao.getLast3();
                if (last3 == null) last3 = new java.util.ArrayList<>();

                // Obtener recomendación detallada (opcional, con timeout)
                final String[] detailedRec = new String[1];
                if (!last3.isEmpty() && NetworkUtils.isNetworkAvailable(this)) {
                    final Object lock = new Object();
                    final boolean[] done = {false};
                    DetailedRecommendationFetcher.fetch(getApplication(), last3, new DetailedRecommendationFetcher.CallbackRec() {
                        @Override
                        public void onSuccess(String recommendation) {
                            detailedRec[0] = recommendation;
                            synchronized (lock) { done[0] = true; lock.notify(); }
                        }
                        @Override
                        public void onError(String message) {
                            synchronized (lock) { done[0] = true; lock.notify(); }
                        }
                    });
                    synchronized (lock) {
                        try { lock.wait(8000); } catch (InterruptedException ignored) {}
                    }
                }

                // Generar PDF
                java.io.File pdf = PdfReportGenerator.generate(this,
                        last3.isEmpty() ? java.util.Collections.emptyList() : last3,
                        detailedRec[0]);

                // Compartir PDF
                android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                        this, getPackageName() + ".fileprovider", pdf);
                runOnUiThread(() -> {
                    android.content.Intent share = new android.content.Intent(android.content.Intent.ACTION_SEND);
                    share.setType("application/pdf");
                    share.putExtra(android.content.Intent.EXTRA_STREAM, uri);
                    share.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(android.content.Intent.createChooser(share, getString(R.string.share_via)));
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                    Snackbar.make(fabAdd, "Error al generar PDF: " + e.getMessage(), Snackbar.LENGTH_LONG).show()
                );
            }
        });
    }

    private void showCompareDialog() {
        Snackbar.make(fabAdd,
            "Funcionalidad de comparación disponible próximamente",
            Snackbar.LENGTH_SHORT).show();
        // TODO: Implementar actividad de comparación
    }

    private void showReminderDialog() {
        // Mostrar diálogo simple para configurar recordatorios (vista inflada no necesaria aquí)
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.reminder_dialog_title)
            .setMessage(R.string.reminder_dialog_message)
            .setPositiveButton(R.string.btn_save, (dialog, which) -> {
                Snackbar.make(fabAdd, R.string.reminder_set, Snackbar.LENGTH_SHORT).show();
                // TODO: Implementar sistema de notificaciones
            })
            .setNegativeButton(R.string.btn_cancel, null)
            .show();
    }

    private void showGoalsDialog() {
        Snackbar.make(fabAdd,
            "Configuración de metas disponible próximamente",
            Snackbar.LENGTH_SHORT).show();
        // TODO: Implementar sistema de metas
    }

    private void showSettingsDialog() {
        String[] settings = {
            getString(R.string.settings_notifications),
            getString(R.string.settings_units),
            getString(R.string.settings_theme),
            getString(R.string.settings_backup)
        };

        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_title)
            .setItems(settings, (dialog, which) -> {
                Snackbar.make(fabAdd,
                    "Configuración " + settings[which] + " disponible próximamente",
                    Snackbar.LENGTH_SHORT).show();
            })
            .setNegativeButton(R.string.btn_close, null)
            .show();
    }

    private void updateRecordCard(BloodPressureRecord record) {
        // Obtener correctamente las referencias desde el layout incluido
        View includeLastMeasure = findViewById(R.id.includeLastMeasure);
        if (includeLastMeasure == null) return;

        TextView tvStatus = includeLastMeasure.findViewById(R.id.tvStatusToday);
        TextView tvValues = includeLastMeasure.findViewById(R.id.tvValuesToday);
        TextView tvMeta = includeLastMeasure.findViewById(R.id.tvMetaToday);
        MaterialButton btnStatusChip = includeLastMeasure.findViewById(R.id.btnStatusChip);

        if (tvStatus == null || tvValues == null || tvMeta == null) return;

        String classification = record.getClassification();
        if (classification == null || classification.isEmpty()) {
            classification = ClassificationHelper.classify(record.getSystolic(), record.getDiastolic());
        }

        tvStatus.setText(capitalize(classification));
        tvValues.setText(getString(R.string.values_today,
            record.getSystolic(), record.getDiastolic(), record.getHeartRate()));
        tvMeta.setText(getString(R.string.meta_today,
            record.getDate(), record.getTime(), record.getCondition()));

        // Colorear según clasificación con alto contraste en el chip
        if (btnStatusChip != null) {
            int tint;
            int textColor;
            String chipText;
            String c = classification.toLowerCase(Locale.getDefault());

            if (c.contains("hipertensión") || c.contains("hipertension")) {
                tint = ContextCompat.getColor(this, R.color.bp_red);
                chipText = getString(R.string.chip_hipertension);
                textColor = android.graphics.Color.WHITE;
            } else if (c.contains("elevada")) {
                tint = ContextCompat.getColor(this, R.color.bp_yellow);
                chipText = getString(R.string.chip_elevada);
                // sobre amarillo, usar texto oscuro para contraste
                textColor = ContextCompat.getColor(this, R.color.onSurface);
            } else {
                tint = ContextCompat.getColor(this, R.color.bp_green);
                chipText = getString(R.string.chip_normal);
                textColor = android.graphics.Color.WHITE;
            }

            btnStatusChip.setText(chipText);
            btnStatusChip.setBackgroundTintList(ColorStateList.valueOf(tint));
            btnStatusChip.setStrokeColor(ColorStateList.valueOf(tint));
            btnStatusChip.setTextColor(textColor);
        }

        // Asegurarnos que la tarjeta sea visible
        cardToday.setVisibility(View.VISIBLE);
        applyCardColor(cardToday, classification);
    }

    private void showRecordDetails() {
        // Usar la última medición ya mostrada para evitar crear observers en cada click
        if (lastRecordDisplayed == null) return;
        BloodPressureRecord record = lastRecordDisplayed;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_record_details, null);

        // Configurar datos
        TextView tvSystolic = view.findViewById(R.id.tvDetailsSystolic);
        TextView tvDiastolic = view.findViewById(R.id.tvDetailsDiastolic);
        TextView tvHeartRate = view.findViewById(R.id.tvDetailsHeartRate);
        TextView tvTime = view.findViewById(R.id.tvDetailsTime);
        TextView tvCondition = view.findViewById(R.id.tvDetailsCondition);
        TextView tvClassification = view.findViewById(R.id.tvDetailsClassification);

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

        // Botones
        MaterialButton btnShare = view.findViewById(R.id.btnShareRecord);
        MaterialButton btnDelete = view.findViewById(R.id.btnDeleteRecord);

        btnShare.setOnClickListener(v -> {
            shareRecord(record);
            dialog.dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            showDeleteDialog(record, dialog);
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void shareRecord(BloodPressureRecord record) {
        String shareText = String.format(getString(R.string.share_record_template),
            record.getSystolic(), record.getDiastolic(), record.getHeartRate(),
            record.getDate(), record.getTime(),
            record.getClassification() != null ? record.getClassification() :
                ClassificationHelper.classify(record.getSystolic(), record.getDiastolic()));

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
    }

    private void showDeleteDialog(BloodPressureRecord record, BottomSheetDialog bottomSheet) {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_record_title)
            .setMessage(R.string.delete_record_confirmation)
            .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                mainViewModel.deleteRecord(record);
                bottomSheet.dismiss();
                Snackbar.make(fabAdd, R.string.record_deleted, Snackbar.LENGTH_SHORT).show();
            })
            .setNegativeButton(R.string.btn_cancel, null)
            .show();
    }

    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.logout_title)
            .setMessage(R.string.logout_confirmation)
            .setPositiveButton(R.string.btn_logout, (dialog, which) -> logout())
            .setNegativeButton(R.string.btn_cancel, null)
            .show();
    }

    private void logout() {
        SessionManager.clearSession(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void applyCardColor(MaterialCardView card, String classification) {
        int color = ContextCompat.getColor(this, R.color.bp_green);
        String c = classification == null ? "" : classification.toLowerCase(Locale.getDefault());

        if (c.contains("hipertensión") || c.contains("hipertension")) {
            color = ContextCompat.getColor(this, R.color.bp_red);
        } else if (c.contains("elevada")) {
            color = ContextCompat.getColor(this, R.color.bp_yellow);
        }

        card.setStrokeColor(color);
        int overlay = android.graphics.Color.argb(28,
            android.graphics.Color.red(color),
            android.graphics.Color.green(color),
            android.graphics.Color.blue(color));
        card.setCardBackgroundColor(overlay);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase(Locale.getDefault()) + str.substring(1);
    }

    // Capitaliza cada palabra de una cadena: "juan perez" -> "Juan Perez"
    private String capitalizeWords(String str) {
        if (str == null) return null;
        String trimmed = str.trim();
        if (trimmed.isEmpty()) return trimmed;
        String[] parts = trimmed.split("\\s+");
        StringBuilder sb = new StringBuilder(trimmed.length());
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(p.substring(0, 1).toUpperCase(Locale.getDefault()));
            if (p.length() > 1) sb.append(p.substring(1).toLowerCase(Locale.getDefault()));
        }
        return sb.toString();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Recargar datos ligeros. Evita duplicar observers o recargar estadísticas innecesariamente.
        mainViewModel.refreshData();
    }

    /**
     * Muestra información detallada sobre la clasificación de presión arterial
     * cuando el usuario hace clic en el chip de estado
     */
    private void showClassificationInfo() {
        // Información por categoría
        String normalInfo = "• <120/80 mmHg\n• Mantenga un estilo de vida saludable\n• Controle su presión periódicamente";
        String elevadaInfo = "• 120-129/<80 mmHg\n• Modifique su estilo de vida\n• Reduzca el consumo de sal\n• Aumente la actividad física";
        String hipertensionInfo = "• ≥130/80 mmHg\n• Consulte con su médico\n• Puede necesitar medicación\n• Controle su presión regularmente";

        // Determinar qué categoría está actualmente
        MaterialButton btnStatusChip = findViewById(R.id.btnStatusChip);
        String currentCategory = btnStatusChip != null ? btnStatusChip.getText().toString() : "";
        String infoText;
        String title;

        if (currentCategory.equalsIgnoreCase(getString(R.string.chip_hipertension))) {
            infoText = hipertensionInfo;
            title = "Información sobre Hipertensión";
        } else if (currentCategory.equalsIgnoreCase(getString(R.string.chip_elevada))) {
            infoText = elevadaInfo;
            title = "Información sobre Presión Elevada";
        } else {
            infoText = normalInfo;
            title = "Información sobre Presión Normal";
        }

        // Mostrar el diálogo con la información
        new MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(infoText)
            .setPositiveButton("Entendido", null)
            .setNeutralButton("Más información", (dialog, which) -> {
                startActivity(new Intent(MainActivity.this, StatsActivity.class));
            })
            .show();
    }
}
