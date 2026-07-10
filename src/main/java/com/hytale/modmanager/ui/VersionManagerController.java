package com.hytale.modmanager.ui;

import com.hytale.modmanager.model.HytaleVersion;
import com.hytale.modmanager.service.HytaleVersionDetector;
import com.hytale.modmanager.service.VersionManager;
import com.hytale.modmanager.util.I18n;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.nio.file.Path;

public class VersionManagerController {

    @FXML private Label pageTitle;
    @FXML private Label pageSubtitle;
    @FXML private TableView<HytaleVersion> versionTable;
    @FXML private TableColumn<HytaleVersion, Boolean> colRecommended;
    @FXML private TableColumn<HytaleVersion, String>  colRange;
    @FXML private TableColumn<HytaleVersion, String>  colLabel;
    @FXML private TableColumn<HytaleVersion, String>  colNotes;
    @FXML private Label formTitle;
    @FXML private Label lblRange;
    @FXML private Label lblLabel;
    @FXML private Label lblNotes;
    @FXML private TextField rangeField;
    @FXML private TextField labelField;
    @FXML private TextArea  notesField;
    @FXML private CheckBox  recommendedCheckBox;
    @FXML private Button    btnAdd;
    @FXML private Button    btnSave;
    @FXML private Button    btnDelete;
    @FXML private Button    btnSetRecommended;
    @FXML private Button    btnImportJson;
    @FXML private Button    btnExportJson;
    @FXML private Label     versionStatusLabel;

    private VersionManager versionManager;
    private final HytaleVersionDetector hytaleVersionDetector = new HytaleVersionDetector();

    public void setVersionManager(VersionManager versionManager) {
        this.versionManager = versionManager;
        versionTable.setItems(versionManager.getVersions());
        // Recalcule la ServerVersion recommandee a l'ouverture de cette
        // fenetre, a partir de la version Hytale reellement installee.
        // Ne modifie que l'entree recommandee ; conserve la valeur existante
        // si Hytale n'est pas installe ou si le journal est illisible.
        try {
            hytaleVersionDetector.refreshRecommendedVersion(versionManager);
            versionTable.refresh();
        } catch (IOException ex) {
            showError(I18n.t("vdlg.err.recommended"), ex.getMessage());
        }
    }

