package com.hytale.modmanager.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Représente une version de Hytale connue par le logiciel, telle qu'elle apparaît
 * dans la page "Gestion des versions".
 *
 * Le champ {@link #range} correspond à la chaîne exacte qui sera écrite dans le
 * champ "ServerVersion" du manifest.json d'un mod, par exemple : {@code >=1.0.0 <1.1.0}.
 */
public class HytaleVersion implements Comparable<HytaleVersion> {

    private final String id;
    private String range;
    private String label;
    private String notes;
    private boolean recommended;

    public HytaleVersion(String range) {
        this(UUID.randomUUID().toString(), range, range, "", false);
    }

    public HytaleVersion(String id, String range, String label, String notes, boolean recommended) {
        this.id = id;
        this.range = range;
        this.label = (label == null || label.isBlank()) ? range : label;
        this.notes = notes == null ? "" : notes;
        this.recommended = recommended;
    }

    public String getId() {
        return id;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public void setRecommended(boolean recommended) {
        this.recommended = recommended;
    }

    /**
     * Extrait la borne inférieure de la plage de version (ex : "1.0.0" depuis
     * ">=1.0.0 &lt;1.1.0") afin de permettre un tri par ordre de version.
     * Retourne {0,0,0} si aucune valeur n'a pu être extraite.
     */
    public int[] lowerBound() {
        return VersionUtils.extractLowerBound(range);
    }

    @Override
    public int compareTo(HytaleVersion other) {
        return VersionUtils.compare(this.lowerBound(), other.lowerBound());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HytaleVersion)) return false;
        HytaleVersion that = (HytaleVersion) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return range;
    }
}
