package com.hytale.modmanager.service;

import com.hytale.modmanager.model.UpdateStatus;

/** Résultat de la mise à jour du champ ServerVersion d'un manifest.json donné. */
public class ManifestUpdateResult {

    private final UpdateStatus status;
    private final String message;
    private final String previousValue;
    private final String newValue;

    public ManifestUpdateResult(UpdateStatus status, String message, String previousValue, String newValue) {
        this.status = status;
        this.message = message;
        this.previousValue = previousValue;
        this.newValue = newValue;
    }

    public UpdateStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public String getNewValue() {
        return newValue;
    }
}
