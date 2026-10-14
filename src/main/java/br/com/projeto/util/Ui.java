package br.com.projeto.util;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Utilidades de interface: tarefas em segundo plano, mensagens de erro amigáveis,
 * diálogos padronizados e formatação de datas.
 */
public final class Ui {

    private static final System.Logger LOG = System.getLogger("Lumi");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "lumi-worker");
        thread.setDaemon(true);
        return thread;
    });

    private Ui() {
    }

    /**
     * Executa operações demoradas (MongoDB, Groq) fora da JavaFX Application Thread para a
     * interface não congelar. Os callbacks rodam de volta na thread do JavaFX.
     */
    public static <T> void async(Callable<T> work, Consumer<T> onSuccess, Consumer<String> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(friendly(task.getException())));
        EXECUTOR.submit(task);
    }

    public static void shutdown() {
        EXECUTOR.shutdownNow();
    }

    /** Traduz exceções em mensagens compreensíveis; o detalhe técnico vai só para o log. */
    public static String friendly(Throwable error) {
        Throwable current = error;
        while (!(current instanceof AppException) && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        if (current instanceof AppException app) {
            if (app.getCause() != null) {
                LOG.log(Level.WARNING, app.getMessage(), app.getCause());
            }
            return app.getMessage();
        }
        LOG.log(Level.ERROR, "Erro inesperado", error);
        return "Ocorreu um erro inesperado. Tente novamente em instantes.";
    }

    /** "14:32" para hoje, "12/10/2026 14:32" para outros dias. */
    public static String formatDate(Instant instant) {
        if (instant == null) {
            return "";
        }
        var zoned = instant.atZone(ZoneId.systemDefault());
        return zoned.toLocalDate().equals(LocalDate.now()) ? TIME.format(zoned) : DATE_TIME.format(zoned);
    }

    // ---------- diálogos (herdam o tema da janela dona) ----------

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
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Salvar");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancelar");
        return dialog.showAndWait().map(String::trim);
    }

    public static void error(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(title);
        prepare(alert, owner, "Erro");
        alert.showAndWait();
    }

    private static void prepare(Dialog<?> dialog, Window owner, String windowTitle) {
        dialog.setTitle(windowTitle);
        if (owner != null) {
            dialog.initOwner(owner);
            if (owner.getScene() != null) {
                dialog.getDialogPane().getStylesheets().setAll(owner.getScene().getStylesheets());
            }
        }
    }
}
