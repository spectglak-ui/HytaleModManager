package com.hytale.modmanager.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitaires de comparaison de versions de type semver, utilisés pour trier les
 * versions de Hytale et déterminer automatiquement la version la plus récente.
 *
 * Le logiciel ne dispose pas d'un schéma officiel des plages de versions Hytale ;
 * il se contente donc d'extraire la première séquence "x.y.z" rencontrée dans la
 * chaîne (généralement la borne inférieure introduite par ">=") pour pouvoir
 * comparer les versions entre elles.
 */
public final class VersionUtils {

    private static final Pattern NUMBER_TRIPLET = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");

    private VersionUtils() {
    }

    public static int[] extractLowerBound(String range) {
        if (range == null) {
            return new int[]{0, 0, 0};
        }
        Matcher m = NUMBER_TRIPLET.matcher(range);
        if (m.find()) {
            return new int[]{
                    Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3))
            };
        }
        return new int[]{0, 0, 0};
    }

    public static int compare(int[] a, int[] b) {
        for (int i = 0; i < 3; i++) {
            int diff = Integer.compare(a[i], b[i]);
            if (diff != 0) {
                return diff;
            }
        }
        return 0;
    }

    /**
     * Détermine si une valeur de ServerVersion correspond exactement à l'une des
     * plages enregistrées (comparaison textuelle normalisée : espaces ignorés).
     */
    public static boolean matches(String serverVersionValue, String range) {
        if (serverVersionValue == null || range == null) {
            return false;
        }
        return normalize(serverVersionValue).equals(normalize(range));
    }

    private static String normalize(String s) {
        return s.replaceAll("\\s+", "").trim();
    }
}
