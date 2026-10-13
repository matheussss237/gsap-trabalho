package br.com.projeto.view.components;

import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;

/** Botão que mostra um indicador de progresso enquanto a operação está em andamento. */
public class LoadingButton extends Button {

    private final String idleText;
    private final ProgressIndicator spinner = new ProgressIndicator();

    public LoadingButton(String text) {
        super(text);
        this.idleText = text;
        spinner.getStyleClass().add("button-spinner");
        spinner.setPrefSize(16, 16);
        spinner.setMaxSize(16, 16);
        getStyleClass().add("primary-button");
        setMaxWidth(Double.MAX_VALUE);
    }

    public void setLoading(boolean loading, String loadingText) {
        setDisable(loading);
        setGraphic(loading ? spinner : null);
        setText(loading ? loadingText : idleText);
    }

    public void setLoading(boolean loading) {
        setLoading(loading, "Aguarde…");
    }
}
