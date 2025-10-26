package com.example.proyectopresionarterial;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Genera un PDF con últimas 3 mediciones, gráfico simple y recomendaciones detalladas. */
public final class PdfReportGenerator {

    private PdfReportGenerator() {}

    public static File generate(Context ctx, List<BloodPressureRecord> records, String detailedRecommendation) throws Exception {
        if (records == null) records = new ArrayList<>();

        // Tamaño A4 (595x842 pt aprox)
        int pageWidth = 595;
        int pageHeight = 842;
        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = doc.startPage(pageInfo);
        Canvas c = page.getCanvas();

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(18);
        titlePaint.setFakeBoldText(true);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(12);

        Paint smallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        smallPaint.setColor(Color.DKGRAY);
        smallPaint.setTextSize(10);

        int margin = 24;
        int y = margin + 10;

        // Título
        c.drawText("Informe de Presión Arterial (últimas 3 mediciones)", margin, y, titlePaint);
        y += 16;
        String dateStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        c.drawText("Generado: " + dateStr, margin, y, smallPaint);
        y += 12;

        // Tabla de mediciones
        y += 8;
        c.drawText("Mediciones:", margin, y, textPaint);
        y += 14;
        int col1 = margin;
        int col2 = margin + 170;
        int col3 = margin + 310;
        c.drawText("Fecha/Hora", col1, y, smallPaint);
        c.drawText("Valores", col2, y, smallPaint);
        c.drawText("Condición/Clas.", col3, y, smallPaint);
        y += 12;

        int count = Math.min(3, records.size());
        for (int i = 0; i < count; i++) {
            BloodPressureRecord r = records.get(i);
            String cls = r.getClassification();
            if (cls == null || cls.isEmpty()) cls = ClassificationHelper.classify(r.getSystolic(), r.getDiastolic());
            c.drawText(r.getDate() + " " + r.getTime(), col1, y, textPaint);
            c.drawText(r.getSystolic() + "/" + r.getDiastolic() + " mmHg · " + r.getHeartRate() + " bpm", col2, y, textPaint);
            c.drawText((r.getCondition() == null ? "" : r.getCondition()) + " · " + cls, col3, y, textPaint);
            y += 16;
        }

        // Gráfico simple (sistólica/diastólica)
        y += 8;
        c.drawText("Gráfico (Sistólica/Diastólica)", margin, y, textPaint);
        y += 6;
        int chartTop = y + 8;
        int chartLeft = margin;
        int chartWidth = pageWidth - margin * 2;
        int chartHeight = 140;
        int chartBottom = chartTop + chartHeight;

        Paint axis = new Paint();
        axis.setColor(Color.GRAY);
        axis.setStrokeWidth(1);
        c.drawLine(chartLeft, chartTop, chartLeft, chartBottom, axis);
        c.drawLine(chartLeft, chartBottom, chartLeft + chartWidth, chartBottom, axis);

        // Escala simple: 50..200 mmHg
        int minVal = 50;
        int maxVal = 200;
        Paint sysPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sysPaint.setColor(Color.RED);
        sysPaint.setStrokeWidth(2);
        Paint diaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        diaPaint.setColor(Color.BLUE);
        diaPaint.setStrokeWidth(2);

        int points = count;
        if (points > 0) {
            float stepX = points == 1 ? chartWidth / 2f : (float) chartWidth / (points - 1);
            float prevX = 0, prevYsys = 0, prevYdia = 0;
            for (int i = 0; i < points; i++) {
                BloodPressureRecord r = records.get(i);
                float x = chartLeft + (points == 1 ? chartWidth / 2f : stepX * i);
                float ySys = chartBottom - (chartHeight * (clamp(r.getSystolic(), minVal, maxVal) - minVal)) / (maxVal - minVal);
                float yDia = chartBottom - (chartHeight * (clamp(r.getDiastolic(), minVal, maxVal) - minVal)) / (maxVal - minVal);
                // puntos
                c.drawCircle(x, ySys, 2.5f, sysPaint);
                c.drawCircle(x, yDia, 2.5f, diaPaint);
                // líneas
                if (i > 0) {
                    c.drawLine(prevX, prevYsys, x, ySys, sysPaint);
                    c.drawLine(prevX, prevYdia, x, yDia, diaPaint);
                }
                prevX = x; prevYsys = ySys; prevYdia = yDia;
            }
        }
        y = chartBottom + 18;

        // Recomendación detallada (texto largo con saltos de línea)
        c.drawText("Recomendaciones detalladas:", margin, y, textPaint);
        y += 14;
        String rec = detailedRecommendation == null ? "No disponible (sin conexión)" : detailedRecommendation.trim();
        y = drawMultiline(c, rec, margin, y, pageWidth - margin * 2, textPaint, 14);

        doc.finishPage(page);

        // Guardar en cache
        String fileName = "informe_pa_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf";
        File out = new File(ctx.getCacheDir(), fileName);
        try (FileOutputStream fos = new FileOutputStream(out)) {
            doc.writeTo(fos);
        } finally {
            doc.close();
        }
        return out;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static int drawMultiline(Canvas c, String text, int x, int y, int maxWidth, Paint p, int lineHeight) {
        if (text == null || text.isEmpty()) return y;
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String w : words) {
            String test = line.length() == 0 ? w : line + " " + w;
            float width = p.measureText(test);
            if (width > maxWidth) {
                c.drawText(line.toString(), x, y, p);
                y += lineHeight;
                line.setLength(0);
                line.append(w);
            } else {
                line.setLength(0);
                line.append(test);
            }
        }
        if (line.length() > 0) {
            c.drawText(line.toString(), x, y, p);
            y += lineHeight;
        }
        return y;
    }
}

