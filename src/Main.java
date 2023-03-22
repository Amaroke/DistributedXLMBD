/**
 * Cette classe contient la méthode principale qui initialise et lance les threads {@link Agent}.
 * Une fois que les threads ont terminé leur travail, la méthode principale permet d'échanger les clés publiques
 * entre les deux agents.
 */
public class Main {
    /**
     * Initialise et lance les threads {@link Agent}, attend que les threads aient terminé leur travail, puis échange
     * les clés publiques entre les deux agents.
     *
     * @param args les arguments de la ligne de commande (non utilisés dans cette méthode)
     */
    public static void main(String[] args) {
        SharedState sharedState = new SharedState();
        Agent agent1 = new Agent(sharedState, true);
        Agent agent2 = new Agent(sharedState, false);

        agent1.start();
        agent2.start();

        try {
            agent1.join();
            agent2.join();

            agent1.exchangeKeys(agent2);
            agent2.exchangeKeys(agent1);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
