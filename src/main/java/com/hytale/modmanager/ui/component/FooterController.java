package com.hytale.modmanager.ui.component;

import com.hytale.modmanager.AppInfo;
import com.hytale.modmanager.BuildInfo;
import com.hytale.modmanager.util.BrowserLauncher;
import com.hytale.modmanager.util.I18n;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

/**
 * Contrôleur du pied de page global de l'application (version à gauche,
 * icônes de réseaux sociaux à droite). Inclus de manière autonome dans
 * chaque fenêtre via {@code <fx:include source="footer.fxml"/>}
 * (voir {@code main.fxml}, {@code version_manager.fxml}, {@code about.fxml}) :
 * aucune référence depuis les contrôleurs parents n'est nécessaire, ce qui
 * permet de l'ajouter à une future fenêtre sans modifier le reste du code.
 */
public class FooterController {

    @FXML private Label versionLabel;
    @FXML private Button btnDiscord;
    @FXML private Button btnGithub;
    @FXML private Button btnTwitter;
    @FXML private Button btnTwitch;
    @FXML private Button btnWebsite;

    @FXML
    private void initialize() {
        versionLabel.setText("v" + BuildInfo.VERSION);

        applyTooltips();
        I18n.localeProperty().addListener((obs, oldLocale, newLocale) -> applyTooltips());

        // Le site web est optionnel ("si présent ultérieurement") : masquer
        // proprement l'icône si aucune URL n'est renseignée dans AppInfo.
        boolean hasWebsite = AppInfo.WEBSITE_URL != null && !AppInfo.WEBSITE_URL.isBlank();
        btnWebsite.setVisible(hasWebsite);
        btnWebsite.setManaged(hasWebsite);
    }

    private void applyTooltips() {
        btnDiscord.setTooltip(new Tooltip(I18n.t("footer.tooltip.discord")));
        btnGithub.setTooltip(new Tooltip(I18n.t("footer.tooltip.github")));
        btnTwitter.setTooltip(new Tooltip(I18n.t("footer.tooltip.twitter")));
        btnTwitch.setTooltip(new Tooltip(I18n.t("footer.tooltip.twitch")));
        btnWebsite.setTooltip(new Tooltip(I18n.t("footer.tooltip.website")));
    }

    @FXML private void onOpenDiscord() { BrowserLauncher.open(AppInfo.DISCORD_URL); }
    @FXML private void onOpenGithub()  { BrowserLauncher.open(AppInfo.GITHUB_URL); }
    @FXML private void onOpenTwitter() { BrowserLauncher.open(AppInfo.TWITTER_URL); }
    @FXML private void onOpenTwitch()  { BrowserLauncher.open(AppInfo.TWITCH_URL); }
    @FXML private void onOpenWebsite() { BrowserLauncher.open(AppInfo.WEBSITE_URL); }
}
