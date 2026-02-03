CREATE EXTENSION IF NOT EXISTS postgis;


CREATE TABLE utilisateurs (
                              id SERIAL PRIMARY KEY,
                              email VARCHAR(255) NOT NULL UNIQUE,
                              nom VARCHAR(100),
                              doc_id VARCHAR(128),
                              mot_de_passe_hash TEXT,
                              firebase_uid VARCHAR(128) UNIQUE,
                              tentatives_echouees INT DEFAULT 0,
                              bloque_jusqua TIMESTAMP,
                              actif BOOLEAN DEFAULT TRUE,
                              version INT DEFAULT 1,
                              date_creation TIMESTAMP DEFAULT now(),
                              date_mis_a_jour TIMESTAMP,
                              date_suppression TIMESTAMP
);

CREATE TABLE roles (
                       id SERIAL PRIMARY KEY,
                       nom VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE entreprises (
                             id SERIAL PRIMARY KEY,
                             nom VARCHAR(100) NOT NULL
);

CREATE TABLE statuts_signalement (
                                     id SERIAL PRIMARY KEY,
                                     niveau INT,
                                     nom VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE parametres (
                            id SERIAL PRIMARY KEY,
                            nom VARCHAR(50) NOT NULL UNIQUE,
                            valeur VARCHAR(255) NOT NULL,
                            type VARCHAR(50),
                            date_creation TIMESTAMP DEFAULT now(),
                            date_mis_a_jour TIMESTAMP
);

CREATE TABLE type_signalement (
                                  id SERIAL PRIMARY KEY,
                                  firebase_uid VARCHAR(100) UNIQUE,
                                  nom VARCHAR(50) NOT NULL UNIQUE,
                                  icone VARCHAR(50)
);

CREATE TABLE signalements (
                              id SERIAL PRIMARY KEY,
                              firebase_uid VARCHAR(100),
                              description TEXT,
                              surface_m2 DOUBLE PRECISION,
                              budget DOUBLE PRECISION,
                              points GEOMETRY,
                              id_entreprise INT REFERENCES entreprises(id),
                              id_type_signalement INT REFERENCES type_signalement(id),
                              version INT DEFAULT 1,
                              date_creation TIMESTAMP DEFAULT now(),
                              date_mis_a_jour TIMESTAMP,
                              date_suppression TIMESTAMP
);

CREATE TABLE roles_utilisateur (
                                   id SERIAL PRIMARY KEY,
                                   id_utilisateur INT NOT NULL,
                                   id_role INT NOT NULL,
                                   date_creation TIMESTAMP DEFAULT now(),
                                   FOREIGN KEY (id_utilisateur) REFERENCES utilisateurs(id),
                                   FOREIGN KEY (id_role) REFERENCES roles(id)
);

CREATE TABLE historique_statut_signalement (
                                               id SERIAL PRIMARY KEY,
                                               id_signalement INT NOT NULL,
                                               id_statut_signalement INT NOT NULL,
                                               date_creation TIMESTAMP DEFAULT now(),
                                               FOREIGN KEY (id_signalement) REFERENCES signalements(id),
                                               FOREIGN KEY (id_statut_signalement) REFERENCES statuts_signalement(id)
);

CREATE TABLE journal (
                         id SERIAL PRIMARY KEY,
                         id_entite TEXT,
                         type_entite TEXT,
                         operation TEXT CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
                         donnees JSONB,
                         version INT,
                         date_creation TIMESTAMP DEFAULT now(),
                         synchronise BOOLEAN DEFAULT FALSE
);

INSERT INTO roles (nom) VALUES
                            ('USER'),
                            ('MANAGER');

INSERT INTO statuts_signalement (niveau, nom) VALUES
                                                  (1, 'NOUVEAU'),
                                                  (2, 'EN_COURS'),
                                                  (3, 'TERMINE');

INSERT INTO parametres (nom, valeur, type, date_creation) VALUES
                                                              ('MAX_FAILED_ATTEMPTS', '3', 'INTEGER', now()),
                                                              ('BLOCK_DURATION_MINUTES', '30', 'INTEGER', now()),
                                                              ('DEFAULT_ROLE', 'USER', 'STRING', now()),
                                                              ('DEFAULT_STATUS', 'ACTIF', 'STRING', now()),
                                                              ('SESSION_DURATION_MINUTES', '30', 'INTEGER', now());


