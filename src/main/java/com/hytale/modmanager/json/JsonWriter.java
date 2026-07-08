package com.hytale.modmanager.json;

import java.util.List;
import java.util.Map;

/**
 * Sérialise une arborescence JSON (telle que produite par {@link JsonParser})
 * vers du texte, avec une indentation configurable. Utilisé uniquement lorsque
 * la structure du document doit être modifiée (par exemple lors de l'ajout
 * d'une nouvelle clé "ServerVersion" absente du fichier d'origine) ; dans tous
 * les autres cas, {@code ManifestUpdater} préfère une édition textuelle ciblée
 * qui laisse le reste du fichier strictement inchangé.
 */
public final class JsonWriter {

    private final String indentUnit;

    public JsonWriter(String indentUnit) {
        this.indentUnit = (indentUnit == null || indentUnit.isEmpty()) ? "  " : indentUnit;
    }

    public String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value, 0);
        sb.append('\n');
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private void writeValue(StringBuilder sb, Object value, int depth) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Map) {
            writeObject(sb, (Map<String, Object>) value, depth);
        } else if (value instanceof List) {
            writeArray(sb, (List<Object>) value, depth);
        } else if (value instanceof String) {
            writeString(sb, (String) value);
        } else if (value instanceof Boolean) {
            sb.append(value);
        } else if (value instanceof Double) {
            double d = (Double) value;
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                sb.append((long) d);
            } else {
                sb.append(d);
            }
        } else {
            sb.append(value);
        }
    }

    private void writeObject(StringBuilder sb, Map<String, Object> map, int depth) {
        if (map.isEmpty()) {
            sb.append("{}");
            return;
        }
        sb.append("{\n");
        int i = 0;
        int size = map.size();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            indent(sb, depth + 1);
            writeString(sb, entry.getKey());
            sb.append(": ");
            writeValue(sb, entry.getValue(), depth + 1);
            if (++i < size) {
                sb.append(',');
            }
            sb.append('\n');
        }
        indent(sb, depth);
        sb.append('}');
    }

    private void writeArray(StringBuilder sb, List<Object> list, int depth) {
        if (list.isEmpty()) {
            sb.append("[]");
            return;
        }
        sb.append("[\n");
        for (int i = 0; i < list.size(); i++) {
            indent(sb, depth + 1);
            writeValue(sb, list.get(i), depth + 1);
            if (i < list.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        indent(sb, depth);
        sb.append(']');
    }

    private void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    private void indent(StringBuilder sb, int depth) {
        for (int i = 0; i < depth; i++) {
            sb.append(indentUnit);
        }
    }
}
