package com.hamradio.modem.ui;

import com.hamradio.modem.model.ModeType;
import com.hamradio.modem.model.SignalSnapshot;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class SpectrumPane extends Region {
    private final Canvas canvas = new Canvas(600, 220);

    private SignalSnapshot latest;
    private ModeType statusMode = ModeType.RTTY;
    private double peakFrequencyHz = 0.0;

    public SpectrumPane() {
        getChildren().add(canvas);
        setPrefSize(600, 220);
    }

    public void update(SignalSnapshot snapshot) {
        this.latest = snapshot;
        redraw();
    }

    public void setStatusMode(ModeType mode) {
        this.statusMode = mode != null ? mode : ModeType.RTTY;
        redraw();
    }

    public void setPeakFrequencyHz(double peakFrequencyHz) {
        this.peakFrequencyHz = peakFrequencyHz;
        redraw();
    }

    private void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.setFill(Color.rgb(10, 12, 18));
        gc.fillRect(0, 0, w, h);

        drawGrid(gc, w, h);

        if (latest != null && latest.getMagnitudes().length > 1) {
            drawSpectrum(gc, w, h, latest.getMagnitudes(), latest.getSampleRate());
            drawPeakMarker(gc, w, h, latest.getSampleRate());
            drawModeMarkers(gc, w, h, latest.getSampleRate());
        }

        drawLabels(gc, w, h);
    }

    private void drawGrid(GraphicsContext gc, double w, double h) {
        gc.setStroke(Color.rgb(55, 60, 70));
        gc.setLineWidth(1.0);

        for (int i = 0; i <= 10; i++) {
            double x = i * (w / 10.0);
            gc.strokeLine(x, 0, x, h);
        }

        for (int i = 0; i <= 6; i++) {
            double y = i * (h / 6.0);
            gc.strokeLine(0, y, w, y);
        }
    }

    private void drawSpectrum(GraphicsContext gc, double w, double h, double[] mags, double sampleRate) {
        double max = 1e-9;
        for (double m : mags) {
            if (m > max) max = m;
        }

        gc.setStroke(Color.LIMEGREEN);
        gc.setLineWidth(1.4);
        gc.beginPath();

        for (int i = 0; i < mags.length; i++) {
            double x = i * w / (mags.length - 1);
            double norm = mags[i] / max;
            double y = h - 8 - (norm * (h - 16));
            if (i == 0) gc.moveTo(x, y);
            else gc.lineTo(x, y);
        }

        gc.stroke();
    }

    private void drawPeakMarker(GraphicsContext gc, double w, double h, double sampleRate) {
        if (peakFrequencyHz <= 0.0 || sampleRate <= 0.0) {
            return;
        }

        double nyquist = sampleRate / 2.0;
        double x = (peakFrequencyHz / nyquist) * w;

        gc.setStroke(Color.ORANGE);
        gc.setLineWidth(1.5);
        gc.strokeLine(x, 0, x, h);

        gc.setFill(Color.ORANGE);
        gc.setFont(Font.font("Consolas", 12));
        gc.fillText(String.format("%.1f Hz", peakFrequencyHz), Math.min(w - 90, x + 4), 16);
    }

    private void drawModeMarkers(GraphicsContext gc, double w, double h, double sampleRate) {
        if (sampleRate <= 0.0) {
            return;
        }

        if (statusMode == ModeType.RTTY) {
            drawFrequencyMarker(gc, w, h, sampleRate, 2125.0, Color.CYAN, "Mark");
            drawFrequencyMarker(gc, w, h, sampleRate, 2295.0, Color.MAGENTA, "Space");
        } else if (statusMode == ModeType.AX25) {
            drawFrequencyMarker(gc, w, h, sampleRate, 1200.0, Color.CYAN, "1200");
            drawFrequencyMarker(gc, w, h, sampleRate, 2200.0, Color.MAGENTA, "2200");
        } else if (statusMode == ModeType.PSK31) {
            drawFrequencyMarker(gc, w, h, sampleRate, 1000.0, Color.CYAN, "PSK");
        } else if (statusMode == ModeType.CW) {
            drawFrequencyMarker(gc, w, h, sampleRate, 700.0, Color.YELLOW, "CW");
        } else if (statusMode == ModeType.OLIVIA) {
            // Olivia 8/500: 8 tones, 500 Hz bandwidth, 1500 Hz centre
            drawFrequencyMarker(gc, w, h, sampleRate, 1250.0, Color.rgb(100, 200, 255), "OLV-");
            drawFrequencyMarker(gc, w, h, sampleRate, 1500.0, Color.CYAN,               "OLV");
            drawFrequencyMarker(gc, w, h, sampleRate, 1750.0, Color.rgb(100, 200, 255), "OLV+");
        } else if (statusMode == ModeType.MFSK16) {
            // MFSK16: 16 tones, bin 89–104 at 8 kHz → ~1391–1625 Hz
            drawFrequencyMarker(gc, w, h, sampleRate, 1390.625, Color.rgb(180, 255, 140), "M16-");
            drawFrequencyMarker(gc, w, h, sampleRate, 1500.0,   Color.LIMEGREEN,          "M16");
            drawFrequencyMarker(gc, w, h, sampleRate, 1625.0,   Color.rgb(180, 255, 140), "M16+");
        } else if (statusMode == ModeType.DOMINOEX) {
            // DominoEX8 (default): 18 tones centred at 1500 Hz, ~1438–1570 Hz
            drawFrequencyMarker(gc, w, h, sampleRate, 1437.5, Color.rgb(255, 180, 100), "DX-");
            drawFrequencyMarker(gc, w, h, sampleRate, 1500.0, Color.ORANGE,             "DX");
            drawFrequencyMarker(gc, w, h, sampleRate, 1570.3, Color.rgb(255, 180, 100), "DX+");
        }
    }

    private void drawFrequencyMarker(GraphicsContext gc,
                                     double w,
                                     double h,
                                     double sampleRate,
                                     double freqHz,
                                     Color color,
                                     String label) {
        double nyquist = sampleRate / 2.0;
        double x = (freqHz / nyquist) * w;

        gc.setStroke(color);
        gc.setLineWidth(1.0);
        gc.strokeLine(x, 0, x, h);

        gc.setFill(color);
        gc.setFont(Font.font("Consolas", 11));
        gc.fillText(label, Math.min(w - 40, x + 3), h - 8);
    }

    private void drawLabels(GraphicsContext gc, double w, double h) {
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("Consolas", 11));
        gc.fillText("0 Hz", 6, h - 6);

        if (latest != null) {
            double nyquist = latest.getSampleRate() / 2.0;
            gc.fillText(String.format("%.0f Hz", nyquist), w - 60, h - 6);
        }
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        redraw();
    }
}
