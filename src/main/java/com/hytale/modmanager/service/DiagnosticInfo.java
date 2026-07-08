package com.hytale.modmanager.service;

import com.hytale.modmanager.BuildInfo;
import com.hytale.modmanager.util.ConfigPaths;

import java.io.File;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.Date;

/**
 * Rassemble les informations techniques affichées sur la page « À propos »
 * et copiées dans le presse-papiers (bouton « Copier les informations
 * techniques ») pour faciliter les rapports de bugs.
 * <p>
 * Le projet est un logiciel Java/JavaFX (pas .NET) : « Version Java (JRE) »
 * et « Architecture » sont lues via les propriétés système du JRE en cours
 * d'exécution, qui jouent ce rôle pour une application Java.
 */
public final class DiagnosticInfo {

    public final String appVersion;
    public final String javaVersion;
    public final String architecture;
    public final String osName;
    /** {@code null} si non disponible (ex. lancé depuis les classes en mode développement, via {@code mvn javafx:run}). */
    public final String buildDate;
    public final String userDataDir;

    public DiagnosticInfo() {
        this.appVersion = BuildInfo.VERSION;
        this.javaVersion = System.getProperty("java.version", "?");
        this.architecture = System.getProperty("os.arch", "?");
        this.osName = (System.getProperty("os.name", "?") + " " + System.getProperty("os.version", "")).trim();
        this.buildDate = detectBuildDate();
        this.userDataDir = detectUserDataDir();
    }

    /**
     * Déduit une « date de compilation » approximative à partir de la date de
     * dernière modification du fat-jar ou du .jar exécuté (fiable une fois le
     * logiciel empaqueté via {@code mvn package}/{@code build.bat}). Retourne
     * {@code null} en mode développement ({@code mvn javafx:run}), où le code
     * s'exécute depuis un dossier de classes et non depuis un jar.
     */
    private static String detectBuildDate() {
        try {
            File source = new File(DiagnosticInfo.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (source.isFile()) {
                long lastModified = source.lastModified();
                if (lastModified > 0) {
                    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(new Date(lastModified));
                }
            }
        } catch (URISyntaxException | SecurityException | NullPointerException ignored) {
            // Mode developpement (classes non empaquetees) ou acces refuse.
        }
        return null;
    }

    private static String detectUserDataDir() {
        try {
            return ConfigPaths.appDataDir().toString();
        } catch (Exception ex) {
            return "?";
        }
    }

    /** Bloc de texte brut prêt à être copié dans le presse-papiers. */
    public String toClipboardText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Hytale Mod Manager").append(System.lineSeparator());
        sb.append("Version : ").append(appVersion).append(System.lineSeparator());
        sb.append("Java (JRE) : ").append(javaVersion).append(System.lineSeparator());
        sb.append("Architecture : ").append(architecture).append(System.lineSeparator());
        sb.append("Systeme : ").append(osName).append(System.lineSeparator());
        sb.append("Date de compilation : ")
                .append(buildDate != null ? buildDate : "Non disponible (mode developpement)")
                .append(System.lineSeparator());
        sb.append("Dossier de donnees : ").append(userDataDir).append(System.lineSeparator());
        return sb.toString();
    }
}
