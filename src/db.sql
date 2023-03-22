DROP
DATABASE IF EXISTS db_relationnelle_1;

CREATE
DATABASE db_relationnelle_1;

USE
db_relationnelle_1;

CREATE TABLE personnes
(
    id     INT         NOT NULL AUTO_INCREMENT,
    nom    VARCHAR(50) NOT NULL,
    prenom VARCHAR(50) NOT NULL,
    age    INT         NOT NULL,
    PRIMARY KEY (id)
);

DROP
DATABASE IF EXISTS  db_relationnelle_2;

CREATE
DATABASE db_relationnelle_2;

USE
db_relationnelle_2;

CREATE TABLE produits
(
    id          INT            NOT NULL AUTO_INCREMENT,
    nom         VARCHAR(50)    NOT NULL,
    description VARCHAR(255)   NOT NULL,
    prix        DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (id)
);

USE
db_relationnelle_1;

-- Ajout de quelques enregistrements dans la table "personnes"
INSERT INTO personnes (nom, prenom, age)
VALUES ('Doe', 'John', 30);
INSERT INTO personnes (nom, prenom, age)
VALUES ('Doe', 'Jane', 28);
INSERT INTO personnes (nom, prenom, age)
VALUES ('Smith', 'Bob', 35);
INSERT INTO personnes (nom, prenom, age)
VALUES ('Smith', 'Alice', 25);

USE
db_relationnelle_2;

-- Ajout de quelques enregistrements dans la table "produits"
INSERT INTO produits (nom, description, prix)
VALUES ('Ordinateur portable', 'PC portable haut de gamme', 999.99);
INSERT INTO produits (nom, description, prix)
VALUES ('Smartphone', 'Téléphone intelligent dernière génération', 799.99);
INSERT INTO produits (nom, description, prix)
VALUES ('Tablette', 'Tablette tactile de 10 pouces', 299.99);
INSERT INTO produits (nom, description, prix)
VALUES ('Casque audio', 'Casque sans fil avec réduction de bruit', 199.99);