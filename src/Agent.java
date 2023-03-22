import org.w3c.dom.Document;

import java.security.NoSuchAlgorithmException;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Collections;

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
     * Constructeur de la classe Agent.
     *
     * @param sharedState l'objet SharedState partagé entre tous les threads
     * @param sender      true si l'agent est un émetteur, false sinon
     */
    public Agent(SharedState sharedState, boolean sender) {
        this.sharedState = sharedState;
        this.sender = sender;
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
        // Enregistrer la clé publique de cet agent auprès de l'autre agent
        otherAgentPublicKey = keyPair.getPublic();

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
     * Si l'agent est un émetteur, il charge un document XML, le signe et l'affiche.
     * Si l'agent est un destinataire, il attend que l'émetteur soit prêt avant d'afficher un message de confirmation.
     */
    @Override
    public void run() {
        try {
            if (sender) {
                // Le thread "sender" charge le document XML.
                Document xmlDocument = chargerDocumentXML("./requests/test.xml");
                // Puis le signe.
                signerDocumentXML(xmlDocument);
                afficherDocumentXML(xmlDocument);
                System.out.println("\nDocument XML chargé et signé !\n");
                sharedState.setReady();
            } else {
                // L'autre thread attend que le thread "sender" soit prêt.
                sharedState.waitForReady();
                System.out.println("Tous les threads sont prêts !");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Affiche le contenu d'un document XML dans la console en utilisant une indentation.
     *
     * @param xmlDocument le document XML à afficher
     */
    public static void afficherDocumentXML(Document xmlDocument) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{https://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(xmlDocument);
            StreamResult console = new StreamResult(System.out);
            transformer.transform(source, console);
        } catch (TransformerException e) {
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
     * @param xmlDocument le document XML à signer
     * @throws Exception si une erreur survient lors de la signature du document
     */
    private void signerDocumentXML(Document xmlDocument) throws Exception {
        DOMSignContext signContext = new DOMSignContext(keyPair.getPrivate(), xmlDocument.getDocumentElement());
        XMLSignatureFactory sigFactory = XMLSignatureFactory.getInstance("DOM");
        Reference ref = sigFactory.newReference("", sigFactory.newDigestMethod(DigestMethod.SHA1, null), Collections.singletonList(sigFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)), null, null);
        SignedInfo signedInfo = sigFactory.newSignedInfo(sigFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null), sigFactory.newSignatureMethod(SignatureMethod.RSA_SHA1, null), Collections.singletonList(ref));
        KeyInfoFactory keyInfoFactory = sigFactory.getKeyInfoFactory();
        KeyValue keyValue = keyInfoFactory.newKeyValue(keyPair.getPublic());
        KeyInfo keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(keyValue));
        XMLSignature signature = sigFactory.newXMLSignature(signedInfo, keyInfo);
        signature.sign(signContext);
    }

}