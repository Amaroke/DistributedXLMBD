package agent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.sql.*;
import java.util.Collections;
import java.util.Properties;

/**
 * La classe Agent représente un agent de communication qui peut être soit un émetteur, soit un destinataire.
 * Chaque agent possède une paire de clés RSA (publique/privée) pour la signature/le chiffrement des messages.
 * Les agents peuvent échanger leur clé publique afin de pouvoir communiquer de manière sécurisée.
 * Chaque agent est exécuté dans un thread distinct et peut interagir avec un objet partagé de type SharedState.
 */
public class Agent extends Thread {
    /**
     * La paire de clés RSA (publique/privée) de l'agent pour la signature/le chiffrement des messages.
     */
    private final KeyPair keyPair;
    /**
     * La base de données de l'agent.
     */
    private final String database;
    /**
     * La clé publique de l'autre agent avec qui cet agent communique.
     */
    private PublicKey otherAgentPublicKey;
    /**
     * L'objet SharedState qui est partagé entre tous les threads.
     */
    public final SharedState sharedState;
    /**
     * Indique si l'agent est un émetteur ou un destinataire.
     */
    public final boolean sender;
    /**
     * Le fichier à envoyer.
     */
    public final String fileToSend;

    /**
     * Constructeur de la classe agent.Agent.
     *
     * @param sharedState l'objet agent.SharedState partagé entre tous les threads
     * @param sender      true si l'agent est un émetteur, false sinon
     * @param database    le nom de la base de données de l'agent
     */
    public Agent(SharedState sharedState, boolean sender, String database, String fileToSend) {
        this.sharedState = sharedState;
        this.database = database;
        this.sender = sender;
        this.fileToSend = fileToSend;
        // Générer une paire de clés RSA pour cet agent
        KeyPairGenerator keyPairGenerator;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();
    }

    /**
     * Permet à deux agents d'échanger leur clé publique respective.
     *
     * @param otherAgent l'autre agent avec qui échanger la clé publique
     */
    public void exchangeKeys(Agent otherAgent) {
        // Récupérer la clé publique de l'autre agent et l'enregistrer localement
        this.otherAgentPublicKey = otherAgent.getPublicKey();
    }

