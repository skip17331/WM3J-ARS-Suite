package com.jlog.plugin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

/**
 * Loads a multiplier-value universe (e.g. list of county codes for a state
 * QSO party) from a classpath JSON resource. The file format is a simple
 * array of strings: {@code ["ALAM","ALPI","AMAD",...]}.
 *
 * When a plugin's {@code multiplierList} field points at a valid resource,
 * the controller calls this loader and passes the list to the worked_mults
 * pane so operators see every possible multiplier (unworked = gray, worked
 * = green) rather than only the DB-derived worked set.
 */
public final class MultiplierLists {

    private static final Logger log = LoggerFactory.getLogger(MultiplierLists.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<String, List<String>> CACHE = new HashMap<>();

    private MultiplierLists() {}

    /** Load (and cache) a multiplier list by classpath resource path.
     *  Returns empty list if the resource is missing or malformed — missing
     *  list just means "no predeclared universe, use DB-derived worked only". */
    public static List<String> load(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) return List.of();
        List<String> cached = CACHE.get(resourcePath);
        if (cached != null) return cached;

        try (InputStream is = MultiplierLists.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.warn("multiplier list resource not found: {}", resourcePath);
                CACHE.put(resourcePath, List.of());
                return List.of();
            }
            List<String> list = MAPPER.readValue(is, new TypeReference<List<String>>() {});
            List<String> imm = List.copyOf(list);
            CACHE.put(resourcePath, imm);
            return imm;
        } catch (Exception e) {
            log.warn("failed to load multiplier list {}: {}", resourcePath, e.getMessage());
            CACHE.put(resourcePath, List.of());
            return List.of();
        }
    }
}
