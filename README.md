# Fonctionnement de l'application

Avant toute chose, une vidéo de demonstration est à votre disposition à la racine du projet.

Pour vérifier que le code compile : javac *.java depuis le dossier ./src/agent
Pour initialiser la base de données :
java -jar DistributedXMLDB_DB.jar
Pour exécuter le code avec les différents exemples (possible d'en ajouter) :
* java -jar DistributedXMLDB.jar 1 recupererDoe1.xml
* java -jar DistributedXMLDB.jar 1 simulationInjectionSQL1.xml (Ne fonctionne pas, c'est normal puisqu'il s'agit d'une tentative d'attaque du système).
* java -jar DistributedXMLDB.jar 1 recupererAgeSup301.xml
* java -jar DistributedXMLDB.jar 2 recupererProduitsHautDeGamme2.xml