    /**
     * Permet de récupérer la clé publique de l'agent.
     *
     * @return la clé publique de l'agent
     */
    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    /**
     * La méthode run() est exécutée lorsqu'un thread est lancé.
     * Elle permet de gérer les actions à effectuer par le thread en fonction de sa nature, soit "sender" soit "receiver".
     * Si le thread est de type "sender", il attend que sa clé publique soit échangée, signe le document XML à envoyer,
     * le transmet au thread "receiver", attend que le résultat signé lui, soit retourné, puis vérifie la signature
     * du document XML reçu.
     * Si le thread est de type "receiver", il attend que la clé publique de l'expéditeur soit échangée,
     * attend que le thread "sender" soit prêt, vérifie la signature du document XML reçu,
     * extrait la requête SQL contenue dans le document XML, l'exécute, signe le document XML contenant le résultat
     * et le transmet au thread "sender".
     */
    @Override
    public void run() {

        try {
            if (sender) {
                // Le thread "sender" attends que sa clé publique soit échangé.
                sharedState.waitForKeyExchanged();
                System.out.println("L'envoyeur a récupéré la publique du récepteur !\n");
                // Puis le signe et l'envoie.
                signerDocumentXML("./requests/" + fileToSend, "./requests/signed/" + fileToSend);
                System.out.println("Document XML chargé et signé par l'envoyeur !\n");
                sharedState.setReady();
                sharedState.waitForResultSigned();
                System.out.println("L'envoyeur a reçu le document de réponse XML signé !\n");
                if (verifierSignatureDocumentXML("./requests/results/signed/" + fileToSend)) {
                    System.out.println("La signature de la réponse est correcte !");
                } else {
                    System.out.println("La signature de la réponse est incorrecte !");
                    System.out.println("FIN DU PROGRAMME !");
                }
            } else {
                // L'autre thread attend que sa clé publique soit échangée.
                sharedState.waitForKeyExchanged();
                System.out.println("Le récepteur a récupéré la publique de l'envoyeur !\n");
                // L'autre thread attend que le thread "sender" soit prêt.
                sharedState.waitForReady();
                System.out.println("Le récepteur a reçu le document XML signé !\n");
                if (verifierSignatureDocumentXML("./requests/signed/" + fileToSend)) {
                    System.out.println("La signature du document envoyé est correcte !\n");
                    String requete = extraireRequeteXML("./requests/signed/" + fileToSend);
                    System.out.println("Requête : \"" + requete + "\" extraite du document XML signé !\n");
                    executerRequeteSQL(requete);
                    System.out.println("Requête SQL exécutée avec succès !\n");
                    signerDocumentXML("./requests/results/" + fileToSend, "./requests/results/signed/" + fileToSend);
                    System.out.println("Résultats enregistrés dans un fichier XML, et fichier signé !\n");
                    System.out.println("Voici les résultats :\n" + afficherResultatsXML("./requests/results/" + fileToSend));
                    sharedState.setResultSigned();
                } else {
                    System.out.println("La signature est incorrecte !\n");
                    System.out.println("FIN DU PROGRAMME !");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Charge un document XML à partir d'un fichier donné et retourne l'objet Document correspondant.
     *
     * @param filePath le chemin d'accès au fichier XML à charger
     * @return l'objet Document correspondant au fichier XML chargé
     * @throws Exception si une erreur survient lors de la lecture ou du parsing du fichier XML
     */
    public static Document chargerDocumentXML(String filePath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new File(filePath));
    }

    /**
     * Signe le document XML donné en utilisant la paire de clés RSA associée à cet Agent.
     *
     * @param inputFilePath  le chemin d'accès au fichier XML à signer
     * @param outputFilePath le chemin d'accès au fichier XML dans lequel enregistrer le document XML signé
     * @throws Exception si une erreur survient lors de la signature du document
     */
    private void signerDocumentXML(String inputFilePath, String outputFilePath) throws Exception {
        Document xmlDocument = chargerDocumentXML(inputFilePath);
        DOMSignContext signContext = new DOMSignContext(keyPair.getPrivate(), xmlDocument.getDocumentElement());
        XMLSignatureFactory sigFactory = XMLSignatureFactory.getInstance("DOM");
        Reference ref = sigFactory.newReference("", sigFactory.newDigestMethod(DigestMethod.SHA256, null), Collections.singletonList(sigFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)), null, null);
        SignedInfo signedInfo = sigFactory.newSignedInfo(sigFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null), sigFactory.newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null), Collections.singletonList(ref));
        KeyInfoFactory keyInfoFactory = sigFactory.getKeyInfoFactory();
        KeyValue keyValue = keyInfoFactory.newKeyValue(keyPair.getPublic());
        KeyInfo keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(keyValue));
        XMLSignature signature = sigFactory.newXMLSignature(signedInfo, keyInfo);
        signature.sign(signContext);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(xmlDocument);
        StreamResult result = new StreamResult(new File(outputFilePath));
        transformer.transform(source, result);
    }

    /**
     * Vérifie la signature d'un document XML donné.
     *
     * @param inputPath le chemin d'accès au fichier XML à vérifier
     * @return true si la signature est valide, false sinon
     */
    public boolean verifierSignatureDocumentXML(String inputPath) {
        try {
            // Charger le document XML
            String xml = Files.readString(Paths.get(inputPath));
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            // Extraire la signature du document
            NodeList signatures = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
            if (signatures.getLength() == 0) {
                System.err.println("Le document ne contient pas de signature");
                return false;
            }

            // Charger la clé publique du destinataire
            PublicKey cle = otherAgentPublicKey;

            // Créer un validateur de signature
            DOMValidateContext valContext = new DOMValidateContext(cle, signatures.item(0));

            // Valider la signature
            XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");
            XMLSignature signature = factory.unmarshalXMLSignature(valContext);

            return signature.validate(valContext);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Extrait la requête SQL d'un document XML donné.
     *
     * @param inputPath le chemin d'accès au fichier XML à extraire
     * @return la requête SQL extraite du document XML
     */
    public String extraireRequeteXML(String inputPath) {
        try {
            String xml = Files.readString(Paths.get(inputPath));
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            Document doc = dBuilder.parse(input);
            doc.getDocumentElement().normalize();
            NodeList champsNodes = doc.getElementsByTagName("CHAMP");
            NodeList tablesNodes = doc.getElementsByTagName("TABLE");
            Node conditionNode = doc.getElementsByTagName("CONDITION").item(0);
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ");
            for (int i = 0; i < champsNodes.getLength(); i++) {
                sql.append(champsNodes.item(i).getTextContent()).append(i == champsNodes.getLength() - 1 ? " " : ", ");
            }
            sql.append("FROM ");
            for (int i = 0; i < tablesNodes.getLength(); i++) {
                sql.append(tablesNodes.item(i).getTextContent()).append(i == tablesNodes.getLength() - 1 ? " " : ", ");
            }
            if (conditionNode != null) {
                sql.append("WHERE ").append(conditionNode.getTextContent());
            }
            return sql.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Exécute une requête SQL donnée et enregistre le résultat dans un fichier XML.
     *
     * @param sql la requête SQL à exécuter
     */
    public void executerRequeteSQL(String sql) {
        String configFilePath = "src/config.ini";

        try (FileReader reader = new FileReader(configFilePath)) {
            Properties props = new Properties();
            props.load(reader);

            String db = props.getProperty("db");
            String dbUser = props.getProperty("dbUser");
            String dbPass = props.getProperty("dbPass");

            try (Connection conn = DriverManager.getConnection(db, dbUser, dbPass)) {
                Statement stmt = conn.createStatement();
                stmt.execute("USE " + this.database);
                ResultSet rs = stmt.executeQuery(sql);
                resultSetToXML(rs, "./requests/results/" + fileToSend);
            } catch (SQLException | ParserConfigurationException | TransformerException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convertit un ResultSet en fichier XML.
     *
     * @param resultSet le ResultSet à convertir
     * @param filename  le nom du fichier XML à créer
     * @throws SQLException                 si une erreur SQL survient
     * @throws ParserConfigurationException si une erreur de configuration du parseur survient
     * @throws TransformerException         si une erreur de transformation survient
     * @throws FileNotFoundException        si le fichier n'est pas trouvé
     */
    public void resultSetToXML(ResultSet resultSet, String filename) throws SQLException, ParserConfigurationException, TransformerException, FileNotFoundException {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element resultat = document.createElement("RESULTAT");
        document.appendChild(resultat);

        Element tuples = document.createElement("TUPLES");
        resultat.appendChild(tuples);

        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (resultSet.next()) {
            Element tuple = document.createElement("TUPLE");
            tuples.appendChild(tuple);

            for (int i = 1; i <= columnCount; i++) {
                Object value = resultSet.getObject(i);

                Element champ = document.createElement("CHAMP");
                champ.appendChild(document.createTextNode(value.toString()));
                tuple.appendChild(champ);
            }
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(new FileOutputStream(filename));
        transformer.transform(source, result);
    }


    /**
     * Affiche les résultats d'un fichier XML contenant des tuples et des champs.
     *
     * @param filename Le nom du fichier XML.
     * @return Une chaîne de caractères représentant les résultats.
     * @throws Exception Si une erreur se produit lors de l'analyse du fichier XML.
     */
    public static String afficherResultatsXML(String filename) throws Exception {

        StringBuilder res = new StringBuilder(1024);

        File inputFile = new File(filename);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputFile);
        doc.getDocumentElement().normalize();

        NodeList nodeList = doc.getElementsByTagName("TUPLE");

        for (int temp = 0; temp < nodeList.getLength(); temp++) {
            Node node = nodeList.item(temp);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;

                NodeList champs = element.getElementsByTagName("CHAMP");
                res.append("Tuple ").append(temp + 1).append(": ");
                for (int i = 0; i < champs.getLength(); i++) {
                    String champ = champs.item(i).getTextContent();
                    res.append(champ).append(" ");
                }
                res.append("\n");
            }
        }
        return res.toString();
    }


}