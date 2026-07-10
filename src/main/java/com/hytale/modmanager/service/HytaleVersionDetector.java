package com.hytale.modmanager.service;

import com.hytale.modmanager.model.HytaleVersion;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Détecte la version Release de Hytale réellement installée sur la machine,
 * en lisant le journal (log) le plus récent produit par le jeu, plutôt que
 * de dépendre d'une source distante ou d'une valeur codée en dur.
 * <p>
 * Localise {@code %APPDATA%\Hytale\UserData\Logs} (mêmes principes de
 * résolution que {@link ModsFolderService} pour le dossier des mods), lit
 * uniquement le début du fichier {@code .log} le plus récent, et y
 * recherche une ligne contenant {@code HytaleClient vX.Y.Z}.
 * <p>
 * Ne lève jamais d'exception pour la partie détection : dossier absent,
 * aucun fichier {@code .log}, fichier illisible, ou format inattendu se
 * traduisent tous simplement par {@link Optional#empty()} — la version
 * actuellement recommandée dans le Gestionnaire de versions est alors
 * conservée telle quelle par l'appelant. Seule la sauvegarde finale (si une
 * mise à jour doit être appliquée) peut lever {@link IOException}, de la
 * même façon que les autres méthodes de {@link VersionManager}.
 */
public class HytaleVersionDetector {

    private static final Pattern VERSION_LINE_PATTERN =
            Pattern.compile("HytaleClient v([0-9]+\\.[0-9]+\\.[0-9]+)");

    /** On ne lit que le début du fichier : la ligne de version apparaît dès le démarrage du jeu. */
    private static final int MAX_LINES_TO_SCAN = 200;

    /**
     * Détecte la version installée puis, si elle diffère de la version
     * recommandée actuellement enregistrée, met à jour <strong>uniquement</strong>
     * l'entrée recommandée du Gestionnaire de versions — les autres entrées
     * du tableau ne sont jamais modifiées. Si aucune version n'est détectée
     * (Hytale non installé, journal introuvable ou invalide), la version
     * actuellement enregistrée est conservée telle quelle.
     *
     * @return la nouvelle version détectée si une mise à jour a été
     *         appliquée ; vide si rien n'a été détecté ou si la valeur
     *         recommandée était déjà à jour.
     * @throws IOException uniquement si l'écriture du fichier de
     *         configuration des versions échoue (disque plein, permissions...).
     */
    public Optional<String> refreshRecommendedVersion(VersionManager versionManager) throws IOException {
        Optional<String> detected = detectInstalledVersion();
        if (detected.isEmpty()) {
            return Optional.empty();
        }
        String detectedVersion = detected.get();

        Optional<String> newRange = RecommendedRangeCalculator.computeRange(detectedVersion);
        if (newRange.isEmpty()) {
            return Optional.empty();
        }

        Optional<HytaleVersion> currentRecommended = versionManager.getRecommended();
        if (currentRecommended.isPresent() && newRange.get().equals(currentRecommended.get().getRange())) {
            return Optional.empty();
        }

        String label = RecommendedRangeCalculator.computeLabel(detectedVersion).orElse(detectedVersion);
        String note = "Detectee automatiquement depuis le journal de Hytale installe (v" + detectedVersion + ").";

        if (currentRecommended.isPresent()) {
            HytaleVersion v = currentRecommended.get();
            v.setRange(newRange.get());
            v.setLabel(label);
            v.setNotes(note);
            versionManager.update(v);
        } else {
            HytaleVersion created = new HytaleVersion(
                    UUID.randomUUID().toString(), newRange.get(), label, note, true);
            versionManager.add(created);
            versionManager.setRecommended(created);
        }
        return Optional.of(detectedVersion);
    }

    /**
     * @return la version détectée (ex. "0.5.6"), ou vide si Hytale n'est pas
     *         installé, si aucun journal n'est trouvé, ou si son contenu ne
     *         correspond pas au format attendu.
     */
    public Optional<String> detectInstalledVersion() {
        Path logsDir = resolveLogsFolder();
        if (logsDir == null || !Files.isDirectory(logsDir)) {
            return Optional.empty();
        }
        Optional<Path> latestLog = findMostRecentLogFile(logsDir);
        if (latestLog.isEmpty()) {
            return Optional.empty();
        }
        return extractVersionFromLog(latestLog.get());
    }

    /** Resout {@code %APPDATA%\Hytale\UserData\Logs} (repli sur {@code user.home} si APPDATA est absent, comme {@link ModsFolderService}). */
    private Path resolveLogsFolder() {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isBlank()) {
            appData = System.getProperty("user.home");
        }
        if (appData == null) {
            return null;
        }
        return Path.of(appData, "Hytale", "UserData", "Logs");
    }

    private Optional<Path> findMostRecentLogFile(Path logsDir) {
        try (var stream = Files.list(logsDir)) {
            return stream
                    .filter(p -> !Files.isDirectory(p))
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".log"))
                    .max(Comparator.comparingLong(this::lastModifiedSafely));
        } catch (IOException | SecurityException ex) {
            return Optional.empty();
        }
    }

    private long lastModifiedSafely(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ex) {
            return 0L;
        }
    }

    /** Ne lit que les {@link #MAX_LINES_TO_SCAN} premières lignes, jamais le fichier entier. */
    private Optional<String> extractVersionFromLog(Path logFile) {
        try (BufferedReader reader = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
            String line;
            int count = 0;
            while (count < MAX_LINES_TO_SCAN && (line = reader.readLine()) != null) {
                Matcher matcher = VERSION_LINE_PATTERN.matcher(line);
                if (matcher.find()) {
                    return Optional.of(matcher.group(1));
                }
                count++;
            }
        } catch (IOException | SecurityException ex) {
            return Optional.empty();
        }
        return Optional.empty();
    }
}
