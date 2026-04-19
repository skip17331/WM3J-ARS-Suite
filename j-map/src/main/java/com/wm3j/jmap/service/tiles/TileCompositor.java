package com.wm3j.jmap.service.tiles;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Fetches XYZ (slippy-map / TMS) tiles, stitches them into a Web Mercator
 * composite, then reprojects to equirectangular so it maps cleanly onto
 * the j-map canvas.
 *
 * All work happens on the calling thread's ForkJoinPool — safe to call
 * from a provider's doFetch() on a background scheduler thread.
 */
public class TileCompositor {

    private static final Logger log = LoggerFactory.getLogger(TileCompositor.class);

    private static final int TILE_PX = 256;

    /** Output equirectangular image dimensions. */
    public static final int OUT_W = 720;
    public static final int OUT_H = 360;

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    /**
     * Fetch all tiles at {@code zoom}, composite in Mercator space, reproject
     * to equirectangular.
     *
     * @param urlTemplate URL containing {z}, {x}, {y} placeholders
     * @param zoom        0 = 1 tile, 1 = 4 tiles, 2 = 16 tiles, 3 = 64 tiles
     * @return equirectangular {@link WritableImage}, or null if no tiles loaded
     */
    public static WritableImage fetchEquirectangular(String urlTemplate, int zoom) {
        int numTiles = 1 << zoom;          // 2^zoom per axis
        int compW    = numTiles * TILE_PX;
        int compH    = numTiles * TILE_PX;

        // ── 1. Fetch all tiles in parallel ─────────────────────────────────
        List<CompletableFuture<TileResult>> futures = new ArrayList<>(numTiles * numTiles);
        for (int tx = 0; tx < numTiles; tx++) {
            for (int ty = 0; ty < numTiles; ty++) {
                final int x = tx, y = ty;
                futures.add(CompletableFuture.supplyAsync(() -> fetchTile(urlTemplate, zoom, x, y)));
            }
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // ── 2. Stitch tiles into a Mercator pixel array ─────────────────────
        int[] comp = new int[compW * compH];
        int tilesOk = 0;

        for (CompletableFuture<TileResult> f : futures) {
            try {
                TileResult r = f.getNow(null);
                if (r == null || r.bytes() == null) continue;

                Image tile = new Image(new ByteArrayInputStream(r.bytes()));
                if (tile.isError()) continue;

                var pr = tile.getPixelReader();
                if (pr == null) continue;

                int tw = Math.min((int) tile.getWidth(),  TILE_PX);
                int th = Math.min((int) tile.getHeight(), TILE_PX);
                int[] tpx = new int[tw * th];
                pr.getPixels(0, 0, tw, th, PixelFormat.getIntArgbInstance(), tpx, 0, tw);

                int dstX = r.tileX() * TILE_PX;
                int dstY = r.tileY() * TILE_PX;
                for (int row = 0; row < th; row++) {
                    System.arraycopy(tpx, row * tw, comp, (dstY + row) * compW + dstX, tw);
                }
                tilesOk++;
            } catch (Exception e) {
                log.debug("Tile stitch error: {}", e.getMessage());
            }
        }

        if (tilesOk == 0) {
            log.warn("No tiles loaded — urlTemplate={}", urlTemplate);
            return null;
        }
        log.debug("Loaded {}/{} tiles (z={})", tilesOk, numTiles * numTiles, zoom);

        // ── 3. Reproject Mercator → equirectangular ─────────────────────────
        //
        // For each equirectangular pixel (ex, ey):
        //   lat  = 90 - ey/OUT_H * 180
        //   mercY = 0.5 - ln(tan(π/4 + lat/2)) / (2π)   [0..1 in Mercator space]
        //
        // Mercator is only defined for |lat| < 85.05°; poles stay transparent.
        int[] out = new int[OUT_W * OUT_H];

        for (int ey = 0; ey < OUT_H; ey++) {
            double lat = 90.0 - (double) ey / OUT_H * 180.0;
            if (Math.abs(lat) >= 85.051_129) continue;

            double latRad  = Math.toRadians(lat);
            double mercYFrac = 0.5 - Math.log(Math.tan(Math.PI / 4 + latRad / 2)) / (2 * Math.PI);
            int srcY = (int) (mercYFrac * compH);
            if (srcY < 0 || srcY >= compH) continue;

            int srcRow = srcY * compW;
            int dstRow = ey  * OUT_W;

            for (int ex = 0; ex < OUT_W; ex++) {
                int srcX = (int) ((double) ex / OUT_W * compW);
                out[dstRow + ex] = comp[srcRow + srcX];
            }
        }

        WritableImage result = new WritableImage(OUT_W, OUT_H);
        result.getPixelWriter().setPixels(0, 0, OUT_W, OUT_H,
            PixelFormat.getIntArgbInstance(), out, 0, OUT_W);
        return result;
    }

    // ── HTTP tile fetch ───────────────────────────────────────────────────────

    private static TileResult fetchTile(String urlTemplate, int z, int x, int y) {
        String url = urlTemplate
            .replace("{z}", Integer.toString(z))
            .replace("{x}", Integer.toString(x))
            .replace("{y}", Integer.toString(y));
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "J-Map/1.0 (ham radio ARS Suite)")
                .GET()
                .build();
            HttpResponse<byte[]> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() == 200 && resp.body().length > 0) {
                return new TileResult(x, y, resp.body());
            }
            log.debug("Tile HTTP {}: {}", resp.statusCode(), url);
        } catch (Exception e) {
            log.debug("Tile fetch {}: {}", url, e.getMessage());
        }
        return new TileResult(x, y, null);
    }

    private record TileResult(int tileX, int tileY, byte[] bytes) {}
}
