package com.hamlog.app;

import com.hamlog.controller.ContestLogController;
import com.hamlog.i18n.I18n;
import com.hamlog.plugin.ContestPlugin;
import com.hamlog.plugin.PluginLoader;
import com.hamlog.util.AppConfig;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * Dialog that lists available contest plugins and lets the user choose
 * one before opening the Contest Log window.
 *
 * The ListView stores ContestPlugin objects directly (with a custom
 * cell factory for display), so index lookup is never needed and there
 * is no possibility of a stale-index mismatch after an import.
 */
public class ContestChooser {

    private static final Logger log = LoggerFactory.getLogger(ContestChooser.class);

    private final Stage owner;

    public ContestChooser(Stage owner) {
        this.owner = owner;
    }

    public void show() {
        Stage dialog = new Stage();
        dialog.setTitle(I18n.get("contest.chooser.title"));
        dialog.initOwner(owner);

        // ListView holds ContestPlugin objects directly — no index mapping needed
        ListView<ContestPlugin> list = new ListView<>();
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ContestPlugin item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getContestName() + "  [" + item.getContestId() + "]");
                }
            }
        });
        list.setPrefHeight(260);
        refreshList(list);

        Button importBtn = new Button(I18n.get("contest.import.plugin"));
        Button selectBtn = new Button(I18n.get("contest.select"));
        Button cancelBtn = new Button(I18n.get("button.cancel"));

        selectBtn.setDefaultButton(true);
        selectBtn.disableProperty().bind(
            list.getSelectionModel().selectedItemProperty().isNull());

        importBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Import Contest Plugin");
            fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Contest Plugin (*.json)", "*.json"));
            File f = fc.showOpenDialog(dialog);
            if (f != null) {
                try {
                    PluginLoader.getInstance().importPlugin(f.toPath());
                    refreshList(list);
                } catch (Exception ex) {
                    log.error("Failed to import plugin", ex);
                    new Alert(Alert.AlertType.ERROR,
                        "Failed to import plugin:\n" + ex.getMessage()).showAndWait();
                }
            }
        });

        selectBtn.setOnAction(e -> {
            ContestPlugin chosen = list.getSelectionModel().getSelectedItem();
            if (chosen == null) return;
            dialog.close();
            launchContestLog(chosen);
        });

        cancelBtn.setOnAction(e -> dialog.close());

        // Double-click to select
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2
                    && list.getSelectionModel().getSelectedItem() != null) {
                dialog.close();
                launchContestLog(list.getSelectionModel().getSelectedItem());
            }
        });

        HBox buttons = new HBox(10, importBtn, selectBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        Label prompt = new Label(I18n.get("contest.chooser.prompt"));
        prompt.getStyleClass().add("chooser-prompt");

        VBox root = new VBox(14, prompt, list, buttons);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 540, 380);
        HamLogApp.applyTheme(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    /** Reload the ListView from the plugin loader. */
    private void refreshList(ListView<ContestPlugin> list) {
        List<ContestPlugin> plugins = PluginLoader.getInstance().getAvailablePlugins();
        list.setItems(FXCollections.observableArrayList(plugins));
        if (!plugins.isEmpty()) {
            list.getSelectionModel().selectFirst();
        }
        // Show a helpful message if truly empty
        if (plugins.isEmpty()) {
            list.setPlaceholder(new Label(
                "No contest plugins found.\nUse 'Import Plugin...' to add a JSON plugin file."));
        }
    }

    private void launchContestLog(ContestPlugin plugin) {
        try {
            AppConfig.getInstance().setActiveContestId(plugin.getContestId());
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/hamlog/fxml/ContestLog.fxml"),
                I18n.getBundle());
            Scene scene = new Scene(loader.load());
            HamLogApp.applyTheme(scene);

            ContestLogController ctrl = loader.getController();
            ctrl.initPlugin(plugin);

            owner.setScene(scene);
            owner.setTitle("HamLog — Contest Log: " + plugin.getContestName());
            owner.show();
            log.info("Contest Log opened for {}", plugin.getContestName());
        } catch (Exception ex) {
            log.error("Failed to open Contest Log", ex);
            new Alert(Alert.AlertType.ERROR,
                "Failed to open Contest Log:\n" + ex.getMessage()).showAndWait();
        }
    }
}
