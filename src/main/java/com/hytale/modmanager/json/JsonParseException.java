package com.hytale.modmanager.json;

/** Exception levée lorsqu'un document JSON est invalide ou ne peut pas être analysé. */
public class JsonParseException extends RuntimeException {
    public JsonParseException(String message) {
        super(message);
    }

    public JsonParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
