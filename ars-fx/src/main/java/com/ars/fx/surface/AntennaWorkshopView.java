package com.ars.fx.surface;

import com.ars.fx.data.AntennaCalcs;
import com.ars.fx.data.AntennaCalcs.Calc;
import com.ars.fx.data.AntennaCalcs.In;
import com.ars.fx.data.AntennaRecommender;
import com.ars.fx.data.AntennaRecommender.Q;
import com.ars.fx.data.AntennaRecommender.Result;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.function.Consumer;

import static com.ars.fx.shell.Shell.lbl;

/**
 * Antenna Workshop — lives inside J-Learn. A questionnaire-driven antenna <b>Recommender</b> and a set of live
 * <b>Calculators</b> (antennas, components, formulas). Inputs recompute the outputs as you type.
 */
public final class AntennaWorkshopView {
    private final Consumer<String> openSection;                 // jump to a J-Learn section id, e.g. "09-02"
    private final StackPane body = new StackPane();
    private final Map<String, Map<String, String>> calcValues = new HashMap<>();   // per-calculator inputs, kept while open
    private final AntennaRecommender.Answers answers = new AntennaRecommender.Answers();
    private int step = 0;
    private boolean showResults = false;
    private String currentCalc;
    private boolean calcMode;
    private final HBox tabs = new HBox(8);

    private AntennaWorkshopView(Consumer<String> openSection) { this.openSection = openSection; }

    /** The workshop; {@code calcId} opens that calculator straight away, null starts on the Recommender. */
    public static Region build(Consumer<String> openSection, String calcId) {
        AntennaWorkshopView w = new AntennaWorkshopView(openSection);
        VBox root = new VBox(12);
        root.setPadding(new Insets(24, 28, 30, 28));
        root.setStyle("-fx-background-color:-ars-bg;");
        root.getChildren().addAll(lbl("ANTENNA WORKSHOP", "j2-content-eyebrow"), wrap(lbl("What antenna should I build — and how long do I cut it?", "j2-h1")), w.tabs, w.body);
        if (calcId != null && AntennaCalcs.get(calcId) != null) w.openCalc(calcId); else w.showRecommender();
        ScrollPane sp = new ScrollPane(root); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color:-ars-bg;-fx-background:-ars-bg;"); HBox.setHgrow(sp, Priority.ALWAYS);
        return sp;
    }

    // ---- tabs --------------------------------------------------------------------------------------------
    private void renderTabs() {
        Button rec = tab("Recommender", !calcMode), calc = tab("Calculators", calcMode);
        rec.setOnAction(e -> showRecommender());
        calc.setOnAction(e -> openCalc(currentCalc != null ? currentCalc : "flat-dipole"));
        tabs.getChildren().setAll(rec, calc);
    }
    private static Button tab(String t, boolean on) {
        Button b = new Button(t); b.getStyleClass().add("ars-btn"); if (on) b.getStyleClass().add("primary"); return b;
    }

    // ---- recommender -------------------------------------------------------------------------------------
    private void showRecommender() {
        calcMode = false; renderTabs();
        body.getChildren().setAll(showResults ? resultsPane() : wizardPane());
    }

    private Node wizardPane() {
        var st = AntennaRecommender.STEPS.get(step);
        VBox v = new VBox(14);
        v.getChildren().add(lbl(st.title(), "j2-content-h"));
        for (Q q : st.questions()) v.getChildren().add(question(q));
        Button back = new Button("← Back"); back.getStyleClass().add("ars-btn"); back.setDisable(step == 0);
        back.setOnAction(e -> { step--; showRecommender(); });
        boolean last = step == AntennaRecommender.STEPS.size() - 1;
        Button next = new Button(last ? "See Results →" : "Next →"); next.getStyleClass().addAll("ars-btn", "primary");
        next.setOnAction(e -> { if (last) showResults = true; else step++; showRecommender(); });
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox nav = new HBox(10, back, lbl("Step " + (step + 1) + " of " + AntennaRecommender.STEPS.size(), "j2-content-meta"), sp, next);
        nav.setAlignment(Pos.CENTER_LEFT);
        v.getChildren().add(nav);
        return v;
    }

