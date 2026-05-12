package com.morsetrainer.trainer.qso;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.morsetrainer.core.Logger;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Loads categorised phrase fragments from the classpath resource
 * {@code /data/qso-phrases.json}. Used by {@link QsoGenerator} to assemble
 * realistic CW exchanges from a large pool of mix-and-match fragments rather
 * than a handful of fixed templates.
 */
public final class PhrasePool {

    private static final String RESOURCE = "/data/qso-phrases.json";

    private final Map<String, List<String>> categories;

    public PhrasePool(Map<String, List<String>> categories) {
        this.categories = Map.copyOf(categories);
    }

    /** Loads the bundled pool. Returns an empty pool if the resource is missing. */
    public static PhrasePool loadDefault() {
        try (InputStream in = PhrasePool.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                Logger.warn("qso-phrases.json not found on classpath at %s", RESOURCE);
                return new PhrasePool(Map.of());
            }
            ObjectMapper m = new ObjectMapper();
            Map<String, Object> raw = m.readValue(in, new TypeReference<>() {});
            Map<String, List<String>> out = new java.util.HashMap<>();
            for (var e : raw.entrySet()) {
                if (e.getKey().startsWith("_")) continue;
                if (e.getValue() instanceof List<?> list && !list.isEmpty()
                        && list.get(0) instanceof String) {
                    @SuppressWarnings("unchecked")
                    List<String> sl = (List<String>) list;
                    out.put(e.getKey(), List.copyOf(sl));
                }
            }
            return new PhrasePool(out);
        } catch (Exception ex) {
            Logger.warn("Failed to load %s: %s", RESOURCE, ex.getMessage());
            return new PhrasePool(Map.of());
        }
    }

    public boolean has(String category) {
        List<String> xs = categories.get(category);
        return xs != null && !xs.isEmpty();
    }

    public List<String> get(String category) {
        return categories.getOrDefault(category, List.of());
    }

    /** Pick a random entry, or {@code fallback} if the category is missing/empty. */
    public String pick(String category, Random random, String fallback) {
        List<String> xs = categories.get(category);
        if (xs == null || xs.isEmpty()) return fallback;
        return xs.get(random.nextInt(xs.size()));
    }

    public int size() {
        return categories.values().stream().mapToInt(List::size).sum();
    }
}
