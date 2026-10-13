package br.com.projeto.service;

import java.util.prefs.Preferences;

/** Guarda a preferência de tema do computador (java.util.prefs, fora do repositório). */
public class ThemeService {

    public enum Theme {
        LIGHT("/css/light.css", "Tema claro"),
        DARK("/css/dark.css", "Tema escuro");

        private final String stylesheet;
        private final String label;

        Theme(String stylesheet, String label) {
            this.stylesheet = stylesheet;
            this.label = label;
        }

        public String stylesheet() {
            return stylesheet;
        }

        public String label() {
            return label;
        }

        public Theme opposite() {
            return this == LIGHT ? DARK : LIGHT;
        }
    }

    private static final String KEY = "theme";

    private final Preferences preferences = Preferences.userNodeForPackage(ThemeService.class);

    public Theme load() {
        try {
            return Theme.valueOf(preferences.get(KEY, Theme.DARK.name()));
        } catch (IllegalArgumentException e) {
            return Theme.DARK;
        }
    }

    public void save(Theme theme) {
        preferences.put(KEY, theme.name());
    }
}
