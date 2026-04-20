package com.hamradio.jsat.service.registry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.jsat.model.SatelliteDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads the satellite capability registry from satellite-registry.json
 * bundled in the JAR resources.
 */
public class SatelliteRegistry {

    private static final Logger log = LoggerFactory.getLogger(SatelliteRegistry.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final List<SatelliteDefinition> satellites;

    public SatelliteRegistry() {
        this.satellites = load();
    }

    private List<SatelliteDefinition> load() {
        try (InputStream in = getClass().getResourceAsStream("/data/satellite-registry.json")) {
            if (in == null) {
                log.error("satellite-registry.json not found in resources");
                return List.of();
            }
            JsonNode root = MAPPER.readTree(in);
            List<SatelliteDefinition> list = new ArrayList<>();
            for (JsonNode node : root.path("satellites")) {
                SatelliteDefinition def = MAPPER.treeToValue(node, SatelliteDefinition.class);
                list.add(def);
            }
            log.info("Loaded {} satellite definitions from registry", list.size());
            return Collections.unmodifiableList(list);
        } catch (Exception e) {
            log.error("Failed to load satellite registry: {}", e.getMessage());
            return List.of();
        }
    }

    public List<SatelliteDefinition> getAll() { return satellites; }

    public List<SatelliteDefinition> getEnabled() {
        return satellites.stream().filter(s -> s.enabled).toList();
    }

    public List<SatelliteDefinition> getFmSatellites() {
        return satellites.stream().filter(SatelliteDefinition::isFm).toList();
    }

    public List<SatelliteDefinition> getLinearSatellites() {
        return satellites.stream().filter(SatelliteDefinition::isLinearTransponder).toList();
    }

    public List<SatelliteDefinition> getAprsSatellites() {
        return satellites.stream().filter(SatelliteDefinition::isAprs).toList();
    }

    public SatelliteDefinition findByName(String name) {
        if (name == null) return null;
        String lc = name.toLowerCase();
        return satellites.stream()
            .filter(s -> s.name.toLowerCase().contains(lc) || lc.contains(s.name.toLowerCase()))
            .findFirst().orElse(null);
    }
}
