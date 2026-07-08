package com.hytale.modmanager.model;

import com.hytale.modmanager.util.I18n;

/**
 * Statut d'un mod dans le tableau principal.
 * Le libellé affiché est traduit dynamiquement via {@link I18n}.
 */
public enum UpdateStatus {

    EN_ATTENTE("status.pending"),
    MODIFIE("status.modified"),
    CREE("status.created"),
    DEJA_A_JOUR("status.up_to_date"),
    ERREUR("status.error");

    private final String i18nKey;

    UpdateStatus(String i18nKey) {
        this.i18nKey = i18nKey;
    }

    public String getLabel() {
        return I18n.t(i18nKey);
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
