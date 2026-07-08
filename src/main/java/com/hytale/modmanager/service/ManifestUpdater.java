package com.hytale.modmanager.service;

import com.hytale.modmanager.json.JsonParser;
import com.hytale.modmanager.json.JsonWriter;
import com.hytale.modmanager.model.UpdateStatus;
import com.hytale.modmanager.util.IndentDetector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implémente les règles de mise à jour automatique du champ "ServerVersion"
 * décrites dans le cahier des charges :
 *
 * <ol>
 *   <li>Le champ existe avec une valeur : seul le contenu entre guillemets est remplacé,
 *       le reste du fichier JSON est conservé strictement à l'identique.</li>
 *   <li>Le champ existe mais sa valeur est vide : il est rempli avec la nouvelle valeur.</li>
 *   <li>Le champ n'existe pas : il est créé à un emplacement logique (juste après le
 *       champ "Version"), avec la valeur sélectionnée par l'utilisateur.</li>
 * </ol>
 *
 * Stratégie d'édition : lorsque la clé "ServerVersion" peut être localisée de façon
 * non ambiguë dans le texte brut (un seul site de correspondance), une simple
 * substitution textuelle est appliquée afin de préserver le fichier à l'octet près
 * en dehors de la valeur modifiée. En cas d'ambiguïté (plusieurs occurrences,
 * généralement dues à un objet imbriqué portant la même clé) ou lorsqu'il faut
 * insérer une clé absente, le document est reconstruit depuis son arbre JSON via
 * {@link JsonWriter}, en respectant l'indentation détectée dans le fichier d'origine.
 */
public class ManifestUpdater {

    private static final Pattern SERVER_VERSION_FIELD =
            Pattern.compile("(\"ServerVersion\"\\s*:\\s*\")((?:[^\"\\\\]|\\\\.)*)(\")");

    public ManifestUpdateResult updateServerVersion(Path manifestPath, String newValue) throws IOException {
        String rawText = Files.readString(manifestPath, StandardCharsets.UTF_8);
        String lineEnding = IndentDetector.detectLineEnding(rawText);

        Matcher matcher = SERVER_VERSION_FIELD.matcher(rawText);
        int matchCount = 0;
        String firstPreviousValue = null;
        while (matcher.find()) {
            matchCount++;
            if (matchCount == 1) {
                firstPreviousValue = matcher.group(2);
            }
        }

        if (matchCount == 1) {
            // Cas 1 et 2 : substitution textuelle ciblée, reste du fichier inchangé.
            String escapedNewValue = escapeJsonString(newValue);
            Matcher single = SERVER_VERSION_FIELD.matcher(rawText);
            // $1 et $3 sont les groupes de capture (clé + guillemet ouvrant / guillemet fermant) ;
            // seule la valeur insérée doit être échappée pour le moteur de regex (Matcher.quoteReplacement),
            // les références de groupe ne doivent surtout pas l'être.
            String replacement = "$1" + Matcher.quoteReplacement(escapedNewValue) + "$3";
            String updatedText = single.replaceFirst(replacement);
            writeIfChanged(manifestPath, rawText, updatedText);

            boolean wasEmpty = firstPreviousValue == null || firstPreviousValue.isEmpty();
            if (firstPreviousValue != null && firstPreviousValue.equals(newValue)) {
                return new ManifestUpdateResult(UpdateStatus.DEJA_A_JOUR,
                        "Le champ ServerVersion est déjà à la valeur souhaitée.", firstPreviousValue, newValue);
            }
            String msg = wasEmpty
                    ? "Champ ServerVersion vide rempli avec la nouvelle valeur."
                    : "Valeur de ServerVersion remplacée (" + firstPreviousValue + " → " + newValue + ").";
            return new ManifestUpdateResult(UpdateStatus.MODIFIE, msg, firstPreviousValue, newValue);
        }

        // Reconstruction via l'arbre JSON : soit la clé est absente (cas 3), soit
        // elle apparaît plusieurs fois et une édition textuelle serait ambiguë.
        Map<String, Object> root = JsonParser.parseObject(rawText);
        boolean keyExistsAtRoot = root.containsKey("ServerVersion");

        if (keyExistsAtRoot) {
            Object current = root.get("ServerVersion");
            String currentStr = current == null ? "" : String.valueOf(current);
            if (newValue.equals(currentStr)) {
                return new ManifestUpdateResult(UpdateStatus.DEJA_A_JOUR,
                        "Le champ ServerVersion est déjà à la valeur souhaitée.", currentStr, newValue);
            }
            root.put("ServerVersion", newValue);
            String indent = IndentDetector.detect(rawText);
            String updatedText = new JsonWriter(indent).write(root).replace("\n", lineEnding);
            writeIfChanged(manifestPath, rawText, updatedText);
            String msg = currentStr.isEmpty()
                    ? "Champ ServerVersion vide rempli avec la nouvelle valeur (structure reconstruite)."
                    : "Valeur de ServerVersion remplacée (" + currentStr + " → " + newValue + "), structure reconstruite.";
            return new ManifestUpdateResult(UpdateStatus.MODIFIE, msg, currentStr, newValue);
        } else {
            Map<String, Object> rebuilt = insertServerVersionAtLogicalPosition(root, newValue);
            String indent = IndentDetector.detect(rawText);
            String updatedText = new JsonWriter(indent).write(rebuilt).replace("\n", lineEnding);
            writeIfChanged(manifestPath, rawText, updatedText);
            return new ManifestUpdateResult(UpdateStatus.CREE,
                    "Champ ServerVersion absent : créé avec la valeur \"" + newValue + "\".", null, newValue);
        }
    }

    /**
     * Reconstruit la map en insérant "ServerVersion" juste après le champ "Version"
     * (ou "ModVersion"/"Name" à défaut), afin de le placer parmi les autres
     * informations de compatibilité du mod, comme demandé par le cahier des charges.
     */
    private Map<String, Object> insertServerVersionAtLogicalPosition(Map<String, Object> original, String newValue) {
        Map<String, Object> result = new LinkedHashMap<>();
        String anchorKey = findAnchorKey(original);
        boolean inserted = false;
        for (Map.Entry<String, Object> entry : original.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
            if (!inserted && entry.getKey().equals(anchorKey)) {
                result.put("ServerVersion", newValue);
                inserted = true;
            }
        }
        if (!inserted) {
            result.put("ServerVersion", newValue);
        }
        return result;
    }

    private String findAnchorKey(Map<String, Object> map) {
        for (String candidate : new String[]{"Version", "ModVersion", "Name", "ModName"}) {
            for (String key : map.keySet()) {
                if (key.equalsIgnoreCase(candidate)) {
                    return key;
                }
            }
        }
        return null;
    }

    private void writeIfChanged(Path manifestPath, String oldText, String newText) throws IOException {
        if (!oldText.equals(newText)) {
            Files.writeString(manifestPath, newText, StandardCharsets.UTF_8);
        }
    }

    private String escapeJsonString(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
