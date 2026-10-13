package br.com.projeto.support;

import javafx.application.Application;

/** Inicia o UiPreview com o JavaFX no classpath. */
public final class UiPreviewLauncher {
    public static void main(String[] args) {
        Application.launch(UiPreview.class, args);
    }
}
