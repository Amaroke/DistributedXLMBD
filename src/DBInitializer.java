import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DBInitializer {

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
