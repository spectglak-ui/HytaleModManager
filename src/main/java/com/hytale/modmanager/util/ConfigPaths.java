package com.hytale.modmanager.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Résout l'emplacement du dossier de configuration de l'application, de manière
 * portable entre Windows, macOS et Linux.
 *
 * Sur Windows : {@code %APPDATA%\HytaleModManager}
 * Sur macOS   : {@code ~/Library/Application Support/HytaleModManager}
 * Sur Linux   : {@code ~/.config/HytaleModManager}
 */
public final class ConfigPaths {

    private static final String APP_FOLDER_NAME = "HytaleModManager";

    private ConfigPaths() {
    }

    public static Path appDataDir() {
        String os = System.getProperty("os.name", "").toLowerCase();
        Path base;
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            base = (appData != null && !appData.isBlank())
                    ? Paths.get(appData)
                    : Paths.get(System.getProperty("user.home"), "AppData", "Roaming");
        } else if (os.contains("mac")) {
            base = Paths.get(System.getProperty("user.home"), "Library", "Application Support");
        } else {
            base = Paths.get(System.getProperty("user.home"), ".config");
        }
        return base.resolve(APP_FOLDER_NAME);
    }

    public static Path versionsConfigFile() {
        return appDataDir().resolve("versions.json");
    }

    public static Path appSettingsFile() {
        return appDataDir().resolve("settings.json");
    }

    public static Path logFile() {
        return appDataDir().resolve("journal.log");
    }

    public static Path extractionTempDir() {
        return appDataDir().resolve("temp_extraction");
    }

    public static void ensureExists(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }
}
