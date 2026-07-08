package com.hytale.modmanager.ui;

import com.hytale.modmanager.AppInfo;
import com.hytale.modmanager.BuildInfo;
import com.hytale.modmanager.service.DiagnosticInfo;
import com.hytale.modmanager.util.BrowserLauncher;
import com.hytale.modmanager.util.I18n;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Duration;

import java.time.Year;

/**
 * Contrôleur de la page « À propos ». Fenêtre indépendante (même patron que
 * {@link VersionManagerController}) ouverte depuis le menu Aide → À propos
 * (voir {@code MainController.onAbout()}).
 */
public class AboutController {

    @FXML private ImageView logoView;
    @FXML private Label versionLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label authorLabel;
    @FXML private Label copyrightLabel;
    @FXML private Button btnCheckUpdates;
    @FXML private Label updateStatusLabel;

    @FXML private Label techTitle;
    @FXML private Label lblTechVersionKey;
    @FXML private Label lblTechVersionVal;
    @FXML private Label lblTechJavaKey;
    @FXML private Label lblTechJavaVal;
    @FXML private Label lblTechArchKey;
    @FXML private Label lblTechArchVal;
    @FXML private Label lblTechOsKey;
    @FXML private Label lblTechOsVal;
    @FXML private Label lblTechBuildDateKey;
    @FXML private Label lblTechBuildDateVal;
    @FXML private Label lblTechDataDirKey;
    @FXML private Label lblTechDataDirVal;
    @FXML private Button btnCopyTech;
    @FXML private Label copyStatusLabel;

    @FXML private Label thanksTitle;
    @FXML private Label thanksCommunity;
    @FXML private Label thanksContributors;
    @FXML private Label thanksDev;

    @FXML private Label licenseTitle;
    @FXML private Label licenseNameLabel;
    @FXML private Hyperlink licenseLink;

    private DiagnosticInfo diagnosticInfo;

    @FXML
    private void initialize() {
        try {
            logoView.setImage(new Image(
                    getClass().getResourceAsStream("/com/hytale/modmanager/icon/icon-256.png")));
        } catch (Exception ignored) {
            // Logo absent : la page reste utilisable sans image.
        }

        diagnosticInfo = new DiagnosticInfo();

        boolean hasLicenseLink = AppInfo.LICENSE_URL != null && !AppInfo.LICENSE_URL.isBlank();
        licenseLink.setVisible(hasLicenseLink);
        licenseLink.setManaged(hasLicenseLink);

        applyTranslations();
        I18n.localeProperty().addListener((obs, oldLocale, newLocale) -> applyTranslations());
    }

    private void applyTranslations() {
        versionLabel.setText(I18n.t("about.version", BuildInfo.VERSION));
        descriptionLabel.setText(I18n.t("about.description"));
        authorLabel.setText(I18n.t("about.author", BuildInfo.PUBLISHER));
        copyrightLabel.setText(I18n.t("about.copyright", Year.now().getValue(), BuildInfo.PUBLISHER));
        btnCheckUpdates.setText(I18n.t("about.btn.check_updates"));

        techTitle.setText(I18n.t("about.section.technical"));
        lblTechVersionKey.setText(I18n.t("about.tech.version"));
        lblTechVersionVal.setText(diagnosticInfo.appVersion);
        lblTechJavaKey.setText(I18n.t("about.tech.java"));
        lblTechJavaVal.setText(diagnosticInfo.javaVersion);
        lblTechArchKey.setText(I18n.t("about.tech.arch"));
        lblTechArchVal.setText(diagnosticInfo.architecture);
        lblTechOsKey.setText(I18n.t("about.tech.os"));
        lblTechOsVal.setText(diagnosticInfo.osName);
        lblTechBuildDateKey.setText(I18n.t("about.tech.build_date"));
        lblTechBuildDateVal.setText(diagnosticInfo.buildDate != null
                ? diagnosticInfo.buildDate : I18n.t("about.tech.build_date.unavailable"));
        lblTechDataDirKey.setText(I18n.t("about.tech.user_data_dir"));
        lblTechDataDirVal.setText(diagnosticInfo.userDataDir);
        btnCopyTech.setText(I18n.t("about.btn.copy_tech_info"));

        thanksTitle.setText(I18n.t("about.section.thanks"));
        thanksCommunity.setText(I18n.t("about.thanks.community"));
        thanksContributors.setText(I18n.t("about.thanks.contributors"));
        thanksDev.setText(I18n.t("about.thanks.dev"));

        licenseTitle.setText(I18n.t("about.section.license"));
        licenseNameLabel.setText(AppInfo.LICENSE_NAME);
        licenseLink.setText(I18n.t("about.license.link"));
    }

    @FXML
    private void onCopyTechnicalInfo() {
        ClipboardContent content = new ClipboardContent();
        content.putString(diagnosticInfo.toClipboardText());
        Clipboard.getSystemClipboard().setContent(content);
        showTransientMessage(copyStatusLabel, I18n.t("about.copy.success"));
    }

    @FXML
    private void onCheckUpdates() {
        // Emplacement reserve pour la verification automatique des mises a
        // jour : aucune API officielle de mise a jour des mods/du logiciel
        // n'existe encore. Implementer ici l'appel reseau puis la comparaison
        // de version le jour ou ce service existe (voir ROADMAP.md, moyen
        // terme : "Ameliorer la gestion des mises a jour").
        showTransientMessage(updateStatusLabel, I18n.t("about.updates.not_available"));
    }

    @FXML
    private void onOpenLicenseLink() {
        BrowserLauncher.open(AppInfo.LICENSE_URL);
    }

    /** Affiche un message à côté d'un bouton pendant quelques secondes puis le masque. */
    private void showTransientMessage(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
        PauseTransition pause = new PauseTransition(Duration.seconds(4));
        pause.setOnFinished(e -> {
            label.setVisible(false);
            label.setManaged(false);
        });
        pause.play();
    }
}
