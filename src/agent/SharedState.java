package agent;

/**
 * Cette classe représente l'état partagé entre plusieurs threads. Elle permet d'indiquer si l'état est prêt ou non
 * et de synchroniser les threads qui doivent attendre que l'état soit prêt.
 */
public class SharedState {
    private boolean ready = false;
    private boolean keyExchanged = false;
    private boolean resultSigned = false;

    /**
     * Met l'état à "prêt" et réveille tous les threads qui attendent sur cet état.
     */
    public synchronized void setReady() {
        ready = true;
        notifyAll();
    }

    /**
     * Met l'état à "clé échangée" et réveille tous les threads qui attendent sur cet état.
     */
    public synchronized void setKeyExchanged() {
        keyExchanged = true;
        notifyAll();
    }

    /**
     * Met l'état à "résultat signé" et réveille tous les threads qui attendent sur cet état.
     */
    public synchronized void setResultSigned() {
        resultSigned = true;
        notifyAll();
    }

    /**
     * Attend que l'état soit "prêt".
     *
     * @throws InterruptedException si l'attente est interrompue
     */
    public synchronized void waitForReady() throws InterruptedException {
        while (!ready) {
            wait();
        }
    }

    /**
     * Attend que l'état soit "clé échangée".
     *
     * @throws InterruptedException si l'attente est interrompue
     */
    public synchronized void waitForKeyExchanged() throws InterruptedException {
        while (!keyExchanged) {
            wait();
        }
    }

    /**
     * Attend que l'état soit "résultat signé".
     *
     * @throws InterruptedException si l'attente est interrompue
     */
    public synchronized void waitForResultSigned() throws InterruptedException {
        while (!resultSigned) {
            wait();
        }
    }
}