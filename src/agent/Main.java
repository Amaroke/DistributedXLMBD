package agent;

import java.io.File;

/**
 * Cette classe contient la méthode principale qui initialise et lance les threads {@link Agent}.
 * Une fois que les threads ont terminé leur travail, la méthode principale permet d'échanger les clés publiques
 * entre les deux agents.
 */
public class Main {
    /**
     * Méthode principale qui initialise et lance les threads {@link Agent}.
     *
     * @param args les arguments de ligne de commande (non utilisés ici)
     */
    public static void main(String[] args) {
        // Création des dossiers nécessaires
        String baseDir = "requests/";
        String signedDir = baseDir + "signed/";
        String resultsDir = baseDir + "results/";
        String signedResultsDir = resultsDir + "signed/";

        File signedFolder = new File(signedDir);
        File resultsFolder = new File(resultsDir);
        File signedResultsFolder = new File(signedResultsDir);

        if (!signedFolder.exists()) {
            signedFolder.mkdir();
        }
        if (!resultsFolder.exists()) {
            resultsFolder.mkdir();
        }
        if (!signedResultsFolder.exists()) {
            signedResultsFolder.mkdir();
        }

        SharedState sharedState = new SharedState();
        Agent agent1 = new Agent(sharedState, (args[0].equals("1")), "db_relationnelle_2", args[1]);
        Agent agent2 = new Agent(sharedState, (!args[0].equals("1")), "db_relationnelle_1", args[1]);
        agent1.exchangeKeys(agent2);
        agent2.exchangeKeys(agent1);
        sharedState.setKeyExchanged();
        agent1.start();
        agent2.start();
    }

}
