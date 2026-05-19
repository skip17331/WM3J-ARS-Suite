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
 * Generic per-state county map for state QSO party plugins. Loads
 * {@code /com/jlog/maps/county-<state>.json} for the state code passed at
 * construction. Section IDs are the contest's 3- or 4-letter county codes.
 *
 * <p>Sized for a single state (900×700 default viewBox). Each map ships
 * tiny (CA = 23 KB for 58 counties).
 */
public class CountyMap extends Pane {

    private static final Logger log = LoggerFactory.getLogger(CountyMap.class);

    private final String stateCode;
    private final Map<String, List<SVGPath>> shapes = new LinkedHashMap<>();
    private final Map<String, List<String>>  aliasTargets = new LinkedHashMap<>();
    private final Set<String> worked = new HashSet<>();
    private double nativeW, nativeH;          // viewBox size, for fitToWidth
    private String current;
    private String invalid;
    private Consumer<String>         onRegionClicked = id -> {};
    private Function<String, String> tooltipProvider = id -> id;

    public CountyMap(String stateCode) {
        this.stateCode = stateCode.toUpperCase();
        getStyleClass().add("county-map");
        setPickOnBounds(false);

        Map<String, Object> raw = loadJson();
        if (raw == null || !raw.containsKey("sections")) {
            log.warn("county-{}.json missing or malformed; map will be empty", stateCode);
            return;
        }
        @SuppressWarnings("unchecked")
        List<Number> vb = (List<Number>) raw.get("viewBox");
        nativeW = vb.get(2).doubleValue();
        nativeH = vb.get(3).doubleValue();
        setPrefSize(nativeW, nativeH);

        @SuppressWarnings("unchecked")
        Map<String, List<String>> aliases =
                (Map<String, List<String>>) raw.getOrDefault("aliasTargets", Map.of());
        aliasTargets.putAll(aliases);

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> sections =
                (Map<String, Map<String, Object>>) raw.get("sections");

        for (var e : sections.entrySet()) {
            String id = e.getKey();
            Map<String, Object> sec = e.getValue();

            SVGPath p = new SVGPath();
            p.setId(id);
            p.setContent((String) sec.get("svgPath"));
            p.getStyleClass().addAll("region-tile", "region-tile-unworked");
            p.setCursor(Cursor.HAND);
            p.setOnMouseClicked(ev -> onRegionClicked.accept(id));

            String displayName = (String) sec.getOrDefault("name", id);
            Tooltip tip = new Tooltip(displayName);
            tip.setShowDelay(Duration.millis(150));
            tip.setShowDuration(Duration.seconds(10));
            p.setOnMouseEntered(ev -> tip.setText(tooltipProvider.apply(id)));
            Tooltip.install(p, tip);

            double lx = ((Number) sec.get("labelX")).doubleValue();
            double ly = ((Number) sec.get("labelY")).doubleValue();
            Label lbl = new Label(id);
            lbl.getStyleClass().add("county-tile-label");
            lbl.setMouseTransparent(true);
            lbl.setLayoutX(lx - 16);
            lbl.setLayoutY(ly - 7);
            lbl.setAlignment(Pos.CENTER);

            List<SVGPath> ps = new ArrayList<>(1);
            ps.add(p);
            shapes.put(id, ps);
            getChildren().add(p);
            getChildren().add(lbl);
        }
        log.info("CountyMap[{}] loaded {} counties", stateCode, shapes.size());
    }

    private Map<String, Object> loadJson() {
        String resource = "/com/jlog/maps/county-" + stateCode.toLowerCase() + ".json";
        try (InputStream in = CountyMap.class.getResourceAsStream(resource)) {
            if (in == null) {
                log.warn("County map resource not found at {}", resource);
                return null;
            }
            return new ObjectMapper().readValue(in, new TypeReference<>() {});
        } catch (Exception ex) {
            log.warn("Failed to load county map for {}: {}", stateCode, ex.getMessage());
            return null;
        }
    }

    public String stateCode() { return stateCode; }
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

    /**
     * Scale the map to exactly fill {@code availWidth}, preserving aspect
     * ratio. Computed from the captured native viewBox so it is safe to call
     * repeatedly on every container-width change (no compounding). Mirrors
     * {@code DxccMap.fitToWidth} so the resizable side column behaves the
     * same for the embedded 7QP county map as for the ARRL-DX world map.
     */
    public void fitToWidth(double availWidth) {
        if (availWidth <= 0 || nativeW <= 0) return;
        double factor = availWidth / nativeW;
        double w = nativeW * factor;
        double h = nativeH * factor;
        getTransforms().clear();
        getTransforms().add(new javafx.scene.transform.Scale(factor, factor, 0, 0));
        setMinSize(w, h);
        setPrefSize(w, h);
        setMaxSize(w, h);
    }

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

    /**
     * Parse a plugin's multiplierList path
     * (e.g. {@code /com/jlog/counties/ca.json}) into a state code ("CA")
     * if the path matches the expected pattern. Returns {@code null} if
     * the plugin doesn't expose a county list we can map.
     */
    public static String stateFromMultiplierListPath(String path) {
        if (path == null) return null;
        int slash = path.lastIndexOf('/');
        if (slash < 0) return null;
        String file = path.substring(slash + 1);
        if (!file.endsWith(".json")) return null;
        String code = file.substring(0, file.length() - 5).toUpperCase();
        if (code.length() != 2) return null;
        return code;
    }
}
