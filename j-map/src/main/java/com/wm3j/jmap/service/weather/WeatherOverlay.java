package com.wm3j.jmap.service.weather;

/** Weather overlay image data */
public class WeatherOverlay {
    private byte[] imageData;
    private String sourceUrl;

    public WeatherOverlay(byte[] imageData) { this.imageData = imageData; }
    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
}
