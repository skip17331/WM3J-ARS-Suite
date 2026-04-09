package com.hamclock.service.aurora;

/**
 * Aurora overlay data - typically a PNG image or intensity grid.
 */
public class AuroraOverlay {
    private byte[] imageData;      // PNG image bytes
    private double[][] intensity;  // [lon][lat] intensity grid 0.0-1.0
    private String sourceUrl;

    public AuroraOverlay(byte[] imageData) { this.imageData = imageData; }
    public AuroraOverlay(double[][] intensity) { this.intensity = intensity; }

    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
    public double[][] getIntensity() { return intensity; }
    public void setIntensity(double[][] intensity) { this.intensity = intensity; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
}
