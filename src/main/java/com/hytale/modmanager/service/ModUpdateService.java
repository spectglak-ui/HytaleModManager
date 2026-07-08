package com.hytale.modmanager.service;

import com.hytale.modmanager.model.HytaleVersion;
import com.hytale.modmanager.model.ModEntry;
import com.hytale.modmanager.model.UpdateReport;
import com.hytale.modmanager.model.UpdateStatus;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Orchestre le chargement des mods et la mise à jour de leur champ ServerVersion,
 * que ce soit pour un mod isolé ("Mettre à jour") ou pour plusieurs mods à la fois
 * ("Mettre à jour tous les mods"), et construit le rapport correspondant.
 */
public class ModUpdateService {

    private final ManifestService manifestService = new ManifestService();
    private final ManifestUpdater manifestUpdater = new ManifestUpdater();
    private final ArchiveExtractor archiveExtractor = new ArchiveExtractor();

    /**
     * Importe un mod depuis un dossier ou une archive (ZIP/RAR), détecte son
     * manifest.json et lit ses informations. Lève une exception explicite si
     * aucun manifest.json n'est trouvé.
     */
    public ModEntry importMod(Path source, VersionManager versionManager) throws IOException, ArchiveExtractor.ExtractionException {
        Path modFolder = archiveExtractor.prepareModFolder(source);
        boolean temporary = !modFolder.equals(source);

        Optional<Path> manifestPath = manifestService.findManifest(modFolder);
        if (manifestPath.isEmpty()) {
            if (temporary) {
                archiveExtractor.cleanupQuietly(modFolder);
            }
            throw new IOException("Aucun fichier manifest.json détecté dans : " + source.getFileName());
        }

        ModEntry entry = new ModEntry(modFolder, manifestPath.get(), temporary, source);
        refreshFromManifest(entry);

        // Pré-sélectionne automatiquement une version cible (recommandée, sinon la plus récente).
        versionManager.resolveSuggestedVersion().ifPresent(entry::setTargetVersion);

        return entry;
    }

    /** Relit le manifest.json d'un mod déjà importé et met à jour les champs affichés. */
    public void refreshFromManifest(ModEntry entry) throws IOException {
        String raw = manifestService.readRaw(entry.getManifestPath());
        ManifestService.ManifestInfo info = manifestService.parse(raw);
        entry.setName(info.name);
        entry.setAuthor(info.author);
        entry.setModVersion(info.version);
        entry.setCurrentServerVersion(info.serverVersion);
    }

    /** Met à jour un mod unique vers sa version cible actuellement sélectionnée. */
    public UpdateReport.Entry updateSingle(ModEntry entry) {
        HytaleVersion target = entry.getTargetVersion();
        if (target == null) {
            entry.setStatus(UpdateStatus.ERREUR);
            entry.setMessage("Aucune version cible sélectionnée.");
            return new UpdateReport.Entry(entry.getName(), UpdateStatus.ERREUR, "Aucune version cible sélectionnée.");
        }
        try {
            ManifestUpdateResult result = manifestUpdater.updateServerVersion(entry.getManifestPath(), target.getRange());
            entry.setStatus(result.getStatus());
            entry.setMessage(result.getMessage());
            entry.setCurrentServerVersion(result.getNewValue());
            if (result.getStatus() == UpdateStatus.MODIFIE || result.getStatus() == UpdateStatus.CREE) {
                if (entry.isFromTemporaryExtraction()) {
                    entry.setExportInfo(com.hytale.modmanager.util.I18n.t("export.warning"));
                } else {
                    entry.setExportInfo(com.hytale.modmanager.util.I18n.t("export.saved_in_place"));
                }
            }
            return new UpdateReport.Entry(entry.getName(), result.getStatus(), result.getMessage());
        } catch (Exception ex) {
            entry.setStatus(UpdateStatus.ERREUR);
            entry.setMessage(ex.getMessage());
            return new UpdateReport.Entry(entry.getName(), UpdateStatus.ERREUR, ex.getMessage());
        }
    }

    /**
     * Réempaquette le mod (modifié ou non) vers le fichier ZIP de destination indiqué.
     * C'est l'étape « Sauvegarde du mod mis à jour » : pour un mod importé depuis une
     * archive, c'est l'unique moyen de récupérer les modifications hors du dossier
     * d'extraction temporaire interne à l'application.
     */
    public void exportMod(ModEntry entry, Path destinationZip) throws IOException {
        archiveExtractor.exportAsZip(entry.getModFolder(), destinationZip);
        entry.setExportInfo(com.hytale.modmanager.util.I18n.t("export.done", destinationZip.getFileName()));
    }

    /** Suggère un nom de fichier de destination pour l'export, basé sur la source d'origine du mod. */
    public String suggestExportFileName(ModEntry entry) {
        Path source = entry.getOriginalSource();
        if (source == null) {
            return entry.getName() + "_updated.zip";
        }
        String original = source.getFileName().toString();
        // Conserver l'extension d'origine (.zip ou .jar) ; les archives .rar sont
        // ré-exportées en .zip faute de bibliothèque d'écriture RAR en Java.
        if (original.toLowerCase().endsWith(".jar")) {
            return original.replaceAll("(?i)\\.jar$", "_updated.jar");
        }
        return original.replaceAll("(?i)\\.(zip|rar)$", "_updated.zip");
    }

    /** Met à jour une liste de mods en lot, chacun vers sa propre version cible sélectionnée, et construit le rapport. */
    public UpdateReport updateAll(List<ModEntry> entries) {
        UpdateReport report = new UpdateReport();
        for (ModEntry entry : entries) {
            UpdateReport.Entry result = updateSingle(entry);
            report.add(result.modName, result.status, result.message);
        }
        return report;
    }

    /** Nettoie le dossier temporaire d'extraction associé à un mod, s'il en provient. */
    public void cleanupIfTemporary(ModEntry entry) {
        if (entry.isFromTemporaryExtraction()) {
            archiveExtractor.cleanupQuietly(entry.getModFolder());
        }
    }
}