    @FXML public void initialize() {
        colRange.setCellValueFactory(new PropertyValueFactory<>("range"));
        colLabel.setCellValueFactory(new PropertyValueFactory<>("label"));
        colNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));
        colRecommended.setCellValueFactory(d ->
                new javafx.beans.property.SimpleBooleanProperty(d.getValue().isRecommended()));
        colRecommended.setCellFactory(col -> starCell());

        versionTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> fillFormFrom(sel));

        applyTranslations();
        I18n.localeProperty().addListener((obs, old, loc) -> applyTranslations());
    }

    private void applyTranslations() {
        pageTitle.setText(I18n.t("vdlg.page_title"));
        pageSubtitle.setText(I18n.t("vdlg.subtitle"));
        colRecommended.setText(I18n.t("vdlg.col.recommended"));
        colRange.setText(I18n.t("vdlg.col.range"));
        colLabel.setText(I18n.t("vdlg.col.label"));
        colNotes.setText(I18n.t("vdlg.col.notes"));
        versionTable.setPlaceholder(new Label(I18n.t("vdlg.placeholder")));
        formTitle.setText(I18n.t("vdlg.form.title"));
        lblRange.setText(I18n.t("vdlg.form.range"));
        lblLabel.setText(I18n.t("vdlg.form.label"));
        lblNotes.setText(I18n.t("vdlg.form.notes"));
        rangeField.setPromptText(I18n.t("vdlg.form.range.prompt"));
        labelField.setPromptText(I18n.t("vdlg.form.label.prompt"));
        recommendedCheckBox.setText(I18n.t("vdlg.form.recommended"));
        btnAdd.setText(I18n.t("vdlg.btn.add"));
        btnSave.setText(I18n.t("vdlg.btn.save"));
        btnDelete.setText(I18n.t("vdlg.btn.delete"));
        btnSetRecommended.setText(I18n.t("vdlg.btn.set_recommended"));
        btnImportJson.setText(I18n.t("vdlg.btn.import_json"));
        btnExportJson.setText(I18n.t("vdlg.btn.export_json"));
        versionTable.refresh();
    }

    private TableCell<HytaleVersion, Boolean> starCell() {
        return new TableCell<>() {
            @Override protected void updateItem(Boolean r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null : (r ? "★" : ""));
                setStyle("-fx-text-fill: #e2a23b; -fx-font-weight: bold; -fx-alignment: CENTER;");
            }
        };
    }

    private void fillFormFrom(HytaleVersion v) {
        if (v == null) return;
        rangeField.setText(v.getRange());
        labelField.setText(v.getLabel());
        notesField.setText(v.getNotes());
        recommendedCheckBox.setSelected(v.isRecommended());
    }

    @FXML private void onAdd() {
        String range = trimmed(rangeField.getText());
        if (range.isEmpty()) { warn(I18n.t("vdlg.warn.range_required")); return; }
        HytaleVersion v = new HytaleVersion(range);
        v.setLabel(orDefault(labelField.getText(), range));
        v.setNotes(notesField.getText());
        try {
            versionManager.add(v);
            if (recommendedCheckBox.isSelected()) versionManager.setRecommended(v);
            clearForm(); setStatus(I18n.t("vdlg.status.added", v.getRange()));
        } catch (IOException ex) { showError(I18n.t("vdlg.err.add"), ex.getMessage()); }
    }

    @FXML private void onSaveEdit() {
        HytaleVersion sel = versionTable.getSelectionModel().getSelectedItem();
        if (sel == null) { warn(I18n.t("vdlg.warn.select_to_edit")); return; }
        String range = trimmed(rangeField.getText());
        if (range.isEmpty()) { warn(I18n.t("vdlg.warn.range_required_edit")); return; }
        sel.setRange(range);
        sel.setLabel(orDefault(labelField.getText(), range));
        sel.setNotes(notesField.getText());
        try {
            if (recommendedCheckBox.isSelected()) versionManager.setRecommended(sel);
            else { sel.setRecommended(false); versionManager.update(sel); }
            versionTable.refresh();
            setStatus(I18n.t("vdlg.status.saved", sel.getRange()));
        } catch (IOException ex) { showError(I18n.t("vdlg.err.save"), ex.getMessage()); }
    }

    @FXML private void onDelete() {
        HytaleVersion sel = versionTable.getSelectionModel().getSelectedItem();
        if (sel == null) { warn(I18n.t("vdlg.warn.select_to_delete")); return; }
        Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                I18n.t("dlg.confirm.delete_version.content", sel.getRange()));
        c.setTitle(I18n.t("dlg.confirm.title"));
        if (c.showAndWait().filter(b -> b.getButtonData().isDefaultButton()).isPresent()) {
            try { versionManager.remove(sel); clearForm(); setStatus(I18n.t("vdlg.status.deleted")); }
            catch (IOException ex) { showError(I18n.t("vdlg.err.delete"), ex.getMessage()); }
        }
    }

    @FXML private void onSetRecommended() {
        HytaleVersion sel = versionTable.getSelectionModel().getSelectedItem();
        if (sel == null) { warn(I18n.t("vdlg.warn.select_to_recommend")); return; }
        try {
            versionManager.setRecommended(sel);
            versionTable.refresh();
            recommendedCheckBox.setSelected(true);
            setStatus(I18n.t("vdlg.status.recommended", sel.getRange()));
        } catch (IOException ex) { showError(I18n.t("vdlg.err.recommended"), ex.getMessage()); }
    }

    @FXML private void onImportJson() {
        FileChooser c = new FileChooser();
        c.setTitle(I18n.t("vdlg.dlg.import.title"));
        c.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.t("vdlg.filter.json"), "*.json"));
        java.io.File f = c.showOpenDialog(versionTable.getScene().getWindow());
        if (f == null) return;
        try {
            versionManager.importFromFile(f.toPath());
            setStatus(I18n.t("vdlg.status.imported", f.getName(), versionManager.getVersions().size()));
        } catch (IOException | IllegalArgumentException ex) { showError(I18n.t("vdlg.err.import_title"), ex.getMessage()); }
    }

    @FXML private void onExportJson() {
        FileChooser c = new FileChooser();
        c.setTitle(I18n.t("vdlg.dlg.export.title"));
        c.setInitialFileName(I18n.t("vdlg.dlg.export.filename"));
        c.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.t("vdlg.filter.json"), "*.json"));
        java.io.File f = c.showSaveDialog(versionTable.getScene().getWindow());
        if (f == null) return;
        try {
            versionManager.exportToFile(f.toPath());
            setStatus(I18n.t("vdlg.status.exported", f.getName()));
        } catch (IOException ex) { showError(I18n.t("vdlg.err.export_title"), ex.getMessage()); }
    }

    private void clearForm() {
        rangeField.clear(); labelField.clear(); notesField.clear();
        recommendedCheckBox.setSelected(false);
        versionTable.getSelectionModel().clearSelection();
    }

    private String trimmed(String s) { return s == null ? "" : s.trim(); }
    private String orDefault(String s, String def) { return (s == null || s.isBlank()) ? def : s.trim(); }
    private void setStatus(String msg) { versionStatusLabel.setText(msg); }

    private void warn(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg);
        a.setTitle(I18n.t("vdlg.warn.title")); a.showAndWait();
    }

    private void showError(String header, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(I18n.t("err.title")); a.setHeaderText(header); a.setContentText(content);
        a.showAndWait();
    }
}
