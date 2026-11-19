package com.ars.fx.web;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhoneQrTest {
    @Test void theCodeReadsBackAsTheAddress() throws Exception {
        String url = "http://10.0.0.95:8095/?t=K7M2QX9A";
        BitMatrix m = PhoneQr.matrix(url);
        assertNotNull(m);
        int scale = 4, w = m.getWidth() * scale, h = m.getHeight() * scale;
        int[] px = new int[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) px[y * w + x] = m.get(x / scale, y / scale) ? 0xFF000000 : 0xFFFFFFFF;
        String back = new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(w, h, px)))).getText();
        assertEquals(url, back);
    }

    @Test void newTokensAreShortAndUseTheUnambiguousAlphabet() {
        for (int i = 0; i < 200; i++) {
            String t = WebClientServer.newToken();
            assertEquals(WebClientServer.TOKEN_LENGTH, t.length());
            for (char c : t.toCharArray()) assertTrue(WebClientServer.TOKEN_ALPHABET.indexOf(c) >= 0, t);
        }
        for (char bad : "01OIL".toCharArray()) assertEquals(-1, WebClientServer.TOKEN_ALPHABET.indexOf(bad));
    }
}
