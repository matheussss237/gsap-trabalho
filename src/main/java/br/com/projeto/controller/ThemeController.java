package br.com.projeto.controller;

import br.com.projeto.view.Components;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.Button;

import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Tema claro/escuro: o código Java troca a folha de estilo (light.css ↔ dark.css) em todas
 * as janelas abertas. base.css é comum aos dois. A escolha fica salva no computador.
 */
public class ThemeController {

    public enum Theme {
        LIGHT("/css/light.css"), DARK("/css/dark.css");

        final String stylesheet;

        Theme(String stylesheet) {
            this.stylesheet = stylesheet;
        }
    }

    private final Preferences preferences = Preferences.userNodeForPackage(ThemeController.class);
    private final ObjectProperty<Theme> theme = new SimpleObjectProperty<>(loadSaved());

    /** Aplica o tema à cena e a mantém sincronizada. Retorna a ação que desfaz o vínculo. */
    public Runnable register(Scene scene) {
        apply(scene, theme.get());
        ChangeListener<Theme> listener = (obs, old, value) -> apply(scene, value);
        theme.addListener(listener);
        return () -> theme.removeListener(listener);
    }

    public void setTheme(Theme value) {
        theme.set(value);
        preferences.put("theme", value.name());
    }

    public void toggle() {
        setTheme(theme.get() == Theme.DARK ? Theme.LIGHT : Theme.DARK);
    }

    public Theme current() {
        return theme.get();
    }

    /** Botão de alternância: sol no tema escuro, lua no claro. */
    public void bindToggleButton(Button button) {
        ChangeListener<Theme> listener = (obs, old, value) ->
                button.setGraphic(Components.icon(value == Theme.DARK ? Components.SUN : Components.MOON, 15));
        listener.changed(theme, null, theme.get());
        // o botão guarda o listener; a propriedade só o referencia fracamente (sem vazamento ao trocar de tela)
        button.getProperties().put("themeListener", listener);
        theme.addListener(new WeakChangeListener<>(listener));
        button.setOnAction(e -> toggle());
    }

    private void apply(Scene scene, Theme value) {
        scene.getStylesheets().setAll(url("/css/base.css"), url(value.stylesheet));
    }

    private String url(String resource) {
        return Objects.requireNonNull(getClass().getResource(resource), resource).toExternalForm();
    }

    private Theme loadSaved() {
        try {
            return Theme.valueOf(preferences.get("theme", Theme.DARK.name()));
        } catch (IllegalArgumentException e) {
            return Theme.DARK;
        }
    }
}
