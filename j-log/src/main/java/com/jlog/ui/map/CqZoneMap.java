package com.jlog.ui.map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * World CQ Zone map (40 zones). Loads polygons from
 * {@code /com/jlog/maps/cq-zones.json} (derived from MIT-licensed
 * go-cq-zone data, © Mark Beech 2015-2018).
 *
 * <p>Section IDs are zero-padded zone numbers ("01"…"40"). One-digit
 * forms ("1"…"9") are accepted via aliasTargets.
 */
public class CqZoneMap extends Pane {

    private static final Logger log = LoggerFactory.getLogger(CqZoneMap.class);
    private static final String RESOURCE = "/com/jlog/maps/cq-zones.json";

    private final Map<String, List<SVGPath>> shapes = new LinkedHashMap<>();
    private final Map<String, List<String>>  aliasTargets = new LinkedHashMap<>();
    private final Set<String> worked = new HashSet<>();
    private String current;
    private String invalid;
    private Consumer<String>         onRegionClicked = id -> {};
    private Function<String, String> tooltipProvider = id -> id;

    public CqZoneMap() {
        getStyleClass().add("cq-zone-map");
        setPickOnBounds(false);

        Map<String, Object> raw = loadJson();
        if (raw == null || !raw.containsKey("entities")) {
            log.warn("cq-zones.json missing or malformed; map will be empty");
            return;
        }
        @SuppressWarnings("unchecked")
        List<Number> vb = (List<Number>) raw.get("viewBox");
        setPrefSize(vb.get(2).doubleValue(), vb.get(3).doubleValue());

        @SuppressWarnings("unchecked")
        Map<String, List<String>> aliases =
                (Map<String, List<String>>) raw.getOrDefault("aliasTargets", Map.of());
        aliasTargets.putAll(aliases);

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> entities =
                (Map<String, Map<String, Object>>) raw.get("entities");

        for (var e : entities.entrySet()) {
            String id = e.getKey();
            Map<String, Object> ent = e.getValue();

            SVGPath p = new SVGPath();
            p.setId(id);
            p.setContent((String) ent.get("svgPath"));
            p.getStyleClass().addAll("region-tile", "region-tile-unworked");
            p.setCursor(Cursor.HAND);
            p.setOnMouseClicked(ev -> onRegionClicked.accept(id));

            Tooltip tip = new Tooltip(id);
            tip.setShowDelay(Duration.millis(150));
            tip.setShowDuration(Duration.seconds(10));
            p.setOnMouseEntered(ev -> tip.setText(tooltipProvider.apply(id)));
            Tooltip.install(p, tip);

            double lx = ((Number) ent.get("labelX")).doubleValue();
            double ly = ((Number) ent.get("labelY")).doubleValue();
            // Strip leading zero from the displayed label ("01" → "1") so the
            // map looks like a familiar zone chart, but section IDs stay
            // zero-padded internally for alias consistency.
            String displayLabel = id.startsWith("0") ? id.substring(1) : id;
            Label lbl = new Label(displayLabel);
            lbl.getStyleClass().add("cq-zone-label");
            lbl.setMouseTransparent(true);
            lbl.setLayoutX(lx - 8);
            lbl.setLayoutY(ly - 8);
            lbl.setAlignment(Pos.CENTER);

            List<SVGPath> ps = new ArrayList<>(1);
            ps.add(p);
            shapes.put(id, ps);
            getChildren().add(p);
            getChildren().add(lbl);
        }
        log.info("CqZoneMap loaded {} zones", shapes.size());
    }

    private Map<String, Object> loadJson() {
        try (InputStream in = CqZoneMap.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                log.warn("CQ zones resource not found at {}", RESOURCE);
                return null;
            }
            return new ObjectMapper().readValue(in, new TypeReference<>() {});
        } catch (Exception ex) {
            log.warn("Failed to load CQ zones map: {}", ex.getMessage());
            return null;
        }
    }

    public Set<String> regionIds() { return Collections.unmodifiableSet(shapes.keySet()); }

    private List<String> resolveTargets(String id) {
        if (shapes.containsKey(id)) return List.of(id);
        List<String> aliased = aliasTargets.get(id);
        if (aliased != null && !aliased.isEmpty()) return aliased;
        return List.of(id);
    }

    public void setWorked(String id, boolean isWorked) {
        for (String target : resolveTargets(id)) {
            if (!shapes.containsKey(target)) continue;
            if (isWorked) worked.add(target); else worked.remove(target);
            restyle(target);
        }
    }

    public void setAllWorked(Collection<String> ids) {
        Set<String> target = new HashSet<>();
        for (String id : ids) target.addAll(resolveTargets(id));
        Set<String> union = new HashSet<>(worked);
        union.addAll(target);
        for (String pid : union) {
            boolean nowWorked = target.contains(pid);
            if (nowWorked) worked.add(pid); else worked.remove(pid);
            restyle(pid);
        }
    }

    public void setCurrent(String id) {
        String prev = this.current;
        List<String> tgts = id == null ? List.of() : resolveTargets(id);
        this.current = tgts.isEmpty() ? null : tgts.get(0);
        if (prev != null) restyle(prev);
        if (this.current != null && shapes.containsKey(this.current)) restyle(this.current);
    }

    public void setInvalid(String id) {
        String prev = this.invalid;
        List<String> tgts = id == null ? List.of() : resolveTargets(id);
        this.invalid = tgts.isEmpty() ? null : tgts.get(0);
        if (prev != null) restyle(prev);
        if (this.invalid != null && shapes.containsKey(this.invalid)) restyle(this.invalid);
    }

    public void clearWorked() {
        Set<String> previous = new HashSet<>(worked);
        worked.clear();
        previous.forEach(this::restyle);
    }

    public void setOnRegionClicked(Consumer<String> cb)        { this.onRegionClicked = cb != null ? cb : id -> {}; }
    public void setTooltipProvider(Function<String, String> p) { this.tooltipProvider = p != null ? p : id -> id; }

    private void restyle(String id) {
        List<SVGPath> ps = shapes.get(id);
        if (ps == null) return;
        for (SVGPath p : ps) {
            p.getStyleClass().removeAll(
                "region-tile-unworked", "region-tile-worked",
                "region-tile-current", "region-tile-invalid");
            if (id.equals(invalid))       p.getStyleClass().add("region-tile-invalid");
            else if (id.equals(current))  p.getStyleClass().add("region-tile-current");
            else if (worked.contains(id)) p.getStyleClass().add("region-tile-worked");
            else                          p.getStyleClass().add("region-tile-unworked");
        }
    }
}
