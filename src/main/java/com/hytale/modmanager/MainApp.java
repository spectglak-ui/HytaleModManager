package com.hytale.modmanager;

import com.hytale.modmanager.service.AppSettings;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Point d'entrée JavaFX.
 * Charge les paramètres (langue) avant l'interface, puis applique l'icône et
 * le thème sombre. La langue détectée ou précédemment choisie est déjà active
 * au moment du chargement du FXML, donc l'interface apparaît directement dans
 * la bonne langue.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Chargement des paramètres (langue, etc.) en premier
        AppSettings.get();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/hytale/modmanager/view/main.fxml"));
        Parent root = loader.load();
        com.hytale.modmanager.ui.MainController controller = loader.getController();

        Scene scene = new Scene(root, 1300, 820);
        scene.getStylesheets().add(
                getClass().getResource("/com/hytale/modmanager/css/dark-theme.css").toExternalForm());

        primaryStage.getIcons().addAll(
                new Image(getClass().getResourceAsStream("/com/hytale/modmanager/icon/icon-256.png")),
                new Image(getClass().getResourceAsStream("/com/hytale/modmanager/icon/icon-64.png")),
                new Image(getClass().getResourceAsStream("/com/hytale/modmanager/icon/icon-32.png"))
        );

        primaryStage.setTitle(BuildInfo.VERSION.isEmpty()
                ? "Hytale Mod Manager"
                : "Hytale Mod Manager  v" + BuildInfo.VERSION);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(660);
        primaryStage.setOnCloseRequest(event -> {
            if (!controller.requestClose()) event.consume();
        });
        primaryStage.show();
    }
}
