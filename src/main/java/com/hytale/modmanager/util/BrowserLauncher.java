package com.hytale.modmanager.util;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Ouvre une URL ou un fichier local avec l'application par défaut du
 * système d'exploitation. Utilisé par le pied de page global (réseaux
 * sociaux) et par la page « À propos » (licence).
 * <p>
 * Repose uniquement sur {@code java.awt.Desktop} (aucune API spécifique à
 * Windows, Linux ou macOS) : le comportement est le même sur les trois
 * plateformes, celui de l'association de fichiers/protocoles configurée
 * par l'utilisateur.
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

    /**
     * Tente d'ouvrir un fichier local avec l'application associée par le
     * système d'exploitation (identique à un double-clic dans l'explorateur
     * de fichiers).
     *
     * @return {@code true} si l'ouverture a été demandée avec succès,
     *         {@code false} si le fichier n'existe pas ou si l'opération
     *         n'est pas supportée sur cette machine.
     */
    public static boolean openFile(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            return false;
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file.toFile());
                return true;
            }
        } catch (Exception ignored) {
            // Best-effort : pas d'application associee a ce type de fichier, etc.
        }
        return false;
    }
}

