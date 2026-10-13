package br.com.projeto.view.components;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;

import java.util.Optional;

/** Diálogos padronizados que herdam o tema (folhas de estilo) da janela principal. */
public final class Dialogs {

    private Dialogs() {
    }

    public static boolean confirm(Window owner, String title, String message, String confirmText) {
        ButtonType confirm = new ButtonType(confirmText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, confirm, cancel);
        alert.setHeaderText(title);
        prepare(alert, owner, "Confirmação");
        alert.getDialogPane().lookupButton(confirm).getStyleClass().add("danger-button");
        return alert.showAndWait().filter(confirm::equals).isPresent();
    }

    public static Optional<String> input(Window owner, String title, String label, String initialValue) {
        TextInputDialog dialog = new TextInputDialog(initialValue);
        dialog.setHeaderText(title);
        dialog.setContentText(label);
        prepare(dialog, owner, title);
        ((javafx.scene.control.Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Salvar");
        ((javafx.scene.control.Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancelar");
        dialog.getEditor().selectAll();
        return dialog.showAndWait().map(String::trim);
    }

    public static void error(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(title);
        prepare(alert, owner, "Erro");
        alert.showAndWait();
    }

    public static void info(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(title);
        prepare(alert, owner, "Informação");
        alert.showAndWait();
    }

    public static void prepare(Dialog<?> dialog, Window owner, String windowTitle) {
        dialog.setTitle(windowTitle);
        if (owner != null) {
            dialog.initOwner(owner);
            if (owner.getScene() != null) {
                dialog.getDialogPane().getStylesheets().setAll(owner.getScene().getStylesheets());
            }
        }
        dialog.getDialogPane().getStyleClass().add("app-dialog");
    }
}
