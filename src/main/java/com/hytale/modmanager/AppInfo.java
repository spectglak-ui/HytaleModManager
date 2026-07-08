package com.hytale.modmanager;

/**
 * Contenu éditorial affiché sur la page « À propos » : liens vers les
 * réseaux sociaux (repris aussi dans le pied de page global de
 * l'application) et licence du logiciel.
 * <p>
 * Contrairement à {@link BuildInfo} (identité de build utilisée aussi par
 * jpackage et {@code pom.xml}), cette classe ne contient que du contenu
 * éditorial destiné à être modifié facilement. Le pseudo de l'auteur est
 * {@link BuildInfo#PUBLISHER} (source unique, déjà utilisée par
 * l'installateur Windows) : il n'est pas dupliqué ici.
 * <p>
 * Pour changer un lien, la licence, ou l'URL du site web : modifier
 * uniquement les constantes ci-dessous. Aucune autre modification de code
 * n'est nécessaire (la page « À propos » et le pied de page global les
 * lisent directement).
 */
public final class AppInfo {

    // -------------------------------------------------------------------
    // Réseaux sociaux — utilisés par le pied de page global (footer.fxml /
    // FooterController) et par la page « À propos ».
    // Laisser une URL vide ("") masque automatiquement l'icône correspondante.
    // -------------------------------------------------------------------
    public static final String DISCORD_URL = "https://discord.com/invite/rt3kMuU935";
    public static final String GITHUB_URL  = "https://github.com/spectglak-ui";
    public static final String TWITTER_URL = "https://x.com/spectglakstream";
    public static final String TWITCH_URL  = "https://www.twitch.tv/spectglack";
    public static final String WEBSITE_URL = "https://hytale-maps-hub.preview.emergentagent.com/";

    // -------------------------------------------------------------------
    // Licence — affichée sur la page « À propos ».
    // Modifier ces deux constantes pour changer la licence affichée.
    // Laisser LICENSE_URL vide ("") pour n'afficher que le nom, sans lien.
    // -------------------------------------------------------------------
    public static final String LICENSE_NAME = "Licence non definie - a completer par l'auteur";
    public static final String LICENSE_URL  = "";

    private AppInfo() {}
}
