package com.hytale.modmanager.service;

import com.hytale.modmanager.util.ConfigPaths;
import com.hytale.modmanager.util.I18n;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parametres persistants de l'application sauvegardes dans settings.json :
 *   - langue de l'interface
 *   - dossier de mods personnalise (null = chemin Hytale par defaut)
 */
public class AppSettings {

    private static final Logger LOG = Logger.getLogger(AppSettings.class.getName());
    private static AppSettings instance;

    private final Path settingsFile;
    private String languageTag     = null;
    private String modsFolderPath  = null;

    private AppSettings(Path settingsFile) {
        this.settingsFile = settingsFile;
    }

    public static synchronized AppSettings get() {
        if (instance == null) {
            instance = new AppSettings(ConfigPaths.appSettingsFile());
            instance.load();
        }
        return instance;
    }

    // =========================================================================
    // Langue
    // =========================================================================

    public Locale getLocale() {
        if (languageTag == null || languageTag.isBlank()) {
            return I18n.detectSystemLocale();
        }
        try {
            return Locale.forLanguageTag(languageTag);
        } catch (Exception ex) {
            return I18n.detectSystemLocale();
        }
    }

    public void setLocale(Locale locale) {
        this.languageTag = locale.toLanguageTag();
        I18n.setLocale(locale);
        save();
    }

    // =========================================================================
    // Dossier de mods
    // =========================================================================

    /**
     * Retourne le chemin personnalise du dossier de mods, ou {@code null}
     * si l'utilisateur n'en a pas defini (utiliser le chemin Hytale par defaut).
     */
    public Path getCustomModsFolder() {
        if (modsFolderPath == null || modsFolderPath.isBlank()) {
            return null;
        }
        try {
            return Path.of(modsFolderPath);
        } catch (Exception ex) {
            return null;
        }
    }

    /** Memorise un dossier de mods personnalise et sauvegarde immediatement. */
    public void setModsFolder(Path folder) {
        this.modsFolderPath = (folder != null) ? folder.toAbsolutePath().toString() : null;
        save();
    }

    /** Reinitialise le choix vers le chemin Hytale par defaut. */
    public void clearModsFolder() {
        this.modsFolderPath = null;
        save();
    }

    // =========================================================================
    // Persistance
    // =========================================================================

    @SuppressWarnings("unchecked")
    private void load() {
        if (!Files.exists(settingsFile)) {
            Locale detected = I18n.detectSystemLocale();
            languageTag = detected.toLanguageTag();
            I18n.setLocale(detected);
            save();
            return;
        }
        try {
            String json = Files.readString(settingsFile, StandardCharsets.UTF_8);
            Object parsed = com.hytale.modmanager.json.JsonParser.parse(json);
            if (parsed instanceof Map<?, ?> map) {
                Object lang = map.get("language");
                if (lang instanceof String s && !s.isBlank()) {
                    languageTag = s;
                }
                Object mods = map.get("modsFolder");
                if (mods instanceof String s && !s.isBlank()) {
                    modsFolderPath = s;
                }
            }
            I18n.setLocale(getLocale());
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to load settings, using defaults.", ex);
            I18n.setLocale(I18n.detectSystemLocale());
        }
    }

    private void save() {
        try {
            ConfigPaths.ensureExists(settingsFile.getParent());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("language",   languageTag   != null ? languageTag   : "");
            data.put("modsFolder", modsFolderPath != null ? modsFolderPath : "");
            String json = new com.hytale.modmanager.json.JsonWriter("  ").write(data);
            Files.writeString(settingsFile, json, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Failed to save settings.", ex);
        }
    }
}
