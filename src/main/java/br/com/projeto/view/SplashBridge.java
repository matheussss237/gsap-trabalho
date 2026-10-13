package br.com.projeto.view;

/**
 * Objeto exposto ao JavaScript da splash como "window.javaBridge".
 * Os métodos precisam ser públicos para serem chamados pelo WebEngine.
 */
public class SplashBridge {

    private final Runnable onIntroDone;
    private final Runnable onFinish;

    public SplashBridge(Runnable onIntroDone, Runnable onFinish) {
        this.onIntroDone = onIntroDone;
        this.onFinish = onFinish;
    }

    /** Chamado pelo GSAP ao terminar a animação de entrada. */
    public void introDone() {
        onIntroDone.run();
    }

    /** Chamado pelo GSAP ao terminar a animação de saída. */
    public void finish() {
        onFinish.run();
    }
}
