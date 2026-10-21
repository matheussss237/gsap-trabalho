package com.matheushenrique.lumi.controller;

import com.matheushenrique.lumi.view.Componentes;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.Button;

import java.util.prefs.Preferences;

// tema claro/escuro: o Java troca a folha de estilo (light.css <-> dark.css) em todas as janelas
public class TemaController {

    public enum Tema {
        CLARO("/css/light.css"),
        ESCURO("/css/dark.css");

        private final String css;

        Tema(String css) {
            this.css = css;
        }
    }

    private final Preferences preferencias = Preferences.userNodeForPackage(TemaController.class);
    private final ObjectProperty<Tema> tema = new SimpleObjectProperty<>(temaSalvo());

    // aplica o tema na tela e continua atualizando quando trocar; devolve o que desfaz isso
    public Runnable aplicar(Scene cena) {
        trocarCss(cena, tema.get());
        ChangeListener<Tema> ouvinte = (obs, antigo, novo) -> trocarCss(cena, novo);
        tema.addListener(ouvinte);
        return () -> tema.removeListener(ouvinte);
    }

    public void mudarTema(Tema novo) {
        tema.set(novo);
        preferencias.put("tema", novo.name()); // fica salvo no computador
    }

    public void alternar() {
        mudarTema(tema.get() == Tema.ESCURO ? Tema.CLARO : Tema.ESCURO);
    }

    // botão de trocar tema: sol no escuro, lua no claro
    public void configurarBotao(Button botao) {
        ChangeListener<Tema> ouvinte = (obs, antigo, novo) ->
                botao.setGraphic(Componentes.icone(novo == Tema.ESCURO ? Componentes.SOL : Componentes.LUA, 15));
        ouvinte.changed(tema, null, tema.get());
        botao.getProperties().put("ouvinteTema", ouvinte); // evita vazar memória quando a tela fecha
        tema.addListener(new WeakChangeListener<>(ouvinte));
        botao.setOnAction(e -> alternar());
    }

    private void trocarCss(Scene cena, Tema novo) {
        cena.getStylesheets().setAll(
                getClass().getResource("/css/base.css").toExternalForm(),
                getClass().getResource(novo.css).toExternalForm());
    }

    private Tema temaSalvo() {
        try {
            return Tema.valueOf(preferencias.get("tema", Tema.ESCURO.name()));
        } catch (IllegalArgumentException e) {
            return Tema.ESCURO;
        }
    }
}
