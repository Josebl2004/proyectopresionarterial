package com.example.proyectopresionarterial;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.util.Locale;

/**
 * Marcador personalizado para mostrar valores en los gráficos de estadísticas
 */
public class StatsMarkerView extends MarkerView {

    private final TextView tvContent;
    private String[] dates = null; // Nuevo campo para guardar las fechas

    public StatsMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvMarkerContent);
    }

    // Nuevo constructor que acepta un array de fechas
    public StatsMarkerView(Context context, int layoutResource, String[] dates) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvMarkerContent);
        this.dates = dates;
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        if (e != null) {
            StringBuilder sb = new StringBuilder();

            // Si tenemos fechas disponibles, mostrarlas
            if (dates != null && dates.length > 0) {
                int index = (int) e.getX();
                if (index >= 0 && index < dates.length) {
                    sb.append(dates[index]).append("\n");
                }
            }

            // Añadir el valor Y (presión arterial o pulso)
            sb.append(String.format(Locale.getDefault(), "%.0f", e.getY()));
            tvContent.setText(sb.toString());
        }
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }
}
