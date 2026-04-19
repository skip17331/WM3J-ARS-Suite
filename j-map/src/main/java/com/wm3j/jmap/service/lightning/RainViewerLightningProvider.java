package com.wm3j.jmap.service.lightning;

import com.wm3j.jmap.service.AbstractDataProvider;
import com.wm3j.jmap.service.DataProviderException;
import com.wm3j.jmap.service.tiles.TileCompositor;
import javafx.scene.image.WritableImage;

import java.time.Instant;

/**
 * Fetches RainViewer lightning strike tiles and composites them into an
 * equirectangular overlay.
 *
 * Source: https://tilecache.rainviewer.com/v2/lightning/{z}/{x}/{y}.png
 *
 * No API key required. Tiles refresh every 10 minutes at RainViewer.
 * Uses zoom level 2 (16 tiles) for world-wide coverage at decent resolution.
 */
public class RainViewerLightningProvider extends AbstractDataProvider<LightningData>
        implements LightningProvider {

    private static final String URL_TEMPLATE =
        "https://tilecache.rainviewer.com/v2/lightning/{z}/{x}/{y}.png";

    private static final int ZOOM = 2;

    @Override
    protected LightningData doFetch() throws DataProviderException {
        WritableImage img = TileCompositor.fetchEquirectangular(URL_TEMPLATE, ZOOM);
        if (img == null) {
            throw new DataProviderException("RainViewer lightning: no tiles loaded",
                DataProviderException.ErrorCode.NETWORK_ERROR);
        }
        log.debug("RainViewer lightning overlay loaded ({}×{})", (int)img.getWidth(), (int)img.getHeight());
        return new LightningData(img, Instant.now());
    }
}
