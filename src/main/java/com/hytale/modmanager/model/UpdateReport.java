package com.hytale.modmanager.model;

import com.hytale.modmanager.util.I18n;

import java.util.ArrayList;
import java.util.List;

/**
 * Rapport généré après une mise à jour (simple ou par lots).
 * Les libellés sont produits via {@link I18n} afin de respecter la langue
 * courante de l'interface, y compris pour les copies exportées ou journalisées.
 */
public class UpdateReport {

    public static class Entry {
        public final String modName;
        public final UpdateStatus status;
        public final String message;

        public Entry(String modName, UpdateStatus status, String message) {
            this.modName = modName;
            this.status = status;
            this.message = message;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    public void add(String modName, UpdateStatus status, String message) {
        entries.add(new Entry(modName, status, message));
    }

    public List<Entry> getEntries() { return entries; }

    public long countByStatus(UpdateStatus status) {
        return entries.stream().filter(e -> e.status == status).count();
    }

    public int size() { return entries.size(); }

    /** Résumé textuel traduit selon la langue courante. */
    public String toSummaryText() {
        StringBuilder sb = new StringBuilder();
        sb.append(I18n.t("report.summary.title", entries.size())).append('\n');
        sb.append("  • ").append(I18n.t("report.summary.modified")).append(" : ")
                .append(countByStatus(UpdateStatus.MODIFIE)).append('\n');
        sb.append("  • ").append(I18n.t("report.summary.created")).append(" : ")
                .append(countByStatus(UpdateStatus.CREE)).append('\n');
        sb.append("  • ").append(I18n.t("report.summary.uptodate")).append(" : ")
                .append(countByStatus(UpdateStatus.DEJA_A_JOUR)).append('\n');
        sb.append("  • ").append(I18n.t("report.summary.errors")).append(" : ")
                .append(countByStatus(UpdateStatus.ERREUR)).append('\n');
        if (!entries.isEmpty()) {
            sb.append('\n').append(I18n.t("report.summary.detail")).append(" :\n");
            for (Entry e : entries) {
                sb.append("  - [").append(e.status.getLabel()).append("] ").append(e.modName);
                if (e.message != null && !e.message.isBlank()) {
                    sb.append(" — ").append(e.message);
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
