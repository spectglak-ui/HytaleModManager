package com.hytale.modmanager;

import javafx.application.Application;

/**
 * Lanceur séparé de la classe {@link MainApp}.
 *
 * Garder le point d'entrée {@code main(String[])} dans une classe qui n'étend
 * pas {@code javafx.application.Application} évite l'erreur classique
 * "JavaFX runtime components are missing" lorsque l'application est exécutée
 * depuis un .jar exécutable sans être lancée explicitement via le module-path.
 */
public final class Main {
    public static void main(String[] args) {
        Application.launch(MainApp.class, args);
    }
}
