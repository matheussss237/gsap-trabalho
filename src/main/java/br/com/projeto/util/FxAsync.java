package br.com.projeto.util;

import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Executa operações demoradas (MongoDB, Groq) fora da JavaFX Application Thread,
 * para que a interface não congele. Os callbacks voltam a rodar na thread do JavaFX.
 */
public final class FxAsync {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "nexus-worker");
        thread.setDaemon(true);
        return thread;
    });

    private FxAsync() {
    }

    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<String> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(ErrorMessages.friendly(task.getException())));
        EXECUTOR.submit(task);
    }

    public static void shutdown() {
        EXECUTOR.shutdownNow();
    }
}
