package com.hytale.modmanager.service;

import com.hytale.modmanager.json.JsonParser;
import com.hytale.modmanager.json.JsonWriter;
import com.hytale.modmanager.model.HytaleVersion;
import com.hytale.modmanager.util.ConfigPaths;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gère la liste des versions Hytale connues par le logiciel : chargement et
 * sauvegarde dans un fichier de configuration local, ajout/modification/
 * suppression, définition de la version recommandée, et import/export JSON,
 * conformément à la page "Gestion des versions" du cahier des charges.
 */
public class VersionManager {

    private final ObservableList<HytaleVersion> versions = FXCollections.observableArrayList();
    private final Path configFile;

    public VersionManager() {
        this(ConfigPaths.versionsConfigFile());
    }

    public VersionManager(Path configFile) {
        this.configFile = configFile;
    }

    public ObservableList<HytaleVersion> getVersions() {
        return versions;
    }

    /** Charge la liste depuis le fichier de configuration local, ou crée une liste par défaut si absent. */
    public void load() throws IOException {
        if (Files.exists(configFile)) {
            String json = Files.readString(configFile, StandardCharsets.UTF_8);
            versions.setAll(deserialize(json));
        } else {
            versions.setAll(defaultVersions());
            save();
        }
    }

    /** Sauvegarde la liste actuelle dans le fichier de configuration local. */
    public void save() throws IOException {
        ConfigPaths.ensureExists(configFile.getParent());
        Files.writeString(configFile, serialize(versions), StandardCharsets.UTF_8);
    }

    public void add(HytaleVersion version) throws IOException {
        versions.add(version);
        save();
    }

    public void update(HytaleVersion version) throws IOException {
        // La liste contient déjà la référence à jour (objet mutable) : on persiste simplement.
        save();
    }

    public void remove(HytaleVersion version) throws IOException {
        versions.remove(version);
        save();
    }

    /** Définit la version donnée comme recommandée, et retire ce statut de toutes les autres. */
    public void setRecommended(HytaleVersion version) throws IOException {
        for (HytaleVersion v : versions) {
            v.setRecommended(v.equals(version));
        }
        save();
    }

    public Optional<HytaleVersion> getRecommended() {
        return versions.stream().filter(HytaleVersion::isRecommended).findFirst();
    }

    /** Retourne la version dont la borne inférieure est la plus élevée (la plus "récente"). */
    public Optional<HytaleVersion> getMostRecent() {
        return versions.stream().max(HytaleVersion::compareTo);
    }

    /**
     * Détermine la version à proposer automatiquement pour un mod : la version
     * recommandée si elle est définie, sinon la version la plus récente de la liste.
     */
    public Optional<HytaleVersion> resolveSuggestedVersion() {
        Optional<HytaleVersion> recommended = getRecommended();
        return recommended.isPresent() ? recommended : getMostRecent();
    }

    public void exportToFile(Path destination) throws IOException {
        Files.writeString(destination, serialize(versions), StandardCharsets.UTF_8);
    }

    /** Importe une liste de versions depuis un fichier JSON externe et remplace la liste actuelle. */
    public void importFromFile(Path source) throws IOException {
        String json = Files.readString(source, StandardCharsets.UTF_8);
        versions.setAll(deserialize(json));
        save();
    }

    private List<HytaleVersion> defaultVersions() {
        List<HytaleVersion> list = new ArrayList<>();
        // Version embarquee par defaut : la premiere version Release de Hytale
        // officiellement supportee par le logiciel. Reste la valeur utilisee
        // tant qu'aucune version plus recente n'est detectee (voir
        // HytaleVersionDetector, qui met a jour cette entree automatiquement
        // sans jamais coder en dur de version particuliere).
        list.add(new HytaleVersion(java.util.UUID.randomUUID().toString(), ">=0.5.3 <0.6.0", "0.5.x",
                "Premiere version Release officiellement supportee par le logiciel.", true));
        return list;
    }

    @SuppressWarnings("unchecked")
    private List<HytaleVersion> deserialize(String json) {
        Object root = JsonParser.parse(json);
        List<Object> rawList;
        if (root instanceof List) {
            rawList = (List<Object>) root;
        } else if (root instanceof Map && ((Map<String, Object>) root).get("versions") instanceof List) {
            rawList = (List<Object>) ((Map<String, Object>) root).get("versions");
        } else {
            throw new IllegalArgumentException("Format de fichier de versions invalide : un tableau JSON était attendu.");
        }
        List<HytaleVersion> result = new ArrayList<>();
        for (Object item : rawList) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<String, Object> obj = (Map<String, Object>) item;
            String id = stringOrDefault(obj.get("id"), java.util.UUID.randomUUID().toString());
            String range = stringOrDefault(obj.get("range"), stringOrDefault(obj.get("version"), ""));
            String label = stringOrDefault(obj.get("label"), range);
            String notes = stringOrDefault(obj.get("notes"), "");
            boolean recommended = Boolean.TRUE.equals(obj.get("recommended"));
            if (!range.isBlank()) {
                result.add(new HytaleVersion(id, range, label, notes, recommended));
            }
        }
        return result;
    }

    private String stringOrDefault(Object value, String def) {
        return value == null ? def : String.valueOf(value);
    }

    private String serialize(List<HytaleVersion> list) {
        List<Object> jsonList = new ArrayList<>();
        for (HytaleVersion v : list) {
            Map<String, Object> obj = new LinkedHashMap<>();
            obj.put("id", v.getId());
            obj.put("range", v.getRange());
            obj.put("label", v.getLabel());
            obj.put("notes", v.getNotes());
            obj.put("recommended", v.isRecommended());
            jsonList.add(obj);
        }
        return new JsonWriter("  ").write(jsonList);
    }
}
