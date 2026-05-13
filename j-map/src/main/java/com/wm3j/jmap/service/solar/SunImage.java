package com.wm3j.jmap.service.solar;

import javafx.scene.image.Image;

import java.time.Instant;

/**
 * Result type for {@link SunImageProvider} — wraps the latest sun
 * image with its observation timestamp and the credit line (so the
 * UI can render the required attribution without hard-coding it).
 */
public final class SunImage {

    public final Image   image;
    public final Instant fetchedAt;
    public final String  credit;
    public final String  wavelength;  // e.g. "HMIIC" (visible-light continuum)

    public SunImage(Image image, Instant fetchedAt, String credit, String wavelength) {
        this.image      = image;
        this.fetchedAt  = fetchedAt;
        this.credit     = credit;
        this.wavelength = wavelength;
    }
}
