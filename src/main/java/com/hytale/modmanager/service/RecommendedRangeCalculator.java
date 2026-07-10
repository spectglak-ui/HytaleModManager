package com.hytale.modmanager.service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Calcule une plage de compatibilité (« ServerVersion recommandée ») à
 * partir d'un numéro de version détecté, en appliquant une règle générique
 * de type « caret » (comme l'opérateur {@code ^} de semver) :
 * <ul>
 *   <li>si le numéro majeur vaut 0 (Hytale est actuellement en accès
 *       anticipé, version 0.x) : {@code >=0.MINEUR.CORRECTIF <0.(MINEUR+1).0}
 *       — chaque nouvelle « Update » (incrément du numéro mineur) peut
 *       casser la compatibilité des mods existants ;</li>
 *   <li>si le numéro majeur est ≥ 1 : {@code >=MAJEUR.MINEUR.CORRECTIF
 *       <(MAJEUR+1).0.0}, comportement standard semver.</li>
 * </ul>
 * Cette règle ne code en dur aucune version particulière : elle s'applique
 * identiquement quelle que soit la version détectée (0.5.3, 0.5.6, 0.6.0,
 * 1.2.4...). C'est ce qui permet au logiciel de suivre automatiquement les
 * futures versions de Hytale sans modification de code (voir
 * {@link HytaleVersionDetector}, qui fournit la version détectée à cette
 * classe).
 */
public final class RecommendedRangeCalculator {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");

    private RecommendedRangeCalculator() {}

    /**
     * @param detectedVersion version détectée (ex. "0.5.6")
     * @return la plage calculée (ex. ">=0.5.6 <0.6.0"), ou vide si
     *         {@code detectedVersion} ne correspond pas au format "X.Y.Z" attendu.
     */
    public static Optional<String> computeRange(String detectedVersion) {
        int[] parsed = parse(detectedVersion);
        if (parsed == null) {
            return Optional.empty();
        }
        int major = parsed[0];
        int minor = parsed[1];
        int patch = parsed[2];
        String lower = major + "." + minor + "." + patch;
        String upper = (major == 0) ? ("0." + (minor + 1) + ".0") : ((major + 1) + ".0.0");
        return Optional.of(">=" + lower + " <" + upper);
    }

    /**
     * @param detectedVersion version détectée (ex. "0.5.6")
     * @return un libellé court correspondant (ex. "0.5.x"), ou vide si le
     *         format "X.Y.Z" n'est pas respecté.
     */
    public static Optional<String> computeLabel(String detectedVersion) {
        int[] parsed = parse(detectedVersion);
        if (parsed == null) {
            return Optional.empty();
        }
        return Optional.of(parsed[0] + "." + parsed[1] + ".x");
    }

    private static int[] parse(String version) {
        if (version == null) {
            return null;
        }
        Matcher matcher = VERSION_PATTERN.matcher(version.trim());
        if (!matcher.matches()) {
            return null;
        }
        try {
            return new int[] {
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))
            };
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
