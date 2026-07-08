package com.hytale.modmanager.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Analyseur JSON minimaliste, sans dépendance externe.
 *
 * Le but n'est pas d'être un parseur JSON généraliste exhaustif, mais de lire
 * fidèlement les fichiers manifest.json des mods Hytale (objets, tableaux,
 * chaînes, nombres, booléens, null) en conservant l'ordre des clés grâce à
 * {@link LinkedHashMap}, afin de pouvoir les réécrire à l'identique lorsque
 * c'est nécessaire (voir {@link JsonWriter}).
 */
public final class JsonParser {

    private final String text;
    private int pos;

    private JsonParser(String text) {
        this.text = text;
        this.pos = 0;
    }

    /** Parse une chaîne JSON et retourne sa représentation arborescente (Map/List/String/Number/Boolean/null). */
    public static Object parse(String json) {
        JsonParser parser = new JsonParser(json);
        parser.skipWhitespace();
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (parser.pos < parser.text.length()) {
            throw new JsonParseException("Caractères inattendus en fin de document JSON à la position " + parser.pos);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Object value = parse(json);
        if (!(value instanceof Map)) {
            throw new JsonParseException("Le document JSON ne contient pas un objet à la racine.");
        }
        return (Map<String, Object>) value;
    }

    private Object parseValue() {
        skipWhitespace();
        if (pos >= text.length()) {
            throw new JsonParseException("Fin de document JSON inattendue.");
        }
        char c = text.charAt(pos);
        switch (c) {
            case '{':
                return parseObjectInternal();
            case '[':
                return parseArray();
            case '"':
                return parseString();
            case 't':
            case 'f':
                return parseBoolean();
            case 'n':
                return parseNull();
            default:
                return parseNumber();
        }
    }

    private Map<String, Object> parseObjectInternal() {
        Map<String, Object> map = new LinkedHashMap<>();
        expect('{');
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return map;
        }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            Object value = parseValue();
            map.put(key, value);
            skipWhitespace();
            char next = peek();
            if (next == ',') {
                pos++;
            } else if (next == '}') {
                pos++;
                break;
            } else {
                throw new JsonParseException("Caractère ',' ou '}' attendu à la position " + pos);
            }
        }
        return map;
    }

    private List<Object> parseArray() {
        List<Object> list = new ArrayList<>();
        expect('[');
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return list;
        }
        while (true) {
            Object value = parseValue();
            list.add(value);
            skipWhitespace();
            char next = peek();
            if (next == ',') {
                pos++;
            } else if (next == ']') {
                pos++;
                break;
            } else {
                throw new JsonParseException("Caractère ',' ou ']' attendu à la position " + pos);
            }
        }
        return list;
    }

    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= text.length()) {
                throw new JsonParseException("Chaîne JSON non terminée.");
            }
            char c = text.charAt(pos++);
            if (c == '"') {
                break;
            }
            if (c == '\\') {
                if (pos >= text.length()) {
                    throw new JsonParseException("Séquence d'échappement incomplète.");
                }
                char esc = text.charAt(pos++);
                switch (esc) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (pos + 4 > text.length()) {
                            throw new JsonParseException("Séquence unicode incomplète.");
                        }
                        String hex = text.substring(pos, pos + 4);
                        sb.append((char) Integer.parseInt(hex, 16));
                        pos += 4;
                        break;
                    default:
                        throw new JsonParseException("Séquence d'échappement invalide : \\" + esc);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Object parseNumber() {
        int start = pos;
        if (peek() == '-') {
            pos++;
        }
        while (pos < text.length() && Character.isDigit(text.charAt(pos))) {
            pos++;
        }
        boolean isDouble = false;
        if (pos < text.length() && text.charAt(pos) == '.') {
            isDouble = true;
            pos++;
            while (pos < text.length() && Character.isDigit(text.charAt(pos))) {
                pos++;
            }
        }
        if (pos < text.length() && (text.charAt(pos) == 'e' || text.charAt(pos) == 'E')) {
            isDouble = true;
            pos++;
            if (pos < text.length() && (text.charAt(pos) == '+' || text.charAt(pos) == '-')) {
                pos++;
            }
            while (pos < text.length() && Character.isDigit(text.charAt(pos))) {
                pos++;
            }
        }
        String numStr = text.substring(start, pos);
        if (numStr.isEmpty() || "-".equals(numStr)) {
            throw new JsonParseException("Nombre JSON invalide à la position " + start);
        }
        if (isDouble) {
            return Double.parseDouble(numStr);
        }
        try {
            return Long.parseLong(numStr);
        } catch (NumberFormatException ex) {
            return Double.parseDouble(numStr);
        }
    }

    private Boolean parseBoolean() {
        if (text.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        }
        if (text.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        }
        throw new JsonParseException("Valeur booléenne invalide à la position " + pos);
    }

    private Object parseNull() {
        if (text.startsWith("null", pos)) {
            pos += 4;
            return null;
        }
        throw new JsonParseException("Valeur 'null' invalide à la position " + pos);
    }

    private void expect(char c) {
        skipWhitespace();
        if (pos >= text.length() || text.charAt(pos) != c) {
            throw new JsonParseException("Caractère '" + c + "' attendu à la position " + pos);
        }
        pos++;
    }

    private char peek() {
        skipWhitespace();
        if (pos >= text.length()) {
            throw new JsonParseException("Fin de document JSON inattendue.");
        }
        return text.charAt(pos);
    }

    private void skipWhitespace() {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
    }
}
