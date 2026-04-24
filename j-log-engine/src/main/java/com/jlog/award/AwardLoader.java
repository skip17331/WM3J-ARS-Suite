package com.jlog.award;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

/**
 * Loads award specifications. Mirrors {@link com.jlog.plugin.PluginLoader}:
 * bundled JSONs from the engine JAR first, then any user-installed files in
 * {@code ~/.j-log/awards/}. Users can drop a JSON in that folder to add a
 * new award without rebuilding.
 */
public class AwardLoader {

    private static final Logger log = LoggerFactory.getLogger(AwardLoader.class);
    private static final AwardLoader INSTANCE = new AwardLoader();
    public  static AwardLoader getInstance() { return INSTANCE; }
    private AwardLoader() {}

    private final ObjectMapper       mapper  = new ObjectMapper();
    private final List<AwardPlugin>  awards  = new ArrayList<>();

    public void init() {
        awards.clear();
        loadBundled();
        loadUserDir();
        log.info("Loaded {} awards", awards.size());
    }

    private void loadBundled() {
        String[] bundled = {
            "/com/jlog/awards/was.json",
            "/com/jlog/awards/wac.json",
            "/com/jlog/awards/dxcc.json",
            "/com/jlog/awards/wpx.json",
            "/com/jlog/awards/colonies_13.json"
        };
        for (String resource : bundled) {
            try (InputStream is = getClass().getResourceAsStream(resource)) {
                if (is == null) {
                    log.warn("Bundled award not found: {}", resource);
                    continue;
                }
                AwardPlugin a = mapper.readValue(is, AwardPlugin.class);
                if (!isDuplicate(a.getAwardId())) {
                    awards.add(a);
                    log.debug("Loaded bundled award: {}", a.getAwardId());
                }
            } catch (Exception ex) {
                log.warn("Could not load bundled award {}: {}", resource, ex.getMessage());
            }
        }
    }

    private void loadUserDir() {
        Path dir = DatabaseManager.getInstance().getDataDir().resolve("awards");
        if (!dir.toFile().exists()) { dir.toFile().mkdirs(); return; }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.json")) {
            for (Path p : ds) loadFromPath(p);
        } catch (Exception ex) {
            log.warn("Error scanning award dir {}: {}", dir, ex.getMessage());
        }
    }

    public void importAward(Path source) throws Exception {
        AwardPlugin a = mapper.readValue(source.toFile(), AwardPlugin.class);
        if (a.getAwardId() == null || a.getAwardId().isBlank())
            throw new IllegalArgumentException("Award has no awardId");
        Path dest = DatabaseManager.getInstance().getDataDir()
            .resolve("awards").resolve(source.getFileName());
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        loadFromPath(dest);
        log.info("Imported award {} from {}", a.getAwardId(), source);
    }

    private void loadFromPath(Path path) {
        try {
            AwardPlugin a = mapper.readValue(path.toFile(), AwardPlugin.class);
            awards.removeIf(ex -> ex.getAwardId().equals(a.getAwardId()));
            awards.add(a);
        } catch (Exception ex) {
            log.warn("Failed to load award {}: {}", path, ex.getMessage());
        }
    }

    private boolean isDuplicate(String id) {
        return awards.stream().anyMatch(a -> a.getAwardId().equals(id));
    }

    public List<AwardPlugin> getAvailableAwards() {
        if (awards.isEmpty()) init();
        return List.copyOf(awards);
    }

    public AwardPlugin getById(String id) {
        return awards.stream().filter(a -> a.getAwardId().equals(id))
            .findFirst().orElse(null);
    }

    public void reload() {
        awards.clear();
        loadBundled();
        loadUserDir();
    }
}
