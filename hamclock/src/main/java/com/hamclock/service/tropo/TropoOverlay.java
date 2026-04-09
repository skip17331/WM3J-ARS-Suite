package com.hamclock.service.tropo;

/** Tropospheric ducting overlay image */
public class TropoOverlay {
    private byte[] imageData;
    private String sourceUrl;

    public TropoOverlay(byte[] imageData) { this.imageData = imageData; }
    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
}
