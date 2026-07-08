package com.hytale.modmanager.service;

import com.hytale.modmanager.json.JsonParseException;
import com.hytale.modmanager.json.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Service responsable de la détection automatique du fichier manifest.json
 * d'un mod, et de la lecture de ses informations principales.
 */
public class ManifestService {

    public static final String MANIFEST_FILE_NAME = "manifest.json";
    private static final int MAX_SEARCH_DEPTH = 4;

    /**
     * Recherche le fichier manifest.json à l'intérieur d'un dossier de mod,
     * en explorant les sous-dossiers sur une profondeur limitée (certains mods
     * peuvent être imbriqués dans un sous-dossier portant le nom du mod après
     * extraction d'une archive).
     */
    public Optional<Path> findManifest(Path modFolder) throws IOException {
        Path direct = modFolder.resolve(MANIFEST_FILE_NAME);
        if (Files.isRegularFile(direct)) {
            return Optional.of(direct);
        }
        try (Stream<Path> walk = Files.walk(modFolder, MAX_SEARCH_DEPTH)) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase(MANIFEST_FILE_NAME))
                    .findFirst();
        }
    }

    /** Lit le contenu brut (texte) du manifest, nécessaire pour les éditions structurelles fines. */
    public String readRaw(Path manifestPath) throws IOException {
        return Files.readString(manifestPath, StandardCharsets.UTF_8);
    }

    /** Résultat de lecture d'un manifest, prêt à être affiché dans le tableau des mods. */
    public static class ManifestInfo {
        public final String name;
        public final String author;
        public final String version;
        public final String serverVersion;
        public final boolean serverVersionPresent;

        public ManifestInfo(String name, String author, String version, String serverVersion, boolean serverVersionPresent) {
            this.name = name;
            this.author = author;
            this.version = version;
            this.serverVersion = serverVersion;
            this.serverVersionPresent = serverVersionPresent;
        }
    }

    /**
     * Analyse le contenu JSON d'un manifest et en extrait les informations
     * affichées dans le tableau principal. Les clés sont recherchées de façon
     * insensible à la casse pour plus de tolérance entre mods.
     */
    public ManifestInfo parse(String rawJson) {
        Map<String, Object> root;
        try {
            root = JsonParser.parseObject(rawJson);
        } catch (JsonParseException ex) {
            throw new ManifestException("Fichier manifest.json invalide : " + ex.getMessage(), ex);
        }
        String name = stringField(root, "Name", "ModName").orElse("(nom inconnu)");
        String author = stringField(root, "Author", "Authors", "Creator").orElse("(auteur inconnu)");
        String version = stringField(root, "Version", "ModVersion").orElse("(version inconnue)");
        boolean present = root.containsKey("ServerVersion");
        String serverVersion = present ? String.valueOf(root.get("ServerVersion")) : "";
        if (present && root.get("ServerVersion") == null) {
            serverVersion = "";
        }
        return new ManifestInfo(name, author, version, serverVersion, present);
    }

    @SuppressWarnings("unchecked")
    private Optional<String> stringField(Map<String, Object> root, String... candidateKeys) {
        for (String key : candidateKeys) {
            for (Map.Entry<String, Object> entry : root.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        return Optional.of((String) value);
                    } else if (value instanceof java.util.List) {
                        java.util.List<Object> list = (java.util.List<Object>) value;
                        if (!list.isEmpty()) {
                            return Optional.of(String.valueOf(list.get(0)));
                        }
                    } else if (value != null) {
                        return Optional.of(String.valueOf(value));
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static class ManifestException extends RuntimeException {
        public ManifestException(String message, Throwable cause) {
            super(message, cause);
        }

        public ManifestException(String message) {
            super(message);
        }
    }
}
