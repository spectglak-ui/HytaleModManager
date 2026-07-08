package com.hytale.modmanager;

/**
 * Constantes de build centralisées : version de l'application et identité
 * de l'éditeur. Ces valeurs sont utilisées :
 * <ul>
 *   <li>dans la boîte de dialogue « À propos »,</li>
 *   <li>dans le script de packaging jpackage (build.bat),</li>
 *   <li>dans le pom.xml (propriété {@code app.version}).</li>
 * </ul>
 * Pour mettre à jour la version lors d'une nouvelle release, modifier
 * uniquement {@link #VERSION} ici et dans {@code pom.xml}.
 */
public final class BuildInfo {

    /** Version de l'application (SemVer). */
    public static final String VERSION   = "1.1.0";

    /** Nom de l'éditeur affiché dans « À propos » et dans l'installateur Windows. */
    public static final String PUBLISHER = "Spectglack";

    private BuildInfo() {}
}
