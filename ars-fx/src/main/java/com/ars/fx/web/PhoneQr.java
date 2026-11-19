package com.ars.fx.web;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.Map;

/** A QR code of the phone web client's address (with its token), for Setup: the phone's camera opens it and the page keeps the token. */
public final class PhoneQr {
    private PhoneQr() {}

    /** The code's modules (true = dark), with the quiet border of 2 modules the phone needs around it; null when the text cannot be encoded. */
    public static BitMatrix matrix(String text) {
        try {
            return new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 2, EncodeHintType.CHARACTER_SET, "UTF-8"));
        } catch (WriterException | IllegalArgumentException e) { return null; }
    }

    /** The code as a picture, each module {@code scale} pixels square, black on white whatever the colour scheme. Null when it cannot be made. */
    public static Image image(String text, int scale) {
        BitMatrix m = matrix(text);
        if (m == null) return null;
        WritableImage img = new WritableImage(m.getWidth() * scale, m.getHeight() * scale);
        PixelWriter w = img.getPixelWriter();
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++) w.setColor(x, y, m.get(x / scale, y / scale) ? Color.BLACK : Color.WHITE);
        return img;
    }
}
