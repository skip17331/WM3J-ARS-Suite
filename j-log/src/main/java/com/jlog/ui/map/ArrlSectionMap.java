package com.jlog.ui.map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
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
 * Real geographic ARRL / RAC section map. Loads pre-built section polygons
 * from {@code /com/jlog/maps/arrl-sections.json} (produced by
 * {@code tools/arrl-map/scripts/build_sections.py}) and renders each section
 * as an {@link SVGPath} with the same CSS state classes as
 * {@link RegionMapPane} so the existing refresh path in
 * {@code ContestLogController.updateStats()} Just Works.
 *
 * <p>Some sections (e.g. DE / MDC / NJ sections / New England) are too small
 * to be clickable at the main-map scale, so the JSON optionally carries an
 * <code>inset</code> geometry: a zoomed copy positioned in a spare corner of
 * the canvas. When present, this class renders <i>both</i> the main polygon
 * and the inset polygon under the same section ID — clicking either fills
 * the entry field, and the worked/current state colours both.
 *
 * <p>Public API is intentionally identical to {@link RegionMapPane}.
 *
 * <p>V1 deferred: AK, PAC (Hawaii + Pacific outlying), PR, VI need composed
 * insets and are not yet rendered.
 */
public class ArrlSectionMap extends Pane {

    private static final Logger log = LoggerFactory.getLogger(ArrlSectionMap.class);
    private static final String RESOURCE = "/com/jlog/maps/arrl-sections.json";

    /** Each section can have one OR two SVGPaths (main + inset). */
    private final Map<String, List<SVGPath>> shapes = new LinkedHashMap<>();
    /**
     * Maps legacy / granular section IDs that plugins use to one or more
     * primary IDs we actually render. e.g. "ONN"→["ON"], "FL"→["NFL","SFL","WCF"].
     */
    private final Map<String, List<String>> aliasTargets = new LinkedHashMap<>();
    private final Set<String> worked = new HashSet<>();
    private String current;
    private String invalid;
    private Consumer<String>         onRegionClicked = id -> {};
    private Function<String, String> tooltipProvider = id -> id;

    public ArrlSectionMap() {
        getStyleClass().add("arrl-section-map");
        setPickOnBounds(false);

        Map<String, Object> raw = loadJson();
        if (raw == null || !raw.containsKey("sections")) {
            log.warn("arrl-sections.json missing or malformed; map will be empty");
            return;
        }

        @SuppressWarnings("unchecked")
        List<Number> vb = (List<Number>) raw.get("viewBox");
        double viewW = vb.get(2).doubleValue();
        double viewH = vb.get(3).doubleValue();
        setPrefSize(viewW, viewH);

        renderInsetFrames(raw);

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

            List<SVGPath> paths = new ArrayList<>(2);
            paths.add(addRegionPath(id, (String) sec.get("svgPath"),
                                    num(sec, "labelX"), num(sec, "labelY"),
                                    /*labelStyleClass=*/ "region-tile-label",
                                    /*isInset=*/ false));

            @SuppressWarnings("unchecked")
            Map<String, Object> inset = (Map<String, Object>) sec.get("inset");
            if (inset != null) {
                paths.add(addRegionPath(id, (String) inset.get("svgPath"),
                                        num(inset, "labelX"), num(inset, "labelY"),
                                        "region-tile-label-inset",
                                        /*isInset=*/ true));
            }
            shapes.put(id, paths);
        }
        log.info("ArrlSectionMap loaded {} sections ({} with inset)",
                shapes.size(),
                shapes.values().stream().filter(l -> l.size() > 1).count());
    }

    private static double num(Map<String, Object> m, String key) {
        return ((Number) m.get(key)).doubleValue();
    }

    private SVGPath addRegionPath(String id, String d, double labelX, double labelY,
                                  String labelStyle, boolean isInset) {
        SVGPath p = new SVGPath();
        p.setId(id + (isInset ? "-inset" : ""));
        p.setContent(d);
        p.getStyleClass().addAll("region-tile", "region-tile-unworked");
        p.setCursor(Cursor.HAND);
        p.setOnMouseClicked(ev -> onRegionClicked.accept(id));

        Tooltip tip = new Tooltip(id);
        tip.setShowDelay(Duration.millis(150));
        tip.setShowDuration(Duration.seconds(10));
        p.setOnMouseEntered(ev -> tip.setText(tooltipProvider.apply(id)));
        Tooltip.install(p, tip);

        Label lbl = new Label(id);
        lbl.getStyleClass().add(labelStyle);
        lbl.setMouseTransparent(true);
        lbl.setLayoutX(labelX - 14);
        lbl.setLayoutY(labelY - 7);
        lbl.setAlignment(Pos.CENTER);

        getChildren().addAll(p, lbl);
        return p;
    }

    /** Draw the rectangle + title for each declared inset region. */
    @SuppressWarnings("unchecked")
    private void renderInsetFrames(Map<String, Object> raw) {
        List<Map<String, Object>> insets = (List<Map<String, Object>>) raw.get("insets");
        if (insets == null) return;
        for (Map<String, Object> ins : insets) {
            List<Number> box = (List<Number>) ins.get("viewBox");
            if (box == null || box.size() < 4) continue;
            double x = box.get(0).doubleValue();
            double y = box.get(1).doubleValue();
            double w = box.get(2).doubleValue();
            double h = box.get(3).doubleValue();

            Rectangle frame = new Rectangle(x, y, w, h);
            frame.getStyleClass().add("region-inset-frame");
            frame.setMouseTransparent(true);
            getChildren().add(frame);

            String name = (String) ins.getOrDefault("name", "");
            if (!name.isBlank()) {
                Label title = new Label(name);
                title.getStyleClass().add("region-inset-title");
                title.setMouseTransparent(true);
                title.setLayoutX(x + 6);
                title.setLayoutY(y + 2);
                getChildren().add(title);
            }
        }
    }

    private Map<String, Object> loadJson() {
        try (InputStream in = ArrlSectionMap.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                log.warn("ARRL section map resource not found at {}", RESOURCE);
                return null;
            }
            return new ObjectMapper().readValue(in, new TypeReference<>() {});
        } catch (Exception ex) {
            log.warn("Failed to load ARRL section map: {}", ex.getMessage());
            return null;
        }
    }

    public Set<String> regionIds() { return Collections.unmodifiableSet(shapes.keySet()); }

    /**
     * Resolve a plugin-supplied section ID to the one or more primary IDs
     * that have rendered polygons. Returns a singleton of the input if no
     * alias is defined and no polygon exists (caller can no-op).
     */
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
        // Translate the plugin's worked set into the rendered primary IDs.
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
        // For "current" we pick the first resolved primary; current is
        // single-target (no 1-to-many semantics).
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
