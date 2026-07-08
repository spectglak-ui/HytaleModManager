package com.hytale.modmanager.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Détecte l'unité d'indentation (espaces ou tabulation) utilisée dans un
 * fichier JSON existant, afin que toute ré-écriture structurelle (ajout d'une
 * clé absente) respecte le style déjà en place dans le manifest.json du mod.
 */
public final class IndentDetector {

    private static final Pattern FIRST_INDENTED_LINE = Pattern.compile("\\n([ \\t]+)\\S");

    private IndentDetector() {
    }

    /** Retourne l'unité d'indentation détectée, ou "  " (2 espaces) par défaut. */
    public static String detect(String jsonText) {
        if (jsonText == null) {
            return "  ";
        }
        Matcher m = FIRST_INDENTED_LINE.matcher(jsonText);
        if (m.find()) {
            String indent = m.group(1);
            if (indent.startsWith("\t")) {
                return "\t";
            }
            return indent;
        }
        return "  ";
    }

    /** Détecte le saut de ligne utilisé (CRLF ou LF) afin de le préserver lors d'une réécriture. */
    public static String detectLineEnding(String text) {
        if (text != null && text.contains("\r\n")) {
            return "\r\n";
        }
        return "\n";
    }
}
