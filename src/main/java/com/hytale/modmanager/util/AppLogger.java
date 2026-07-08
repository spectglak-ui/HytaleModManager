package com.hytale.modmanager.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Journal des opérations. Les messages transmis doivent être déjà traduits
 * (produits via {@link I18n#t(String, Object...)}) afin que les entrées
 * persistées dans {@code journal.log} reflètent la langue choisie au moment
 * de l'action.
 */
public class AppLogger {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObservableList<String> entries = FXCollections.observableArrayList();
    private final Path logFile;

    public AppLogger() { this(ConfigPaths.logFile()); }

    public AppLogger(Path logFile) { this.logFile = logFile; }

    public ObservableList<String> getEntries() { return entries; }

    public void info(String message) { log("INFO", message); }

    public void error(String message) { log(I18n.t("err.title"), message); }

    private void log(String level, String message) {
        String line = "[" + LocalDateTime.now().format(TIME_FORMAT) + "] [" + level + "] " + message;
        entries.add(0, line);
        appendToFile(line);
    }

    private void appendToFile(String line) {
        try {
            ConfigPaths.ensureExists(logFile.getParent());
            Files.writeString(logFile, line + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }
}
