package com.hytale.modmanager.service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gere le dossier des mods Hytale :
 *   - Detection du chemin par defaut : %APPDATA%\Hytale\UserData\Mods
 *   - Chemin personnalise memorise dans settings.json
 *   - Scan des archives et sous-dossiers de mods
 *   - Surveillance en temps reel via WatchService (thread daemon)
 *
 * Le chemin est toujours resolu dynamiquement depuis la variable
 * d'environnement APPDATA, sans jamais coder en dur un nom d'utilisateur.
 */
public class ModsFolderService {

    private static final Logger LOG = Logger.getLogger(ModsFolderService.class.getName());

    /** Chemin relatif au dossier APPDATA. */
    private static final String RELATIVE_MODS_PATH = "Hytale/UserData/Mods";

    private Path customModsFolder = null;
    private Thread watchThread    = null;
    private WatchService watchService = null;

    // =========================================================================
    // Dossier courant
    // =========================================================================

    /**
     * Retourne le dossier de mods actif :
     * le chemin personnalise s'il est defini, sinon le chemin Hytale par defaut.
     */
    public Path getModsFolder() {
        return customModsFolder != null ? customModsFolder : getDefaultModsFolder();
    }

    /**
     * Chemin par defaut : %APPDATA%\Hytale\UserData\Mods.
     * APPDATA est toujours lu depuis la variable d'environnement Windows,
     * pas depuis le profil utilisateur Java (user.home peut etre different).
     */
    public Path getDefaultModsFolder() {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isBlank()) {
            appData = System.getProperty("user.home");
        }
        return Path.of(appData, "Hytale", "UserData", "Mods");
    }

    public void setCustomModsFolder(Path path) {
        this.customModsFolder = path;
    }

    public boolean isUsingCustomFolder() {
        return customModsFolder != null;
    }

    // =========================================================================
    // Scan
    // =========================================================================

    /**
     * Scanne le dossier courant et retourne une liste de chemins (dossiers de
     * mods et archives) prets a etre importes via ModUpdateService.importMod().
     *
     * Sont consideres comme des mods :
     *   - Chaque sous-dossier immediat (un dossier = un mod)
     *   - Chaque archive .zip, .jar ou .rar a la racine du dossier
     *
     * Le scan n'est pas recursif (les sous-sous-dossiers sont traites
     * par ManifestService.findManifest, qui descend jusqu'a 4 niveaux).
     *
     * @throws IOException si le dossier est inaccessible
     */
    public List<Path> scanForMods() throws IOException {
        Path folder = getModsFolder();

        if (folder == null || !Files.exists(folder)) {
            return Collections.emptyList();
        }
        if (!Files.isDirectory(folder)) {
            return Collections.emptyList();
        }

        List<Path> found = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path entry : stream) {
                try {
                    if (Files.isDirectory(entry)) {
                        // Sous-dossier = potentiel mod
                        found.add(entry);
                    } else {
                        String name = entry.getFileName().toString().toLowerCase();
                        if (name.endsWith(".zip") || name.endsWith(".jar") || name.endsWith(".rar")) {
                            found.add(entry);
                        }
                    }
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "Impossible de lire l'entree : " + entry, ex);
                }
            }
        }

        Collections.sort(found, (a, b) -> a.getFileName().toString()
                .compareToIgnoreCase(b.getFileName().toString()));
        return found;
    }

    // =========================================================================
    // Surveillance WatchService
    // =========================================================================

    /**
     * Demarre la surveillance du dossier de mods.
     * Appelle {@code onChanged} (sur le thread watcher, pas le thread FX)
     * a chaque modification detectee. Le controleur est responsable
     * d'appeler Platform.runLater() si necessaire.
     *
     * Appels consecutifs : l'ancien watcher est arrete avant d'en demarrer
     * un nouveau.
     *
     * @param onChanged callback appele lors d'une modification (peut etre null)
     */
    public void startWatching(Runnable onChanged) {
        stopWatching();

        Path folder = getModsFolder();
        if (folder == null || !Files.isDirectory(folder)) {
            LOG.info("WatchService non demarre : dossier inaccessible ou absent.");
            return;
        }

        watchThread = new Thread(() -> {
            try {
                watchService = FileSystems.getDefault().newWatchService();
                folder.register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY);

                while (!Thread.currentThread().isInterrupted()) {
                    WatchKey key = watchService.poll(2, TimeUnit.SECONDS);
                    if (key == null) {
                        continue;
                    }

                    boolean relevant = false;
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() != StandardWatchEventKinds.OVERFLOW) {
                            relevant = true;
                        }
                    }

                    if (!key.reset()) {
                        break; // dossier surveille supprime ou inaccessible
                    }

                    if (relevant && onChanged != null) {
                        try {
                            // Debounce : attendre un peu avant de notifier
                            // pour eviter les rafales d'evenements
                            Thread.sleep(800);
                            onChanged.run();
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            } catch (ClosedWatchServiceException ignored) {
                // arret normal via stopWatching()
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (IOException ex) {
                LOG.log(Level.WARNING, "Erreur WatchService : " + ex.getMessage(), ex);
            } finally {
                closeWatchServiceQuietly();
            }
        }, "mods-folder-watcher");

        watchThread.setDaemon(true);
        watchThread.start();
    }

    /** Arrete la surveillance du dossier. Appel silencieux si non demarre. */
    public void stopWatching() {
        if (watchThread != null) {
            watchThread.interrupt();
            watchThread = null;
        }
        closeWatchServiceQuietly();
    }

    public boolean isWatching() {
        return watchThread != null && watchThread.isAlive();
    }

    private void closeWatchServiceQuietly() {
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {
            }
            watchService = null;
        }
    }
}
