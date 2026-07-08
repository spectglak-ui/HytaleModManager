package com.hytale.modmanager.ui;

import com.hytale.modmanager.model.HytaleVersion;
import com.hytale.modmanager.model.ModEntry;
import com.hytale.modmanager.model.UpdateReport;
import com.hytale.modmanager.model.UpdateStatus;
import com.hytale.modmanager.service.AppSettings;
import com.hytale.modmanager.service.ArchiveExtractor;
import com.hytale.modmanager.service.ModUpdateService;
import com.hytale.modmanager.service.ModsFolderService;
import com.hytale.modmanager.service.VersionManager;
import com.hytale.modmanager.util.AppLogger;
import com.hytale.modmanager.util.I18n;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

public class MainController {

    // ── FXML injections ──────────────────────────────────────────────────────
    @FXML private StackPane dropZone;
    @FXML private TextField searchField;
    @FXML private Button    clearSearchButton;
    @FXML private TableView<ModEntry> modTable;
    @FXML private TableColumn<ModEntry, String>        colName;
    @FXML private TableColumn<ModEntry, String>        colAuthor;
    @FXML private TableColumn<ModEntry, String>        colModVersion;
    @FXML private TableColumn<ModEntry, String>        colServerVersion;
    @FXML private TableColumn<ModEntry, HytaleVersion> colTargetVersion;
    @FXML private TableColumn<ModEntry, UpdateStatus>  colStatus;
    @FXML private TableColumn<ModEntry, String>        colExportInfo;
    @FXML private TableColumn<ModEntry, String>        colMessage;
    @FXML private Label suggestedVersionLabel;
    @FXML private Label statusLabel;
    @FXML private ListView<String> logListView;
    @FXML private ProgressBar progressBar;
    @FXML private Label journalLabel;
    // Menus
    @FXML private Menu menuFile;
    @FXML private MenuItem miImportFolder;
    @FXML private MenuItem miImportArchive;
    @FXML private MenuItem miRemoveSelected;
    @FXML private MenuItem miClearAll;
    @FXML private MenuItem miQuit;
    @FXML private Menu menuMods;
    @FXML private MenuItem miUpdateSelected;
    @FXML private MenuItem miUpdateAll;
    @FXML private MenuItem miRefresh;
    @FXML private MenuItem miExportSelected;
    @FXML private MenuItem miExportAll;
    @FXML private Menu menuVersions;
    @FXML private MenuItem miOpenVersionManager;
    @FXML private Menu menuSettings;
    @FXML private Menu menuLanguage;
    @FXML private RadioMenuItem miLangFr;
    @FXML private RadioMenuItem miLangEn;
    @FXML private MenuItem miChangeModsFolder;
    @FXML private MenuItem miResetModsFolder;
    @FXML private Menu menuHelp;
    @FXML private MenuItem miAbout;
    // Toolbar buttons
    @FXML private Button btnImportFolder;
    @FXML private Button btnImportArchive;
    @FXML private Button btnUpdateSelected;
    @FXML private Button btnUpdateAll;
    @FXML private Button btnExportSelected;
    @FXML private Button btnExportAll;
    @FXML private Button btnManageVersions;
    @FXML private Button btnRefreshMods;
    @FXML private Button btnOpenModsFolder;
    @FXML private Button btnChangeModsFolder;

    // ── State ────────────────────────────────────────────────────────────────
    private final ObservableList<ModEntry> allMods = FXCollections.observableArrayList();
    private FilteredList<ModEntry> filteredMods;
    private final VersionManager     versionManager     = new VersionManager();
    private final ModUpdateService   modUpdateService   = new ModUpdateService();
    private final ModsFolderService  modsFolderService  = new ModsFolderService();
    private final AppLogger          logger             = new AppLogger();
    private final Preferences        prefs              = Preferences.userNodeForPackage(MainController.class);
    private static final String      PREF_LAST_DIR      = "last_import_dir";

    // =========================================================================
    // Initialisation
    // =========================================================================

