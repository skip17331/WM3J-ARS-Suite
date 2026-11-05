package com.ars.fx.surface;

import com.ars.fx.data.LegalDocs;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import static com.ars.fx.shell.Shell.lbl;

/** The licence, Code of Conduct and legal notes, read from their local copies in a window of their own - no web page. */
final class LegalView {
    private LegalView() {}

    private static Stage stage;
    private static TabPane tabs;

    /** Open (or raise) the window on the document with this id ("license", "conduct" or "legal"). */
    static void show(Node owner, String docId) {
        if (stage == null) {
            tabs = new TabPane(); tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            for (LegalDocs.Doc d : LegalDocs.DOCS) {
                TextArea t = new TextArea(LegalDocs.read(d));
                t.setEditable(false); t.setWrapText(true);
                t.setStyle("-fx-font-family:'JetBrains Mono';-fx-font-size:12px;");
                Tab tab = new Tab(d.title(), t); tab.setId(d.id());
                tabs.getTabs().add(tab);
            }
            Label where = lbl("Local copies in " + LegalDocs.dir() + " — checked and refreshed when an update is downloaded.", "jl-emeta");
            where.setWrapText(true); where.setPadding(new Insets(8, 12, 8, 12));
            VBox root = new VBox(tabs, where);
            VBox.setVgrow(tabs, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 860, 660);
            com.ars.fx.Theme.apply(scene, false);
            com.ars.fx.Themes.applyTo(root, "info");
            Stage st = new Stage(); st.setTitle("License and legal notes"); st.setScene(scene);
            com.ars.fx.AppIcons.apply(st, null);
            if (owner != null && owner.getScene() != null && owner.getScene().getWindow() != null) st.initOwner(owner.getScene().getWindow());
            st.setOnHidden(e -> { stage = null; tabs = null; });
            stage = st;
        }
        for (Tab t : tabs.getTabs()) if (t.getId().equals(docId)) tabs.getSelectionModel().select(t);
        stage.show(); stage.toFront();
    }
}
