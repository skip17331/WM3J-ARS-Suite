package com.jlog.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.db.DatabaseManager;
import com.jlog.util.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Plugin loader.
 *
 * Plugin discovery order:
 *   1. Bundled plugins in resources/com/jlog/plugins/
 *   2. User-installed plugins in ~/.j-log/plugins/
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
            "/com/jlog/plugins/arrl_sweepstakes_cw.json",
            "/com/jlog/plugins/arrl_sweepstakes_ssb.json",
            "/com/jlog/plugins/cq_ww_cw.json",
            "/com/jlog/plugins/cq_ww_ssb.json",
            "/com/jlog/plugins/arrl_dx_cw_us.json",
            "/com/jlog/plugins/arrl_dx_cw_dx.json",
            "/com/jlog/plugins/arrl_rtty_ru.json",
            "/com/jlog/plugins/arrl_10m.json",
            "/com/jlog/plugins/arrl_160m.json",
            "/com/jlog/plugins/arrl_intl_digital.json",
            "/com/jlog/plugins/arrl_vhf_june.json",
            "/com/jlog/plugins/arrl_vhf_jan.json",
            "/com/jlog/plugins/arrl_vhf_sep.json",
            "/com/jlog/plugins/arrl_dx_ssb_us.json",
            "/com/jlog/plugins/arrl_dx_ssb_dx.json",
            "/com/jlog/plugins/arrl_field_day.json",
            "/com/jlog/plugins/arrl_rookie_roundup_ssb.json",
            "/com/jlog/plugins/arrl_rookie_roundup_cw.json",
            "/com/jlog/plugins/arrl_rookie_roundup_rtty.json",
            "/com/jlog/plugins/cq_wpx_cw.json",
            "/com/jlog/plugins/cq_wpx_ssb.json",
            "/com/jlog/plugins/cq_wpx_rtty.json",
            "/com/jlog/plugins/cq_ww_rtty.json",
            "/com/jlog/plugins/cq_160m_cw.json",
            "/com/jlog/plugins/cq_160m_ssb.json",
            "/com/jlog/plugins/cq_ww_digital.json",
            "/com/jlog/plugins/oceania_dx_cw.json",
            "/com/jlog/plugins/oceania_dx_ssb.json",
            "/com/jlog/plugins/eu_hf_champ.json",
            "/com/jlog/plugins/all_asian_dx_cw.json",
            "/com/jlog/plugins/all_asian_dx_ssb.json",
            "/com/jlog/plugins/iaru_hf.json",
            "/com/jlog/plugins/sac_cw.json",
            "/com/jlog/plugins/sac_ssb.json",
            "/com/jlog/plugins/russian_dx.json",
            "/com/jlog/plugins/wag.json",
            "/com/jlog/plugins/ari_dx.json",
            "/com/jlog/plugins/ca_qso_party.json",
            "/com/jlog/plugins/tx_qso_party.json",
            "/com/jlog/plugins/fl_qso_party.json",
            "/com/jlog/plugins/mi_qso_party.json",
            "/com/jlog/plugins/oh_qso_party.json",
            "/com/jlog/plugins/pa_qso_party.json",
            "/com/jlog/plugins/ny_qso_party.json",
            "/com/jlog/plugins/ga_qso_party.json",
            "/com/jlog/plugins/nc_qso_party.json",
            "/com/jlog/plugins/wa_qso_party.json",
            "/com/jlog/plugins/on_qso_party.json",
            "/com/jlog/plugins/qc_qso_party.json",
            "/com/jlog/plugins/bc_qso_party.json",
            "/com/jlog/plugins/vt_qso_party.json",
            "/com/jlog/plugins/mn_qso_party.json",
            "/com/jlog/plugins/sc_qso_party.json",
            "/com/jlog/plugins/ns_qso_party.json",
            "/com/jlog/plugins/ok_qso_party.json",
            "/com/jlog/plugins/id_qso_party.json",
            "/com/jlog/plugins/wi_qso_party.json",
            "/com/jlog/plugins/la_qso_party.json",
            "/com/jlog/plugins/ms_qso_party.json",
            "/com/jlog/plugins/nm_qso_party.json",
            "/com/jlog/plugins/mo_qso_party.json",
            "/com/jlog/plugins/ne_qso_party.json",
            "/com/jlog/plugins/seven_qso_party.json",
            "/com/jlog/plugins/de_qso_party.json",
            "/com/jlog/plugins/ky_qso_party.json",
            "/com/jlog/plugins/new_england_qso_party.json",
            "/com/jlog/plugins/cpqp_qso_party.json",
            "/com/jlog/plugins/ar_qso_party.json",
            "/com/jlog/plugins/acqp_qso_party.json",
            "/com/jlog/plugins/wv_qso_party.json",
            "/com/jlog/plugins/al_qso_party.json",
            "/com/jlog/plugins/mdc_qso_party.json",
            "/com/jlog/plugins/wae_cw.json",
            "/com/jlog/plugins/wae_ssb.json",
            "/com/jlog/plugins/wae_rtty.json"
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
    // Load user-installed plugins from ~/.j-log/plugins/
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
        // Importing un-hides — the operator clearly wants this plugin back.
        AppConfig.getInstance().removeHiddenContestId(p.getContestId());
        log.info("Imported plugin {} from {}", p.getContestId(), source);
    }

    /**
     * Remove a plugin from the chooser. For user-installed plugins the JSON file
     * in {@code ~/.j-log/plugins/} is deleted outright. For bundled plugins the
     * contestId is added to the hidden-contest preference so it stays out of the
     * chooser across restarts. A subsequent import of the same contestId un-hides it.
     *
     * @return true if anything changed (file deleted or id newly hidden).
     */
    public boolean removePlugin(String contestId) {
        if (contestId == null || contestId.isBlank()) return false;
        boolean changed = false;

        // Delete any user-installed file(s) whose contents declare this contestId.
        Path dir = DatabaseManager.getInstance().getDataDir().resolve("plugins");
        if (dir.toFile().exists()) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.json")) {
                for (Path f : ds) {
                    try {
                        ContestPlugin cp = mapper.readValue(f.toFile(), ContestPlugin.class);
                        if (contestId.equals(cp.getContestId())) {
                            Files.delete(f);
                            log.info("Deleted user plugin file: {}", f);
                            changed = true;
                        }
                    } catch (Exception ignore) { /* skip unparseable */ }
                }
            } catch (Exception ex) {
                log.warn("scanning plugin dir while removing: {}", ex.getMessage());
            }
        }

        // Record intent to hide (handles bundled plugins too).
        Set<String> hidden = AppConfig.getInstance().getHiddenContestIds();
        if (hidden.add(contestId)) {
            AppConfig.getInstance().setHiddenContestIds(hidden);
            changed = true;
        }

        // Drop from the in-memory list so the chooser updates immediately.
        plugins.removeIf(p -> contestId.equals(p.getContestId()));
        return changed;
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
        Set<String> hidden = AppConfig.getInstance().getHiddenContestIds();
        if (hidden.isEmpty()) return List.copyOf(plugins);
        List<ContestPlugin> visible = new ArrayList<>(plugins.size());
        for (ContestPlugin p : plugins) {
            if (!hidden.contains(p.getContestId())) visible.add(p);
        }
        return List.copyOf(visible);
    }

    public ContestPlugin getById(String id) {
        return plugins.stream()
            .filter(p -> p.getContestId().equals(id))
            .findFirst().orElse(null);
    }
}