    @FXML
    public void initialize() {
        // Versions Hytale
        try {
            versionManager.load();
        } catch (IOException ex) {
            showError(I18n.t("err.load_versions"), ex.getMessage());
        }

        // Dossier de mods personnalise depuis settings
        Path savedFolder = AppSettings.get().getCustomModsFolder();
        if (savedFolder != null) {
            modsFolderService.setCustomModsFolder(savedFolder);
        }

        // Tableau avec liste filtree
        filteredMods = new FilteredList<>(allMods, p -> true);
        modTable.setItems(filteredMods);
        modTable.setEditable(true);

        searchField.textProperty().addListener((obs, o, n) -> applyFilter(n));
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) onClearSearch();
        });

        // Colonnes
        colName.setCellValueFactory(d -> d.getValue().nameProperty());
        colAuthor.setCellValueFactory(d -> d.getValue().authorProperty());
        colModVersion.setCellValueFactory(d -> d.getValue().modVersionProperty());
        colServerVersion.setCellValueFactory(d -> d.getValue().currentServerVersionProperty());
        colMessage.setCellValueFactory(d -> d.getValue().messageProperty());
        colTargetVersion.setCellValueFactory(d -> d.getValue().targetVersionProperty());
        colTargetVersion.setCellFactory(ComboBoxTableCell.forTableColumn(
                versionConverter(), versionManager.getVersions()));
        colTargetVersion.setOnEditCommit(ev -> ev.getRowValue().setTargetVersion(ev.getNewValue()));
        colStatus.setCellValueFactory(d -> d.getValue().statusProperty());
        colStatus.setCellFactory(col -> statusCell());
        colExportInfo.setCellValueFactory(d -> d.getValue().exportInfoProperty());
        colExportInfo.setCellFactory(col -> exportInfoCell());

        modTable.setRowFactory(tv -> buildRowWithContextMenu());
        logListView.setItems(logger.getEntries());

        setupDragAndDrop();
        applyTranslations();

        I18n.localeProperty().addListener((obs, old, loc) -> {
            applyTranslations();
            modTable.refresh();
        });

        syncLangMenu();
        refreshSuggestedVersionLabel();
        logger.info(I18n.t("log.app_started", versionManager.getVersions().size()));

        // Scan automatique du dossier Hytale au demarrage
        Platform.runLater(this::autoScanModsFolder);
    }

    // =========================================================================
    // Traductions dynamiques
    // =========================================================================

    public void applyTranslations() {
        menuFile.setText(I18n.t("menu.file"));
        miImportFolder.setText(I18n.t("menu.file.import_folder"));
        miImportArchive.setText(I18n.t("menu.file.import_archive"));
        miRemoveSelected.setText(I18n.t("menu.file.remove_selected"));
        miClearAll.setText(I18n.t("menu.file.clear_all"));
        miQuit.setText(I18n.t("menu.file.quit"));
        menuMods.setText(I18n.t("menu.mods"));
        miUpdateSelected.setText(I18n.t("menu.mods.update_selected"));
        miUpdateAll.setText(I18n.t("menu.mods.update_all"));
        miRefresh.setText(I18n.t("menu.mods.refresh"));
        miExportSelected.setText(I18n.t("menu.mods.export_selected"));
        miExportAll.setText(I18n.t("menu.mods.export_all"));
        menuVersions.setText(I18n.t("menu.versions"));
        miOpenVersionManager.setText(I18n.t("menu.versions.open"));
        menuSettings.setText(I18n.t("menu.settings"));
        menuLanguage.setText(I18n.t("menu.settings.language"));
        miLangFr.setText(I18n.t("menu.settings.language.fr"));
        miLangEn.setText(I18n.t("menu.settings.language.en"));
        miChangeModsFolder.setText(I18n.t("menu.settings.mods_folder"));
        miResetModsFolder.setText(I18n.t("menu.settings.mods_folder_reset"));
        menuHelp.setText(I18n.t("menu.help"));
        miAbout.setText(I18n.t("menu.help.about"));

        btnImportFolder.setText(I18n.t("toolbar.import_folder"));
        btnImportArchive.setText(I18n.t("toolbar.import_archive"));
        btnUpdateSelected.setText(I18n.t("toolbar.update_selected"));
        btnUpdateAll.setText(I18n.t("toolbar.update_all"));
        btnExportSelected.setText(I18n.t("toolbar.export_selected"));
        btnExportAll.setText(I18n.t("toolbar.export_all"));
        btnManageVersions.setText(I18n.t("toolbar.manage_versions"));
        btnRefreshMods.setText(I18n.t("toolbar.refresh_mods"));
        btnOpenModsFolder.setText(I18n.t("toolbar.open_mods_folder"));
        btnChangeModsFolder.setText(I18n.t("toolbar.change_mods_folder"));

        colName.setText(I18n.t("table.col.name"));
        colAuthor.setText(I18n.t("table.col.author"));
        colModVersion.setText(I18n.t("table.col.mod_version"));
        colServerVersion.setText(I18n.t("table.col.server_version"));
        colTargetVersion.setText(I18n.t("table.col.target_version"));
        colStatus.setText(I18n.t("table.col.status"));
        colExportInfo.setText(I18n.t("table.col.save"));
        colMessage.setText(I18n.t("table.col.detail"));

        modTable.setPlaceholder(new Label(I18n.t("table.placeholder")));
        searchField.setPromptText(I18n.t("search.placeholder"));
        journalLabel.setText(I18n.t("journal.title"));

        String cur = statusLabel.getText();
        if (cur == null || cur.isBlank()
                || cur.equals("Pret.") || cur.equals("Ready.") || cur.startsWith("Pret")) {
            statusLabel.setText(I18n.t("app.ready"));
        }
        refreshSuggestedVersionLabel();
    }

    private void syncLangMenu() {
        miLangFr.setSelected(I18n.getLocale().getLanguage().equals("fr"));
        miLangEn.setSelected(I18n.getLocale().getLanguage().equals("en"));
    }

    @FXML private void onSetLangFr() {
        AppSettings.get().setLocale(java.util.Locale.FRENCH);
        syncLangMenu();
    }

    @FXML private void onSetLangEn() {
        AppSettings.get().setLocale(java.util.Locale.ENGLISH);
        syncLangMenu();
    }

    // =========================================================================
    // Recherche
    // =========================================================================

    private void applyFilter(String text) {
        String lower = (text == null) ? "" : text.trim().toLowerCase();
        clearSearchButton.setVisible(!lower.isEmpty());
        filteredMods.setPredicate(mod -> {
            if (lower.isEmpty()) return true;
            return contains(mod.getName(), lower)
                    || contains(mod.getAuthor(), lower)
                    || contains(mod.getModVersion(), lower)
                    || contains(mod.getCurrentServerVersion(), lower)
                    || contains(mod.getStatus().getLabel(), lower);
        });
    }

    private boolean contains(String s, String n) {
        return s != null && s.toLowerCase().contains(n);
    }

    @FXML private void onClearSearch() { searchField.clear(); }

    // =========================================================================
    // Menu contextuel
    // =========================================================================

    private TableRow<ModEntry> buildRowWithContextMenu() {
        TableRow<ModEntry> row = new TableRow<>();
        ContextMenu cm = new ContextMenu();
        row.setOnMouseClicked(ev -> {
            if (!row.isEmpty() && ev.getButton() == MouseButton.SECONDARY) {
                cm.getItems().clear();
                MenuItem miU  = new MenuItem(I18n.t("ctx.update"));
                MenuItem miEx = new MenuItem(I18n.t("ctx.export"));
                MenuItem miRf = new MenuItem(I18n.t("ctx.refresh"));
                MenuItem miOd = new MenuItem(I18n.t("ctx.open_folder"));
                MenuItem miRm = new MenuItem(I18n.t("ctx.remove"));
                miU.setOnAction(e  -> { modTable.getSelectionModel().select(row.getItem()); onUpdateSelected(); });
                miEx.setOnAction(e -> { modTable.getSelectionModel().select(row.getItem()); onExportSelected(); });
                miRf.setOnAction(e -> { modTable.getSelectionModel().select(row.getItem()); onRefreshSelected(); });
                miOd.setOnAction(e -> openModFolder(row.getItem()));
                miRm.setOnAction(e -> { modTable.getSelectionModel().select(row.getItem()); onRemoveSelected(); });
                cm.getItems().addAll(miU, miEx, new SeparatorMenuItem(), miRf, miOd, new SeparatorMenuItem(), miRm);
                cm.show(row, ev.getScreenX(), ev.getScreenY());
            } else {
                cm.hide();
            }
        });
        row.emptyProperty().addListener((obs, w, isEmpty) -> { if (isEmpty) cm.hide(); });
        return row;
    }

    private void openModFolder(ModEntry entry) {
        if (entry == null) return;
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(entry.getModFolder().toFile());
            }
        } catch (IOException ex) {
            showError(I18n.t("err.open_folder"), ex.getMessage());
        }
    }

    // =========================================================================
    // Cellules de tableau
    // =========================================================================

    private StringConverter<HytaleVersion> versionConverter() {
        return new StringConverter<>() {
            @Override public String toString(HytaleVersion v) {
                return v == null ? "" : v.getLabel() + "  (" + v.getRange() + ")";
            }
            @Override public HytaleVersion fromString(String s) {
                return versionManager.getVersions().stream()
                        .filter(v -> toString(v).equals(s)).findFirst().orElse(null);
            }
        };
    }

    private TableCell<ModEntry, UpdateStatus> statusCell() {
        return new TableCell<>() {
            @Override protected void updateItem(UpdateStatus s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s.getLabel());
                switch (s) {
                    case MODIFIE, CREE, DEJA_A_JOUR -> setStyle("-fx-text-fill:#5fbf77;-fx-font-weight:bold;");
                    case ERREUR                      -> setStyle("-fx-text-fill:#e0584f;-fx-font-weight:bold;");
                    default                          -> setStyle("-fx-text-fill:#8b97a8;");
                }
            }
        };
    }

    private TableCell<ModEntry, String> exportInfoCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String info, boolean empty) {
                super.updateItem(info, empty);
                if (empty || info == null) { setText(null); setStyle(""); return; }
                setText(info);
                if (info.startsWith("\u26a0") || info.startsWith("⚠"))
                    setStyle("-fx-text-fill:#e2a23b;-fx-font-weight:bold;");
                else if (info.startsWith("Export") || info.startsWith("Enreg") || info.startsWith("Saved"))
                    setStyle("-fx-text-fill:#5fbf77;");
                else
                    setStyle("-fx-text-fill:#8b97a8;");
            }
        };
    }

    private void refreshSuggestedVersionLabel() {
        Optional<HytaleVersion> suggested = versionManager.resolveSuggestedVersion();
        suggestedVersionLabel.setText(suggested
                .map(v -> v.isRecommended()
                        ? I18n.t("toolbar.suggested_version", v.getLabel(), v.getRange())
                        : I18n.t("toolbar.suggested_version_simple", v.getLabel(), v.getRange()))
                .orElse(I18n.t("toolbar.no_version")));
    }

    // =========================================================================
    // Import depuis fichiers / archives / drag-drop
    // =========================================================================

    @FXML private void onImportFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(I18n.t("dlg.import_folder.title"));
        applyLastDir(chooser);
        File folder = chooser.showDialog(modTable.getScene().getWindow());
        if (folder != null) {
            saveLastDir(folder.toPath().getParent());
            importPaths(List.of(folder.toPath()));
        }
    }

    @FXML private void onImportArchive() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.t("dlg.import_archive.title"));
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(I18n.t("dlg.import_archive.filter_all"), "*.zip", "*.jar", "*.rar"),
                new FileChooser.ExtensionFilter(I18n.t("dlg.import_archive.filter_zip"), "*.zip"),
                new FileChooser.ExtensionFilter(I18n.t("dlg.import_archive.filter_jar"), "*.jar"),
                new FileChooser.ExtensionFilter(I18n.t("dlg.import_archive.filter_rar"), "*.rar"));
        applyLastDir(chooser);
        List<File> files = chooser.showOpenMultipleDialog(modTable.getScene().getWindow());
        if (files != null && !files.isEmpty()) {
            if (files.get(0).getParentFile() != null) saveLastDir(files.get(0).toPath().getParent());
            importPaths(files.stream().map(File::toPath).toList());
        }
    }

    private void setupDragAndDrop() {
        dropZone.setOnDragOver(ev -> {
            if (ev.getGestureSource() != dropZone && ev.getDragboard().hasFiles()) {
                ev.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                dropZone.getStyleClass().add("drop-zone-active");
            }
            ev.consume();
        });
        dropZone.setOnDragExited(ev -> dropZone.getStyleClass().remove("drop-zone-active"));
        dropZone.setOnDragDropped(ev -> {
            boolean ok = false;
            if (ev.getDragboard().hasFiles()) {
                importPaths(ev.getDragboard().getFiles().stream().map(File::toPath).toList());
                ok = true;
            }
            ev.setDropCompleted(ok);
            ev.consume();
        });
    }

    /** Point d'entree commun pour tous les imports (dossier, archive, drag-drop, scan auto). */
    void importPaths(List<Path> paths) {
        progressBar.setProgress(-1);
        statusLabel.setText(I18n.t("status.importing"));

        Task<List<String>> task = new Task<>() {
            @Override protected List<String> call() {
                List<String> errors = new ArrayList<>();
                for (Path path : paths) {
                    try {
                        ModEntry entry = modUpdateService.importMod(path, versionManager);
                        Platform.runLater(() -> {
                            allMods.add(entry);
                            logger.info(I18n.t("log.mod_imported", entry.getName(), path.getFileName()));
                            if (entry.isFromTemporaryExtraction()) {
                                logger.info(I18n.t("log.mod_archive_hint", entry.getName()));
                            }
                        });
                    } catch (ArchiveExtractor.ExtractionException | IOException ex) {
                        errors.add(path.getFileName() + " : " + ex.getMessage());
                        Platform.runLater(() ->
                                logger.error(I18n.t("log.import_failed", path.getFileName(), ex.getMessage())));
                    }
                }
                return errors;
            }
        };
        task.setOnSucceeded(e -> {
            progressBar.setProgress(0);
            statusLabel.setText(I18n.t("app.loaded", allMods.size()));
            List<String> errors = task.getValue();
            if (!errors.isEmpty()) showError(I18n.t("err.import_failed"), String.join("\n", errors));
        });
        task.setOnFailed(e -> {
            progressBar.setProgress(0);
            statusLabel.setText(I18n.t("app.ready"));
            showError(I18n.t("err.import_unexpected"), String.valueOf(task.getException()));
        });
        new Thread(task, "import-mods").start();
    }

    private void saveLastDir(Path dir) {
        if (dir != null && Files.isDirectory(dir))
            prefs.put(PREF_LAST_DIR, dir.toAbsolutePath().toString());
    }
    private void applyLastDir(DirectoryChooser c) {
        String l = prefs.get(PREF_LAST_DIR, null);
        if (l != null) { File f = new File(l); if (f.isDirectory()) c.setInitialDirectory(f); }
    }
    private void applyLastDir(FileChooser c) {
        String l = prefs.get(PREF_LAST_DIR, null);
        if (l != null) { File f = new File(l); if (f.isDirectory()) c.setInitialDirectory(f); }
    }

    @FXML private void onRemoveSelected() {
        ModEntry sel = modTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        if (hasUnsavedChanges(List.of(sel)) && !confirmDiscard("menu.file.remove_selected")) return;
        modUpdateService.cleanupIfTemporary(sel);
        allMods.remove(sel);
        statusLabel.setText(I18n.t("app.loaded", allMods.size()));
    }

    @FXML private void onClearAll() {
        if (hasUnsavedChanges(allMods) && !confirmDiscard("menu.file.clear_all")) return;
        for (ModEntry e : allMods) modUpdateService.cleanupIfTemporary(e);
        allMods.clear();
        statusLabel.setText(I18n.t("app.ready"));
    }

    @FXML private void onRefreshSelected() {
        ModEntry sel = modTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            modUpdateService.refreshFromManifest(sel);
            modTable.refresh();
            logger.info(I18n.t("log.mod_refreshed", sel.getName()));
        } catch (IOException ex) { showError(I18n.t("err.refresh"), ex.getMessage()); }
    }

    // =========================================================================
    // Dossier des mods Hytale
    // =========================================================================

    /**
     * Scanne automatiquement le dossier des mods Hytale.
     * Lance d'abord le scan dans un thread de fond, puis demarre la surveillance
     * WatchService a la fin. Robuste contre toutes les erreurs possibles.
     */
    private void autoScanModsFolder() {
        Path modsFolder = modsFolderService.getModsFolder();

        if (!Files.exists(modsFolder)) {
            logger.info(I18n.t("log.mods_folder.not_found", modsFolder));
            return;
        }

        logger.info(I18n.t("log.mods_folder.scanning"));
        statusLabel.setText(I18n.t("log.mods_folder.scanning"));

        Task<List<Path>> scanTask = new Task<>() {
            @Override protected List<Path> call() throws IOException {
                return modsFolderService.scanForMods();
            }
        };

        scanTask.setOnSucceeded(e -> {
            List<Path> paths = scanTask.getValue();
            if (paths.isEmpty()) {
                logger.info(I18n.t("log.mods_folder.empty"));
                statusLabel.setText(I18n.t("app.ready"));
            } else {
                logger.info(I18n.t("log.mods_folder.found", paths.size()));
                importPaths(paths);
            }
            startWatchingModsFolder();
        });

        scanTask.setOnFailed(e -> {
            Throwable ex = scanTask.getException();
            logger.error(I18n.t("log.mods_folder.access_error",
                    ex != null ? ex.getMessage() : "?"));
            statusLabel.setText(I18n.t("app.ready"));
        });

        new Thread(scanTask, "auto-scan-mods").start();
    }

    private void startWatchingModsFolder() {
        modsFolderService.startWatching(() ->
                Platform.runLater(() -> {
                    logger.info(I18n.t("log.mods_folder.watch_updated"));
                    // Rafraichissement complet : vider + rescanner
                    for (ModEntry e : allMods) modUpdateService.cleanupIfTemporary(e);
                    allMods.clear();
                    autoScanModsFolder();
                })
        );
        logger.info(I18n.t("log.mods_folder.watch_started"));
    }

    /** Bouton "Actualiser les mods" : vide la liste et rescanne depuis le dossier courant. */
    @FXML private void onRefreshMods() {
        if (hasUnsavedChanges(allMods) && !confirmDiscard("toolbar.refresh_mods")) return;
        modsFolderService.stopWatching();
        for (ModEntry e : allMods) modUpdateService.cleanupIfTemporary(e);
        allMods.clear();
        autoScanModsFolder();
    }

    /** Bouton "Ouvrir le dossier" : ouvre l'Explorateur Windows sur le dossier courant. */
    @FXML private void onOpenModsFolder() {
        Path folder = modsFolderService.getModsFolder();
        if (!Files.isDirectory(folder)) {
            showInfo(I18n.t("err.open_folder"), I18n.t("dlg.mods_folder.no_valid_folder"));
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(folder.toFile());
            }
        } catch (IOException ex) {
            showError(I18n.t("dlg.mods_folder.open_error"), ex.getMessage());
        }
    }

    /** Bouton "Changer le dossier" : choisit un autre dossier de mods et rescanne. */
    @FXML private void onChangeModsFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(I18n.t("dlg.choose_mods_folder.title"));
        Path current = modsFolderService.getModsFolder();
        if (current != null && Files.isDirectory(current)) {
            chooser.setInitialDirectory(current.toFile());
        }
        File dir = chooser.showDialog(modTable.getScene().getWindow());
        if (dir == null) return;
        Path newFolder = dir.toPath();
        AppSettings.get().setModsFolder(newFolder);
        modsFolderService.setCustomModsFolder(newFolder);
        logger.info(I18n.t("dlg.mods_folder.changed", newFolder));
        statusLabel.setText(I18n.t("dlg.mods_folder.changed", newFolder));
        modsFolderService.stopWatching();
        onRefreshMods();
    }

    /** Reinitialise le dossier de mods vers le chemin Hytale par defaut. */
    @FXML private void onResetModsFolder() {
        AppSettings.get().clearModsFolder();
        modsFolderService.setCustomModsFolder(null);
        Path defaultFolder = modsFolderService.getDefaultModsFolder();
        logger.info(I18n.t("dlg.mods_folder.reset"));
        logger.info(I18n.t("dlg.mods_folder.changed", defaultFolder));
        statusLabel.setText(I18n.t("dlg.mods_folder.reset"));
        modsFolderService.stopWatching();
        onRefreshMods();
    }

    // =========================================================================
    // Mise a jour
    // =========================================================================

    @FXML private void onUpdateSelected() {
        ModEntry sel = modTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showInfo(I18n.t("dlg.info.no_mod_selected.header"), I18n.t("dlg.info.no_mod_selected.content"));
            return;
        }
        runUpdate(List.of(sel));
    }

    @FXML private void onUpdateAll() {
        if (allMods.isEmpty()) {
            showInfo(I18n.t("dlg.info.no_mod_for_update.header"), I18n.t("dlg.info.no_mod_for_update.content"));
            return;
        }
        runUpdate(new ArrayList<>(allMods));
    }

    private void runUpdate(List<ModEntry> targets) {
        progressBar.setProgress(0);
        statusLabel.setText(I18n.t("status.updating", 0, targets.size()));
        AtomicInteger done = new AtomicInteger(0);
        int total = targets.size();

        Task<UpdateReport> task = new Task<>() {
            @Override protected UpdateReport call() {
                UpdateReport report = new UpdateReport();
                for (ModEntry entry : targets) {
                    UpdateReport.Entry result = modUpdateService.updateSingle(entry);
                    report.add(result.modName, result.status, result.message);
                    int n = done.incrementAndGet();
                    Platform.runLater(() -> {
                        progressBar.setProgress((double) n / total);
                        statusLabel.setText(I18n.t("status.updating", n, total));
                        modTable.refresh();
                    });
                }
                return report;
            }
        };
        task.setOnSucceeded(e -> {
            progressBar.setProgress(0);
            UpdateReport report = task.getValue();
            statusLabel.setText(I18n.t("status.update_done", total,
                    report.countByStatus(UpdateStatus.MODIFIE),
                    report.countByStatus(UpdateStatus.CREE),
                    report.countByStatus(UpdateStatus.DEJA_A_JOUR),
                    report.countByStatus(UpdateStatus.ERREUR)));
            for (UpdateReport.Entry r : report.getEntries()) {
                String msg = (r.message != null && !r.message.isBlank())
                        ? I18n.t("log.update_entry_detail", r.status.getLabel(), r.modName, r.message)
                        : I18n.t("log.update_entry", r.status.getLabel(), r.modName);
                logger.info(msg);
            }
            modTable.refresh();
            showReportDialog(report);
        });
        task.setOnFailed(e -> {
            progressBar.setProgress(0);
            statusLabel.setText(I18n.t("app.ready"));
            showError(I18n.t("err.update_unexpected"), String.valueOf(task.getException()));
        });
        new Thread(task, "update-mods").start();
    }

    private void showReportDialog(UpdateReport report) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18n.t("dlg.report.title"));
        alert.setHeaderText(I18n.t("dlg.report.header", report.size(),
                report.countByStatus(UpdateStatus.MODIFIE),
                report.countByStatus(UpdateStatus.CREE),
                report.countByStatus(UpdateStatus.ERREUR)));
        TextArea details = new TextArea(report.toSummaryText());
        details.setEditable(false); details.setWrapText(true); details.setPrefSize(580, 340);
        alert.getDialogPane().setContent(details);
        alert.getButtonTypes().setAll(ButtonType.OK);
        alert.showAndWait();
    }

    // =========================================================================
    // Export
    // =========================================================================

    @FXML private void onExportSelected() {
        ModEntry sel = modTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showInfo(I18n.t("dlg.info.no_mod_for_selection.header"), I18n.t("dlg.info.no_mod_for_selection.content"));
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.t("dlg.export_selected.title"));
        String suggested = modUpdateService.suggestExportFileName(sel);
        if (suggested.toLowerCase().endsWith(".jar")) {
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter(I18n.t("dlg.import_archive.filter_jar"), "*.jar"),
                    new FileChooser.ExtensionFilter(I18n.t("dlg.import_archive.filter_zip"), "*.zip"));
        } else {
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter(I18n.t("dlg.import_archive.filter_zip"), "*.zip"),
                    new FileChooser.ExtensionFilter(I18n.t("dlg.import_archive.filter_jar"), "*.jar"));
        }
        chooser.setInitialFileName(suggested);
        if (sel.getOriginalSource() != null && sel.getOriginalSource().getParent() != null) {
            File p = sel.getOriginalSource().getParent().toFile();
            if (p.isDirectory()) chooser.setInitialDirectory(p);
        }
        File file = chooser.showSaveDialog(modTable.getScene().getWindow());
        if (file == null) return;
        try {
            modUpdateService.exportMod(sel, file.toPath());
            modTable.refresh();
            logger.info(I18n.t("log.mod_exported", sel.getName(), file.getAbsolutePath()));
            showInfo(I18n.t("dlg.export_success.header"),
                    I18n.t("dlg.export_success.content", sel.getName(), file.getAbsolutePath()));
        } catch (IOException ex) {
            logger.error(I18n.t("log.export_failed", sel.getName(), ex.getMessage()));
            showError(I18n.t("dlg.export_error.header"), ex.getMessage());
        }
    }

    @FXML private void onExportAll() {
        if (allMods.isEmpty()) {
            showInfo(I18n.t("dlg.info.no_mod_for_export.header"), I18n.t("dlg.info.no_mod_for_export.content"));
            return;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(I18n.t("dlg.export_all.title"));
        applyLastDir(chooser);
        File dir = chooser.showDialog(modTable.getScene().getWindow());
        if (dir == null) return;
        int success = 0;
        StringBuilder errors = new StringBuilder();
        for (ModEntry entry : allMods) {
            try {
                Path dest = dir.toPath().resolve(modUpdateService.suggestExportFileName(entry));
                modUpdateService.exportMod(entry, dest);
                logger.info(I18n.t("log.mod_exported", entry.getName(), dest));
                success++;
            } catch (IOException ex) {
                errors.append(entry.getName()).append(" : ").append(ex.getMessage()).append('\n');
                logger.error(I18n.t("log.export_failed", entry.getName(), ex.getMessage()));
            }
        }
        modTable.refresh();
        String summary = I18n.t("dlg.export_done.content", success, dir.getAbsolutePath());
        if (errors.length() > 0) showError(I18n.t("dlg.export_error.header"), summary + "\n\n" + errors);
        else showInfo(I18n.t("dlg.export_done.header"), summary);
    }

    // =========================================================================
    // Gestion des versions  — BUG FIX : stage.setScene() manquant
    // =========================================================================

    @FXML private void onOpenVersionManager() {
        try {
            var loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/hytale/modmanager/view/version_manager.fxml"));
            javafx.scene.Parent root = loader.load();
            VersionManagerController ctrl = loader.getController();
            ctrl.setVersionManager(versionManager);

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/com/hytale/modmanager/css/dark-theme.css").toExternalForm());

            Stage stage = new Stage();
            stage.setTitle(I18n.t("vdlg.title"));
            stage.setScene(scene);   // ← BUG FIX : ligne precedemment manquante
            stage.setWidth(820);
            stage.setHeight(620);
            stage.getIcons().addAll(
                    new javafx.scene.image.Image(getClass().getResourceAsStream("/com/hytale/modmanager/icon/icon-256.png")),
                    new javafx.scene.image.Image(getClass().getResourceAsStream("/com/hytale/modmanager/icon/icon-64.png")),
                    new javafx.scene.image.Image(getClass().getResourceAsStream("/com/hytale/modmanager/icon/icon-32.png")));
            stage.setOnHidden(e -> { refreshSuggestedVersionLabel(); modTable.refresh(); });
            stage.show();
        } catch (IOException ex) {
            showError(I18n.t("err.open_version_manager"), ex.getMessage());
        }
    }

    @FXML private void onAbout() {
        try {
            var loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/hytale/modmanager/view/about.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/com/hytale/modmanager/css/dark-theme.css").toExternalForm());

            Stage stage = new Stage();
            stage.setTitle(I18n.t("about.window.title"));
            stage.setScene(scene);
            stage.setWidth(640);
            stage.setHeight(760);
            stage.setMinWidth(560);
            stage.setMinHeight(520);
            stage.getIcons().addAll(
                    new javafx.scene.image.Image(getClass().getResourceAsStream("/com/hytale/modmanager/icon/icon-256.png")),
                    new javafx.scene.image.Image(getClass().getResourceAsStream("/com/hytale/modmanager/icon/icon-64.png")),
                    new javafx.scene.image.Image(getClass().getResourceAsStream("/com/hytale/modmanager/icon/icon-32.png")));
            stage.show();
        } catch (IOException ex) {
            showError(I18n.t("err.open_about"), ex.getMessage());
        }
    }

    // =========================================================================
    // Fermeture
    // =========================================================================

    @FXML private void onQuit() { if (requestClose()) Platform.exit(); }

    public boolean requestClose() {
        if (hasUnsavedChanges(allMods) && !confirmDiscard("menu.file.quit")) return false;
        modsFolderService.stopWatching();
        for (ModEntry e : allMods) modUpdateService.cleanupIfTemporary(e);
        return true;
    }

    private boolean hasUnsavedChanges(List<ModEntry> entries) {
        return entries.stream().anyMatch(e -> {
            String info = e.getExportInfo();
            return info != null && (info.startsWith("\u26a0") || info.startsWith("⚠"));
        });
    }

    private boolean confirmDiscard(String actionKey) {
        Alert a = new Alert(Alert.AlertType.WARNING,
                I18n.t("dlg.confirm.unsaved.content", I18n.t(actionKey)),
                ButtonType.YES, ButtonType.NO);
        a.setTitle(I18n.t("dlg.confirm.unsaved.title"));
        a.setHeaderText(I18n.t("dlg.confirm.unsaved.header"));
        return a.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }

    // =========================================================================
    // Helpers UI
    // =========================================================================

    private void showError(String header, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(I18n.t("err.title")); a.setHeaderText(header); a.setContentText(content);
        a.showAndWait();
    }

    private void showInfo(String header, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(I18n.t("dlg.about.title")); a.setHeaderText(header); a.setContentText(content);
        a.showAndWait();
    }
}
