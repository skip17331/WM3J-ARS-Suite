package com.jlog.ui.contest;

import javafx.collections.FXCollections;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.function.Consumer;

/**
 * Scrollable list of DXCC entities (or any other multiplier pool). Clicks emit
 * the entity name; worked entries are flagged with a ✓ and a "worked" style
 * class so the stylesheet can color them.
 */
public class DxccListPane extends VBox {

    private final ListView<String> list = new ListView<>();
    private final Set<String> worked    = new HashSet<>();
    private final Set<String> all       = new LinkedHashSet<>();
    private Consumer<String>  onEntityClicked = e -> {};

    public DxccListPane(Collection<String> entities) {
        getStyleClass().add("pane-content");
        all.addAll(entities);
        list.getStyleClass().add("dxcc-list");
        list.setItems(FXCollections.observableArrayList(all));
        list.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("worked"), false);
                } else {
                    setText((worked.contains(item) ? "✔ " : "  ") + item);
                    pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("worked"), worked.contains(item));
                }
            }
        });
        list.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            String sel = list.getSelectionModel().getSelectedItem();
            if (sel != null) onEntityClicked.accept(sel);
        });
        getChildren().add(list);
    }

    public void setWorked(String entity, boolean isWorked) {
        if (!all.contains(entity)) return;
        if (isWorked) worked.add(entity); else worked.remove(entity);
        list.refresh();
    }

    public void setAllWorked(Collection<String> ids) {
        worked.clear();
        worked.addAll(ids);
        list.refresh();
    }

    public void setOnEntityClicked(Consumer<String> cb) {
        this.onEntityClicked = cb != null ? cb : e -> {};
    }
}
