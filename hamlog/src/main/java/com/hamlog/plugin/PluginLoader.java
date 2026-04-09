package com.hamlog.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamlog.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Plugin loader.
 *
 * Plugin discovery order:
 *   1. Bundled plugins in resources/com/hamlog/plugins/
 *   2. User-installed plugins in ~/.hamlog/plugins/
 *
 * Plugins are cached in memory. Call reload() to refresh.
 */
public class PluginLoader {

    private static final Logger log = LoggerFactory.getLogger(PluginLoader.class);
    private static final PluginLoader INSTANCE = new PluginLoader();
    public  static PluginLoader getInstance() { return INSTANCE; }
    private PluginLoader() {}

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<ContestPlugin> plugins = new ArrayList<>();

    // ---------------------------------------------------------------
    // Initialise — call once at startup
    // ---------------------------------------------------------------

    public void init() {
        plugins.clear();
        loadBundled();
        loadUserDir();
        log.info("Loaded {} contest plugins", plugins.size());
    }

    // ---------------------------------------------------------------
    // Load bundled plugins from jar resources
    // ---------------------------------------------------------------

    private void loadBundled() {
        String[] bundled = {
            "/com/hamlog/plugins/arrl_sweepstakes_cw.json",
            "/com/hamlog/plugins/arrl_sweepstakes_ssb.json",
            "/com/hamlog/plugins/cq_ww_cw.json",
            "/com/hamlog/plugins/cq_ww_ssb.json"
        };
        for (String resource : bundled) {
            try (InputStream is = getClass().getResourceAsStream(resource)) {
                if (is != null) {
                    ContestPlugin p = mapper.readValue(is, ContestPlugin.class);
                    if (!isDuplicate(p.getContestId())) {
                        plugins.add(p);
                        log.debug("Loaded bundled plugin: {}", p.getContestId());
                    }
                }
            } catch (Exception ex) {
                log.warn("Could not load bundled plugin {}: {}", resource, ex.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------
    // Load user-installed plugins from ~/.hamlog/plugins/
    // ---------------------------------------------------------------

    private void loadUserDir() {
        Path dir = DatabaseManager.getInstance().getDataDir().resolve("plugins");
        if (!dir.toFile().exists()) {
            dir.toFile().mkdirs();
            return;
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.json")) {
            for (Path p : ds) {
                loadFromPath(p);
            }
        } catch (Exception ex) {
            log.warn("Error scanning plugin dir: {}", dir, ex);
        }
    }

    // ---------------------------------------------------------------
    // Import a plugin from a user-chosen file
    // ---------------------------------------------------------------

    public void importPlugin(Path source) throws Exception {
        ContestPlugin p = mapper.readValue(source.toFile(), ContestPlugin.class);
        if (p.getContestId() == null || p.getContestId().isBlank())
            throw new IllegalArgumentException("Plugin has no contestId");

        Path dest = DatabaseManager.getInstance().getDataDir()
            .resolve("plugins")
            .resolve(source.getFileName());
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        loadFromPath(dest);
        log.info("Imported plugin {} from {}", p.getContestId(), source);
    }

    private void loadFromPath(Path path) {
        try {
            ContestPlugin p = mapper.readValue(path.toFile(), ContestPlugin.class);
            // Replace existing by id
            plugins.removeIf(ex -> ex.getContestId().equals(p.getContestId()));
            plugins.add(p);
            log.debug("Loaded plugin from {}: {}", path.getFileName(), p.getContestId());
        } catch (Exception ex) {
            log.warn("Failed to load plugin {}: {}", path, ex.getMessage());
        }
    }

    private boolean isDuplicate(String contestId) {
        return plugins.stream().anyMatch(p -> p.getContestId().equals(contestId));
    }

    public void reload() {
        plugins.clear();
        loadBundled();
        loadUserDir();
    }

    // ---------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------

    /**
     * Returns all loaded plugins.
     * If init() was never called (e.g. during testing), it is called now
     * so callers always receive a populated list.
     */
    public List<ContestPlugin> getAvailablePlugins() {
        if (plugins.isEmpty()) {
            init();
        }
        return List.copyOf(plugins);
    }

    public ContestPlugin getById(String id) {
        return plugins.stream()
            .filter(p -> p.getContestId().equals(id))
            .findFirst().orElse(null);
    }
}