    private Node question(Q q) {
        VBox box = new VBox(5);
        box.getChildren().add(lbl(q.text(), "j2-topic-nm"));
        switch (q.type()) {
            case NUMBER -> {
                TextField tf = new TextField(Objects.toString(answers.get(q.id()), "")); tf.setPromptText(q.placeholder()); tf.setMaxWidth(180);
                tf.textProperty().addListener((o, a, b) -> answers.put(q.id(), b));
                box.getChildren().add(tf);
            }
            case SINGLE -> {
                ToggleGroup g = new ToggleGroup();
                Object cur = answers.get(q.id());
                FlowPane fp = new FlowPane(14, 4);
                List<String[]> ch = new ArrayList<>(Arrays.asList(q.choices()));
                ch.add(new String[]{"__skip", "Skip / don't know"});
                for (String[] c : ch) {
                    RadioButton rb = new RadioButton(c[1]); rb.setToggleGroup(g); rb.setSelected(c[0].equals(cur));
                    rb.getStyleClass().add("j2-content-body");
                    rb.selectedProperty().addListener((o, a, sel) -> { if (sel) answers.put(q.id(), c[0]); });
                    fp.getChildren().add(rb);
                }
                box.getChildren().add(fp);
            }
            case MULTI -> {
                FlowPane fp = new FlowPane(14, 4);
                for (String[] c : q.choices()) {
                    CheckBox cb = new CheckBox(c[1]); cb.getStyleClass().add("j2-content-body");
                    @SuppressWarnings("unchecked") List<String> cur = answers.get(q.id()) instanceof List<?> l ? (List<String>) l : null;
                    cb.setSelected(cur != null && cur.contains(c[0]));
                    cb.selectedProperty().addListener((o, a, sel) -> {
                        @SuppressWarnings("unchecked") List<String> list = answers.get(q.id()) instanceof List<?> l2 ? (List<String>) l2 : new ArrayList<>();
                        if (sel && !list.contains(c[0])) list.add(c[0]);
                        if (!sel) list.remove(c[0]);
                        answers.put(q.id(), list);
                    });
                    fp.getChildren().add(cb);
                }
                box.getChildren().add(fp);
            }
        }
        return box;
    }

    private Node resultsPane() {
        VBox v = new VBox(12);
        v.getChildren().add(lbl("Your best-fit antennas", "j2-content-h"));
        List<Result> show = AntennaRecommender.rank(answers);
        if (show.isEmpty()) v.getChildren().add(wrap(lbl("No matching antennas — try answering more questions or relaxing constraints.", "j2-content-body")));
        int i = 1;
        for (Result r : show) {
            VBox card = new VBox(6); card.getStyleClass().add("j2-code-box"); card.setMaxWidth(Double.MAX_VALUE);
            Label title = lbl(i++ + ".  " + r.ant().name() + "     score " + String.format(Locale.ROOT, "%.1f", r.score()), "j2-topic-nm");
            Button open = new Button("▶ Open in calculator"); open.getStyleClass().addAll("ars-btn", "primary");
            open.setOnAction(e -> openCalc(r.ant().id()));
            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
            HBox head = new HBox(10, title, sp, open); head.setAlignment(Pos.CENTER_LEFT);
            card.getChildren().addAll(head, wrap(lbl(r.ant().summary(), "j2-content-body")));
            for (String reason : r.reasons()) card.getChildren().add(wrap(lbl("•  " + reason, "j2-content-body")));
            Label read = lbl("Read §" + r.ant().section() + " for the full design notes →", "j2-seclink");
            read.setOnMouseClicked(e -> openSection.accept(r.ant().section()));
            card.getChildren().add(read);
            v.getChildren().add(card);
        }
        Button again = new Button("↺ Start over"); again.getStyleClass().add("ars-btn");
        again.setOnAction(e -> { answers.clear(); step = 0; showResults = false; showRecommender(); });
        Button edit = new Button("← Change answers"); edit.getStyleClass().add("ars-btn");
        edit.setOnAction(e -> { showResults = false; step = 0; showRecommender(); });
        v.getChildren().add(new HBox(10, edit, again));
        return v;
    }

