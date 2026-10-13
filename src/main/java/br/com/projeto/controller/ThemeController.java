package br.com.projeto.controller;

import br.com.projeto.service.ThemeService;
import br.com.projeto.service.ThemeService.Theme;
import br.com.projeto.view.components.Icons;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.Button;

import java.util.Objects;

/**
 * Troca de tema: o código Java remove a folha de estilo atual (light.css / dark.css)
 * e adiciona a nova em todas as cenas registradas. base.css é comum aos dois temas.
 */
public class ThemeController {

    private static final String BASE_CSS = "/css/base.css";

    private final ThemeService themeService;
    private final ObjectProperty<Theme> theme = new SimpleObjectProperty<>();

    public ThemeController(ThemeService themeService) {
        this.themeService = themeService;
        this.theme.set(themeService.load());
    }

    /**
     * Aplica o tema à cena e a mantém sincronizada com futuras trocas.
     * Retorna uma ação que remove o vínculo (usada ao fechar janelas secundárias).
     */
    public Runnable register(Scene scene) {
        applyTo(scene, theme.get());
        ChangeListener<Theme> listener = (obs, old, value) -> applyTo(scene, value);
        theme.addListener(listener);
        return () -> theme.removeListener(listener);
    }

    public void setTheme(Theme value) {
        theme.set(value);
        themeService.save(value);
    }

    public void toggle() {
        setTheme(theme.get().opposite());
    }

    public Theme current() {
        return theme.get();
    }

    public ObjectProperty<Theme> themeProperty() {
        return theme;
    }

    /** Configura um botão de alternância (ícone de sol no escuro, lua no claro). */
    public void bindToggleButton(Button button) {
        updateIcon(button, theme.get());
        // o botão guarda o listener; a propriedade só o referencia fracamente (sem vazamento ao trocar de tela)
        ChangeListener<Theme> listener = (obs, old, value) -> updateIcon(button, value);
        button.getProperties().put("themeListener", listener);
        theme.addListener(new WeakChangeListener<>(listener));
        button.setOnAction(e -> toggle());
    }

    private void updateIcon(Button button, Theme value) {
        button.setGraphic(Icons.create(value == Theme.DARK ? Icons.SUN : Icons.MOON, 15));
    }

    private void applyTo(Scene scene, Theme value) {
        scene.getStylesheets().setAll(
                url(BASE_CSS),
                url(value.stylesheet()));
    }

    private String url(String resource) {
        return Objects.requireNonNull(getClass().getResource(resource), "CSS não encontrado: " + resource)
                .toExternalForm();
    }
}
