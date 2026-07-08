package com.hytale.modmanager.util;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Moteur de localisation singleton de l'application.
 *
 * <h3>Utilisation</h3>
 * <pre>{@code
 *   I18n.setLocale(Locale.ENGLISH);          // changer de langue (sans redémarrage)
 *   String s = I18n.t("menu.file");           // clé simple
 *   String s = I18n.t("app.loaded", 5);       // clé avec arguments MessageFormat
 * }</pre>
 *
 * <h3>Ajouter une langue</h3>
 * <ol>
 *   <li>Créer {@code messages_xx.properties} dans le dossier {@code i18n/}.</li>
 *   <li>Ajouter le {@link Locale} correspondant dans {@link #SUPPORTED_LOCALES}.</li>
 * </ol>
 * Aucune autre modification de code n'est nécessaire.
 */
public final class I18n {

    private static final Logger LOG = Logger.getLogger(I18n.class.getName());
    private static final String BASE_PATH = "/com/hytale/modmanager/i18n/messages_";

    /** Langues supportées — ajouter ici pour étendre (es, de, it…). */
    public static final List<Locale> SUPPORTED_LOCALES = List.of(
            Locale.FRENCH,   // messages_fr.properties
            Locale.ENGLISH   // messages_en.properties
    );

    /** Propriété observable : permet aux contrôleurs JavaFX de réagir au changement de langue. */
    private static final ObjectProperty<Locale> localeProperty = new SimpleObjectProperty<>(Locale.FRENCH);

    private static Properties bundle = new Properties();

    /* Chargement initial au démarrage */
    static {
        load(localeProperty.get());
    }

    private I18n() {}

    // =========================================================================
    // API publique
    // =========================================================================

    /** Propriété observable sur la locale courante. */
    public static ObjectProperty<Locale> localeProperty() {
        return localeProperty;
    }

    public static Locale getLocale() {
        return localeProperty.get();
    }

    /**
     * Change la langue de l'application à chaud.
     * Les contrôleurs qui ont bindé leurs labels sur {@link #localeProperty()}
     * recevront la notification et pourront se mettre à jour immédiatement.
     */
    public static void setLocale(Locale locale) {
        Locale resolved = resolve(locale);
        if (!resolved.equals(localeProperty.get())) {
            load(resolved);
            localeProperty.set(resolved);
        }
    }

    /**
     * Traduit une clé. Si la clé est absente, retourne {@code [clé]} pour
     * faciliter la détection des traductions manquantes en développement.
     */
    public static String t(String key) {
        String value = bundle.getProperty(key);
        if (value == null) {
            LOG.warning("Missing i18n key: " + key);
            return "[" + key + "]";
        }
        return value;
    }

    /**
     * Traduit une clé avec des arguments {@link MessageFormat}.
     * Exemple : {@code t("app.loaded", 3)} avec {@code app.loaded={0} mod(s) chargé(s).}
     * retourne {@code "3 mod(s) chargé(s)."}.
     */
    public static String t(String key, Object... args) {
        String pattern = t(key);
        if (args == null || args.length == 0) {
            return pattern;
        }
        try {
            return MessageFormat.format(pattern, args);
        } catch (IllegalArgumentException ex) {
            LOG.log(Level.WARNING, "MessageFormat error for key '" + key + "': " + ex.getMessage(), ex);
            return pattern;
        }
    }

    // =========================================================================
    // Interne
    // =========================================================================

    private static void load(Locale locale) {
        String path = BASE_PATH + locale.getLanguage() + ".properties";
        Properties props = new Properties();
        try (InputStream is = I18n.class.getResourceAsStream(path)) {
            if (is == null) {
                LOG.warning("Translation file not found: " + path + " — falling back to French.");
                loadFallback(props);
            } else {
                // InputStreamReader en UTF-8 pour gérer les caractères accentués
                // (contrairement à Properties.load(InputStream) qui utilise ISO-8859-1)
                props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
            }
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Failed to load " + path, ex);
            loadFallback(props);
        }
        bundle = props;
    }

    private static void loadFallback(Properties target) {
        String fallback = BASE_PATH + "fr.properties";
        try (InputStream is = I18n.class.getResourceAsStream(fallback)) {
            if (is != null) {
                target.load(new InputStreamReader(is, StandardCharsets.UTF_8));
            }
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Failed to load fallback translation file", ex);
        }
    }

    /**
     * Trouve la locale supportée la plus proche de celle demandée.
     * Ex : {@code Locale.CANADA_FRENCH} → {@link Locale#FRENCH}.
     * Si aucune correspondance, retourne {@link Locale#FRENCH} (langue par défaut).
     */
    private static Locale resolve(Locale requested) {
        if (requested == null) return Locale.FRENCH;
        // Correspondance exacte langue+pays
        for (Locale l : SUPPORTED_LOCALES) {
            if (l.equals(requested)) return l;
        }
        // Correspondance sur la langue seule
        for (Locale l : SUPPORTED_LOCALES) {
            if (l.getLanguage().equals(requested.getLanguage())) return l;
        }
        return Locale.FRENCH;
    }

    /**
     * Détecte automatiquement la langue du système d'exploitation.
     * À appeler au premier démarrage si aucune préférence n'est enregistrée.
     */
    public static Locale detectSystemLocale() {
        return resolve(Locale.getDefault());
    }
}
