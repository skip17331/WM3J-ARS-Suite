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
 * World DXCC entity map. Loads pre-built entity polygons from
 * {@code /com/jlog/maps/dxcc-entities.json} and renders each entity as
 * an {@link SVGPath} keyed by its primary DXCC prefix (K, JA, DL, F, …).
 *
 * <p>Worked / current / invalid / click / tooltip API mirrors
 * {@link ArrlSectionMap}. Section IDs are DXCC prefixes; an
 * {@code aliasTargets} table maps secondary prefixes (W → K, M → G, etc.)
 * and letters-only forms (KH → KH6) to a primary polygon.
 *
 * <p>V1 deferred: sub-country splits (Alaska/Hawaii from USA, Scotland/Wales
 * from UK, Sardinia from Italy, Spanish overseas, etc.) and ~130 rare
 * remote-island entities.
 */
public class DxccMap extends Pane {

    private static final Logger log = LoggerFactory.getLogger(DxccMap.class);
    private static final String RESOURCE = "/com/jlog/maps/dxcc-entities.json";

    /** Compact metadata exposed to consumers (e.g., the companion table view). */
    public record EntityInfo(String prefix, String name, String continent) {}

    private final Map<String, List<SVGPath>>  shapes = new LinkedHashMap<>();
    private final Map<String, List<String>>   aliasTargets = new LinkedHashMap<>();
    private final Map<String, EntityInfo>     entityInfo = new LinkedHashMap<>();
    private final Set<String> worked = new HashSet<>();
    private String current;
    private String invalid;
    private Consumer<String>         onRegionClicked = id -> {};
    private Function<String, String> tooltipProvider = id -> id;

    /** Native viewBox dimensions, captured once so {@link #fitToWidth} can
     *  scale from the original geometry instead of compounding. */
    private double nativeW = 1, nativeH = 1;

    public DxccMap() {
        getStyleClass().add("dxcc-map");
        setPickOnBounds(false);

        Map<String, Object> raw = loadJson();
        if (raw == null || !raw.containsKey("entities")) {
            log.warn("dxcc-entities.json missing or malformed; map will be empty");
            return;
        }

        @SuppressWarnings("unchecked")
        List<Number> vb = (List<Number>) raw.get("viewBox");
        double viewW = vb.get(2).doubleValue();
        double viewH = vb.get(3).doubleValue();
        nativeW = viewW;
        nativeH = viewH;
        setPrefSize(viewW, viewH);

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

            // Label entities at the country level — useful at this zoom only
            // for the larger ones. Use a small font; the click target is the
            // polygon itself so the label can be unobtrusive.
            double lx = ((Number) ent.get("labelX")).doubleValue();
            double ly = ((Number) ent.get("labelY")).doubleValue();
            Label lbl = new Label(id);
            lbl.getStyleClass().add("dxcc-tile-label");
            lbl.setMouseTransparent(true);
            lbl.setLayoutX(lx - 10);
            lbl.setLayoutY(ly - 6);
            lbl.setAlignment(Pos.CENTER);

            List<SVGPath> ps = new ArrayList<>(1);
            ps.add(p);
            shapes.put(id, ps);

            String name = (String) ent.getOrDefault("name", id);
            String continent = (String) ent.getOrDefault("continent", "??");
            entityInfo.put(id, new EntityInfo(id, name, continent));

            getChildren().add(p);
            getChildren().add(lbl);
        }
        log.info("DxccMap loaded {} entities, {} aliases", shapes.size(), aliasTargets.size());
    }

    private Map<String, Object> loadJson() {
        try (InputStream in = DxccMap.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                log.warn("DXCC map resource not found at {}", RESOURCE);
                return null;
            }
            return new ObjectMapper().readValue(in, new TypeReference<>() {});
        } catch (Exception ex) {
            log.warn("Failed to load DXCC map: {}", ex.getMessage());
            return null;
        }
    }

    public Set<String> regionIds() { return Collections.unmodifiableSet(shapes.keySet()); }

    /** Read-only view of entity metadata (prefix → name/continent). */
    public Map<String, EntityInfo> entityInfo() { return Collections.unmodifiableMap(entityInfo); }

    /**
     * Render the map at {@code factor}× its native viewBox size. Applies a
     * {@link javafx.scene.transform.Scale} (pivot at 0,0) AND resizes the
     * Pane's own reported bounds so parent containers reserve only the
     * scaled area — otherwise ScrollPane / HBox keep allocating the full
     * native size and the visible shrink appears to do nothing.
     */
    public void setRenderScale(double factor) {
        double w = getPrefWidth()  * factor;
        double h = getPrefHeight() * factor;
        getTransforms().clear();
        getTransforms().add(new javafx.scene.transform.Scale(factor, factor, 0, 0));
        setMinSize(w, h);
        setPrefSize(w, h);
        setMaxSize(w, h);
    }

    /**
     * Scale the map to exactly fill {@code availWidth}, preserving aspect
     * ratio. Unlike {@link #setRenderScale}, the factor is computed from the
     * captured native viewBox, so this is safe to call repeatedly (e.g. on
     * every container-width change) without compounding.
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

    /** Whether a prefix has been worked, after alias resolution. */
    public boolean isWorked(String id) {
        for (String target : resolveTargets(id)) if (worked.contains(target)) return true;
        return false;
    }

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