    // ---- calculators -------------------------------------------------------------------------------------
    private void openCalc(String id) {
        Calc c = AntennaCalcs.get(id);
        if (c == null) return;
        currentCalc = id; calcMode = true; renderTabs();
        Map<String, String> vals = calcValues.computeIfAbsent(id, k -> c.defaults());

        VBox list = new VBox(2); list.setMinWidth(210); list.setPrefWidth(210); list.setMaxWidth(210);
        addGroup(list, "ANTENNAS", AntennaCalcs.Kind.ANTENNA);
        addGroup(list, "COMPONENTS", AntennaCalcs.Kind.COMPONENT);
        addGroup(list, "FORMULAS", AntennaCalcs.Kind.FORMULA);

        VBox panel = new VBox(10); HBox.setHgrow(panel, Priority.ALWAYS);
        Label read = lbl("Read §" + c.section() + " →", "j2-seclink"); read.setPadding(new Insets(0));
        read.setOnMouseClicked(e -> openSection.accept(c.section()));
        panel.getChildren().addAll(wrap(lbl(c.name(), "j2-h1")), read);

        GridPane out = new GridPane(); out.setHgap(14); out.setVgap(4);
        ColumnConstraints k = new ColumnConstraints(); k.setPercentWidth(38); ColumnConstraints val = new ColumnConstraints(); val.setPercentWidth(62);
        out.getColumnConstraints().addAll(k, val);
        Runnable recompute = () -> renderOutput(out, c, vals);

        VBox inputs = new VBox(8);
        inputs.getChildren().add(lbl("INPUTS", "j2-sec"));
        for (In in : c.inputs()) inputs.getChildren().add(inputNode(in, vals, recompute));
        VBox outputs = new VBox(6, lbl("OUTPUTS", "j2-sec"), out);
        HBox grid = new HBox(24, inputs, outputs);
        inputs.setMinWidth(260); inputs.setPrefWidth(300); inputs.setMaxWidth(320);
        HBox.setHgrow(outputs, Priority.ALWAYS);
        panel.getChildren().add(grid);
        if (c.diagram() != null) {
            Label d = lbl(c.diagram(), "j2-code"); d.setWrapText(false);
            VBox box = new VBox(d); box.getStyleClass().add("j2-code-box");
            panel.getChildren().addAll(lbl("DIAGRAM", "j2-sec"), box);
        }
        if (!c.notes().isEmpty()) {
            panel.getChildren().add(lbl("NOTES", "j2-sec"));
            for (String n : c.notes()) {
                HBox h = new HBox(8, lbl("•", "j2-content-body"), wrap(lbl(n, "j2-content-body")));
                HBox.setHgrow(h.getChildren().get(1), Priority.ALWAYS); panel.getChildren().add(h);
            }
        }
        recompute.run();
        HBox layout = new HBox(18, list, panel);
        layout.setAlignment(Pos.TOP_LEFT);
        body.getChildren().setAll(layout);
    }

    private void addGroup(VBox list, String title, AntennaCalcs.Kind kind) {
        list.getChildren().add(lbl(title, "j2-sec"));
        for (Calc c : AntennaCalcs.of(kind)) {
            Label l = lbl(c.name() + "   §" + c.section(), "j2-seclink"); l.setPadding(new Insets(5, 8, 5, 8));
            l.setMaxWidth(Double.MAX_VALUE); l.setWrapText(true);
            if (c.id().equals(currentCalc)) l.getStyleClass().add("on");
            l.setOnMouseClicked(e -> openCalc(c.id()));
            list.getChildren().add(l);
        }
    }

    private static Node inputNode(In in, Map<String, String> vals, Runnable recompute) {
        VBox box = new VBox(3);
        box.getChildren().add(wrap(lbl(in.label(), "j2-content-meta")));
        if (in.select()) {
            ComboBox<String> cb = new ComboBox<>(); cb.setMaxWidth(Double.MAX_VALUE);
            String cur = vals.get(in.id());
            for (String[] c : in.choices()) { cb.getItems().add(c[1]); if (c[0].equals(cur)) cb.getSelectionModel().select(c[1]); }
            cb.valueProperty().addListener((o, a, b) -> {
                for (String[] c : in.choices()) if (c[1].equals(b)) vals.put(in.id(), c[0]);
                recompute.run();
            });
            box.getChildren().add(cb);
        } else {
            TextField tf = new TextField(vals.get(in.id()));
            tf.textProperty().addListener((o, a, b) -> { vals.put(in.id(), b); recompute.run(); });
            box.getChildren().add(tf);
        }
        if (in.hint() != null) box.getChildren().add(wrap(lbl(in.hint(), "j2-content-meta")));
        return box;
    }

    private static void renderOutput(GridPane out, Calc c, Map<String, String> vals) {
        out.getChildren().clear();
        List<String[]> rows;
        try { rows = c.compute().apply(vals); } catch (RuntimeException ex) { rows = java.util.Collections.singletonList(new String[]{"Error", String.valueOf(ex.getMessage())}); }
        int r = 0;
        for (String[] row : rows) {
            boolean head = row[0].startsWith("──");
            if (row[0].isEmpty() && row[1].isEmpty()) { Region gap = new Region(); gap.setMinHeight(6); out.add(gap, 0, r++, 2, 1); continue; }
            if (head) { Label h = wrap(lbl(row[0].replace("──", "").strip(), "j2-content-h")); out.add(h, 0, r++, 2, 1); continue; }
            if (row[0].isEmpty()) { out.add(wrap(lbl(row[1], "j2-content-body")), 0, r++, 2, 1); continue; }
            Label k = wrap(lbl(row[0], "j2-content-meta")), val = wrap(lbl(row[1], "j2-content-body"));
            out.add(k, 0, r); out.add(val, 1, r); r++;
        }
    }

    private static Label wrap(Label l) { l.setWrapText(true); l.setMaxWidth(Double.MAX_VALUE); return l; }
}
