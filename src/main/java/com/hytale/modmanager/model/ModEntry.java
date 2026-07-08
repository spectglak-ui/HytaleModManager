package com.hytale.modmanager.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.nio.file.Path;

/**
 * Représente un mod Hytale importé dans le logiciel : son emplacement sur le
 * disque (dossier extrait ou dossier d'origine), les informations lues depuis
 * son manifest.json, ainsi que la version cible choisie pour la mise à jour.
 *
 * Les propriétés JavaFX permettent un affichage et une édition directe dans le
 * {@code TableView} de la fenêtre principale.
 */
public class ModEntry {

    /** Dossier contenant le manifest.json (dossier d'origine ou extrait d'une archive). */
    private final Path modFolder;
    /** Chemin exact du fichier manifest.json détecté. */
    private final Path manifestPath;
    /** Indique si le mod provient d'une archive temporaire à nettoyer après usage. */
    private final boolean fromTemporaryExtraction;
    /** Chemin d'origine choisi par l'utilisateur lors de l'import (dossier, .zip ou .rar). */
    private final Path originalSource;

    private final StringProperty name = new SimpleStringProperty("?");
    private final StringProperty author = new SimpleStringProperty("?");
    private final StringProperty modVersion = new SimpleStringProperty("?");
    private final StringProperty currentServerVersion = new SimpleStringProperty("");
    private final ObjectProperty<HytaleVersion> targetVersion = new SimpleObjectProperty<>();
    private final ObjectProperty<UpdateStatus> status = new SimpleObjectProperty<>(UpdateStatus.EN_ATTENTE);
    private final StringProperty message = new SimpleStringProperty("");
    private final StringProperty exportInfo = new SimpleStringProperty("Non exporté");

    public ModEntry(Path modFolder, Path manifestPath, boolean fromTemporaryExtraction, Path originalSource) {
        this.modFolder = modFolder;
        this.manifestPath = manifestPath;
        this.fromTemporaryExtraction = fromTemporaryExtraction;
        this.originalSource = originalSource;
    }

    public Path getOriginalSource() {
        return originalSource;
    }

    public Path getModFolder() {
        return modFolder;
    }

    public Path getManifestPath() {
        return manifestPath;
    }

    public boolean isFromTemporaryExtraction() {
        return fromTemporaryExtraction;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String value) {
        name.set(value);
    }

    public StringProperty authorProperty() {
        return author;
    }

    public String getAuthor() {
        return author.get();
    }

    public void setAuthor(String value) {
        author.set(value);
    }

    public StringProperty modVersionProperty() {
        return modVersion;
    }

    public String getModVersion() {
        return modVersion.get();
    }

    public void setModVersion(String value) {
        modVersion.set(value);
    }

    public StringProperty currentServerVersionProperty() {
        return currentServerVersion;
    }

    public String getCurrentServerVersion() {
        return currentServerVersion.get();
    }

    public void setCurrentServerVersion(String value) {
        currentServerVersion.set(value == null ? "" : value);
    }

    public ObjectProperty<HytaleVersion> targetVersionProperty() {
        return targetVersion;
    }

    public HytaleVersion getTargetVersion() {
        return targetVersion.get();
    }

    public void setTargetVersion(HytaleVersion value) {
        targetVersion.set(value);
    }

    public ObjectProperty<UpdateStatus> statusProperty() {
        return status;
    }

    public UpdateStatus getStatus() {
        return status.get();
    }

    public void setStatus(UpdateStatus value) {
        status.set(value);
    }

    public StringProperty messageProperty() {
        return message;
    }

    public String getMessage() {
        return message.get();
    }

    public void setMessage(String value) {
        message.set(value == null ? "" : value);
    }

    public StringProperty exportInfoProperty() {
        return exportInfo;
    }

    public String getExportInfo() {
        return exportInfo.get();
    }

    public void setExportInfo(String value) {
        exportInfo.set(value == null ? "" : value);
    }
}
