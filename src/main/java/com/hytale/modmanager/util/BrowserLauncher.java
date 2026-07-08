package com.hytale.modmanager.util;

import java.awt.Desktop;
import java.net.URI;

/**
 * Ouvre une URL dans le navigateur par défaut du système d'exploitation.
 * Utilisé par le pied de page global (réseaux sociaux) et par la page
 * « À propos » (lien de licence).
 */
public final class BrowserLauncher {

    private BrowserLauncher() {}

    /**
     * Tente d'ouvrir {@code url} dans le navigateur par défaut.
     *
     * @return {@code true} si l'ouverture a été demandée avec succès,
     *         {@code false} si l'URL est vide ou si l'opération n'est pas
     *         supportée sur cette machine (aucun navigateur par défaut
     *         configuré, environnement sans interface graphique, etc.).
     */
    public static boolean open(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return true;
            }
        } catch (Exception ignored) {
            // Best-effort : aucun navigateur par defaut disponible, ou URL invalide.
        }
        return false;
    }
}
