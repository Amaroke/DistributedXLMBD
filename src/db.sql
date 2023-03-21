CREATE DATABASE IF NOT EXISTS ustensiles;

USE ustensiles;

-- Table "cordes"
CREATE TABLE IF NOT EXISTS cordes (
                                      id INT PRIMARY KEY AUTO_INCREMENT,
                                      longueur FLOAT,
                                      epaisseur FLOAT,
                                      materiau VARCHAR(255)
    );

-- Table "tabourets"
CREATE TABLE IF NOT EXISTS tabourets (
                                         id INT PRIMARY KEY AUTO_INCREMENT,
                                         hauteur FLOAT,
                                         largeur FLOAT,
                                         bois VARCHAR(255),
    nb_pieds INT
    );