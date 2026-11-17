package com.ars.fx.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** DevHook's own PNG writer (it needs no AWT at run time; the test uses ImageIO only to read the result back). */
class DevHookTest {
    @Test
    void pngRoundTripsEveryPixel() throws Exception {
        int w = 7, h = 5;
        int[] argb = new int[w * h];
        for (int i = 0; i < argb.length; i++) argb[i] = (0xFF << 24) | (i * 37 % 256) << 16 | (i * 91 % 256) << 8 | (i * 13 % 256);
        argb[3] = 0x80123456;                                   // one half-transparent pixel
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(DevHook.png(argb, w, h)));
        assertEquals(w, img.getWidth());
        assertEquals(h, img.getHeight());
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++)
            assertEquals(argb[y * w + x], img.getRGB(x, y), "pixel " + x + "," + y);
    }
}
