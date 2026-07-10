package com.hytale.modmanager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    // Le nom et la description de la licence sont dans les fichiers de
    // traduction (cles about.license.name / about.license.description),
    // car il s'agit de texte affiche a l'utilisateur (voir DEVELOPER_GUIDE.md).
    //
    // REPO_URL est le depot GitHub officiel du projet (distinct de
    // GITHUB_URL ci-dessus, qui est le profil de l'auteur utilise dans le
    // pied de page global). LICENSE_URL en derive : mettre a jour REPO_URL
    // suffit si le depot est renomme ou deplace.
    // -------------------------------------------------------------------
    public static final String REPO_URL    = "https://github.com/spectglak-ui/HytaleModManager";
    public static final String LICENSE_URL = REPO_URL + "/blob/main/LICENSE";

    /** Noms de fichier reconnus comme fichier de licence local. */
    private static final String[] LICENSE_FILE_NAMES = { "LICENSE", "LICENSE.txt", "LICENSE.md" };

    private AppInfo() {}

    /**
     * Recherche un fichier de licence local — à côté de l'exécutable/JAR en
     * cours d'exécution, dans son dossier parent (couvre un jar placé dans
     * un sous-dossier type {@code target/} ou {@code app/} par rapport à la
     * racine du projet ou de l'installation), ou dans le répertoire courant
     * (couvre {@code mvn javafx:run} lancé depuis la racine du projet).
     * <p>
     * Ne dépend d'aucune API spécifique à un système d'exploitation : la
     * recherche se limite à des chemins de fichiers, l'ouverture elle-même
     * étant déléguée à {@code BrowserLauncher.openFile}, déjà multiplateforme.
     *
     * @return le chemin trouvé, ou vide si aucun fichier de licence local
     *         n'existe — l'appelant se rabat alors sur {@link #LICENSE_URL}.
     */
    public static Optional<Path> findLocalLicenseFile() {
        List<Path> candidateDirs = new ArrayList<>();

        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isBlank()) {
            candidateDirs.add(Path.of(userDir));
        }

        try {
            Path codeLocation = Path.of(AppInfo.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            Path baseDir = Files.isDirectory(codeLocation) ? codeLocation : codeLocation.getParent();
            if (baseDir != null) {
                candidateDirs.add(baseDir);
                if (baseDir.getParent() != null) {
                    candidateDirs.add(baseDir.getParent());
                }
            }
        } catch (Exception ignored) {
            // Emplacement du code non determinable (environnement restreint) :
            // on se contente des autres candidats.
        }

        for (Path dir : candidateDirs) {
            for (String fileName : LICENSE_FILE_NAMES) {
                Path candidate = dir.resolve(fileName);
                if (Files.isRegularFile(candidate)) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }
}

