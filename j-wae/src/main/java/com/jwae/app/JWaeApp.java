package com.jwae.app;

import com.jlog.db.DatabaseManager;
import com.jlog.util.AppConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * j-Wae — Worked-All-Europe QTC companion app.
 *
 * Acts as a satellite to j-log: shares the same contest.db (via j-log-engine)
 * and focuses on the WAE QTC exchange — the unique mechanic where EU and DX
 * stations pass up to 10 past-QSO summaries ("QTCs") to each other during
 * each contact. This is only a skeleton; the full app needs a QSO-aware QTC
 * queue, peer tracking, and send/receive UI flows.
 */
public class JWaeApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(JWaeApp.class);

    @Override
    public void init() throws Exception {
        AppConfig.getInstance().load();
        DatabaseManager.getInstance().initAll();
        log.info("j-Wae init complete");
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/jwae/fxml/Main.fxml"));
        Scene scene = new Scene(loader.load(), 820, 520);
        stage.setTitle("j-Wae — WAE QTC Companion");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
