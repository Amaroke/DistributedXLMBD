/**
 * Cette classe représente l'état partagé entre plusieurs threads. Elle permet d'indiquer si l'état est prêt ou non
 * et de synchroniser les threads qui doivent attendre que l'état soit prêt.
 */
public class SharedState {
    private boolean ready = false;

    /**
     * Met l'état à "prêt" et réveille tous les threads qui attendent sur cet état.
     */
    public synchronized void setReady() {
        ready = true;
        notifyAll();
    }

    /**
     * Met l'état à "non prêt" et réveille tous les threads qui attendent sur cet état.
     */
    public synchronized void setWait() {
        ready = false;
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
}