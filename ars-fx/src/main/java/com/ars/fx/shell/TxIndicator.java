package com.ars.fx.shell;

import com.ars.fx.data.TxController;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/** The small flashing red "TRANSMITTING" light shown in every module's top bar while the rig is being keyed. */
public final class TxIndicator {
    private TxIndicator() {}

    /** Hidden (and takes no room) until transmitting; click it, or press Esc, to stop. */
    public static HBox build() {
        Region dot = new Region(); dot.setMinSize(11, 11); dot.setMaxSize(11, 11);
        dot.setStyle("-fx-background-color:#ec3b36;-fx-background-radius:99;-fx-effect:dropshadow(gaussian,#ec3b36,8,0.4,0,0);");
        Label text = new Label("TRANSMITTING"); text.setStyle("-fx-text-fill:#ec3b36;-fx-font-size:11px;-fx-font-weight:bold;");
        HBox box = new HBox(7, dot, text); box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-cursor:hand;-fx-padding:4 10 4 10;-fx-background-color:rgba(236,59,54,0.12);-fx-background-radius:6;-fx-border-color:rgba(236,59,54,0.5);-fx-border-radius:6;");
        HBox.setMargin(box, new javafx.geometry.Insets(0, 14, 0, 0));
        Tips.tip(box, "Transmitting — click here, or press Esc, to stop and return to receive");
        box.setOnMouseClicked(e -> TxController.abort());

        Timeline blink = new Timeline(
                new KeyFrame(Duration.ZERO, new javafx.animation.KeyValue(dot.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(450), new javafx.animation.KeyValue(dot.opacityProperty(), 0.15)),
                new KeyFrame(Duration.millis(900), new javafx.animation.KeyValue(dot.opacityProperty(), 1.0)));
        blink.setCycleCount(Animation.INDEFINITE);

        Runnable apply = () -> {
            boolean on = TxController.transmittingProperty().get();
            box.setVisible(on); box.setManaged(on);
            if (on) blink.play(); else { blink.stop(); dot.setOpacity(1.0); }
        };
        ChangeListener<Boolean> l = (o, a, b) -> apply.run();
        box.getProperties().put("txListener", l);                                    // strong ref; the property holds it weakly
        TxController.transmittingProperty().addListener(new WeakChangeListener<>(l));
        apply.run();
        return box;
    }

    /** Esc stops a transmission in progress (and is passed on untouched when nothing is being sent). */
    public static void installEscStop(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE && TxController.transmittingProperty().get()) { TxController.abort(); e.consume(); }
        });
    }
}
