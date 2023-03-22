import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Cette classe permet d'initialiser une base de données en exécutant un script SQL.
 */
public class DBInitializer {

    /**
     * Méthode principale qui lit les informations de connexion à la base de données depuis un fichier de configuration,
     * établit la connexion et exécute le script SQL donné pour créer les tables.
     *
     * @param args les arguments de ligne de commande (non utilisés ici)
     * @throws Exception si une erreur survient lors de la lecture du fichier de configuration, de la connexion à
     *                   la base de données ou de l'exécution du script SQL
     */
    public static void main(String[] args) throws Exception {

        String configFilePath = "src/config.ini";
        String scriptFilePath = "src/db.sql";

        try (FileReader reader = new FileReader(configFilePath)) {
            Properties props = new Properties();
            props.load(reader);

            String db = props.getProperty("db");
            String dbUser = props.getProperty("dbUser");
            String dbPass = props.getProperty("dbPass");

            try (Connection conn = DriverManager.getConnection(db, dbUser, dbPass)) {
                Statement stmt = conn.createStatement();
                executeScript(stmt, scriptFilePath);
                System.out.println("Création des tables terminées !");
            }
        }
    }

    /**
     * Exécute le script SQL contenu dans le fichier donné en utilisant la connexion SQL donnée.
     *
     * @param stmt           l'objet Statement associé à la connexion SQL
     * @param scriptFilePath le chemin d'accès au fichier contenant le script SQL à exécuter
     * @throws IOException  si une erreur survient lors de la lecture du fichier contenant le script SQL
     * @throws SQLException si une erreur survient lors de l'exécution du script SQL
     */
    private static void executeScript(Statement stmt, String scriptFilePath) throws IOException, SQLException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(scriptFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        String[] commands = sb.toString().split(";");
        for (String command : commands) {
            command = command.trim();
            if (!command.isEmpty()) {
                stmt.execute(command);
            }
        }
    }
}
