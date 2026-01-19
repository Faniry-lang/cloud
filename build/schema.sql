CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE utilisateurs (
                              id SERIAL PRIMARY KEY,
                              email VARCHAR(255) NOT NULL UNIQUE,
                              nom VARCHAR(100),
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
                       nom VARCHAR(50) NOT NULL UNIQUE,
                       date_creation TIMESTAMP DEFAULT now(),
                       date_mis_a_jour TIMESTAMP,
                       date_suppression TIMESTAMP
);

CREATE TABLE statuts (
                         id SERIAL PRIMARY KEY,
                         description VARCHAR(50) NOT NULL UNIQUE,
                         date_creation TIMESTAMP DEFAULT now(),
                         date_mis_a_jour TIMESTAMP,
                         date_suppression TIMESTAMP
);

CREATE TABLE fournisseurs_auth (
                                   id SERIAL PRIMARY KEY,
                                   nom VARCHAR(50) NOT NULL UNIQUE,
                                   date_creation TIMESTAMP DEFAULT now(),
                                   date_mis_a_jour TIMESTAMP,
                                   date_suppression TIMESTAMP
);

CREATE TABLE entreprises (
                             id SERIAL PRIMARY KEY,
                             nom VARCHAR(100) NOT NULL,
                             date_creation TIMESTAMP DEFAULT now(),
                             date_mis_a_jour TIMESTAMP,
                             date_suppression TIMESTAMP
);

CREATE TABLE statuts_signalement (
                                     id SERIAL PRIMARY KEY,
                                     nom VARCHAR(50) NOT NULL UNIQUE,
                                     date_creation TIMESTAMP DEFAULT now(),
                                     date_mis_a_jour TIMESTAMP,
                                     date_suppression TIMESTAMP
);

CREATE TABLE parametres (
                            id SERIAL PRIMARY KEY,
                            nom VARCHAR(50) NOT NULL UNIQUE,
                            valeur VARCHAR(255) NOT NULL,
                            type VARCHAR(50),
                            date_creation TIMESTAMP DEFAULT now(),
                            date_mis_a_jour TIMESTAMP,
                            date_suppression TIMESTAMP
);

CREATE TABLE signalements (
                              id SERIAL PRIMARY KEY,
                              description TEXT,
                              surface_m2 NUMERIC(15,2),
                              budget NUMERIC(15,2),
                              points GEOMETRY,
                              id_entreprise INT NOT NULL,
                              version INT DEFAULT 1,
                              date_creation TIMESTAMP DEFAULT now(),
                              date_mis_a_jour TIMESTAMP,
                              date_suppression TIMESTAMP,
                              CONSTRAINT fk_signalement_entreprise
                                  FOREIGN KEY (id_entreprise)
                                      REFERENCES entreprises(id)
);

CREATE TABLE roles_utilisateur (
                                   id_utilisateur INT NOT NULL,
                                   id_role INT NOT NULL,
                                   date_creation TIMESTAMP DEFAULT now(),
                                   date_mis_a_jour TIMESTAMP,
                                   date_suppression TIMESTAMP,
                                   PRIMARY KEY (id_utilisateur, id_role),
                                   FOREIGN KEY (id_utilisateur) REFERENCES utilisateurs(id),
                                   FOREIGN KEY (id_role) REFERENCES roles(id)
);

CREATE TABLE statuts_utilisateur (
                                     id_utilisateur INT NOT NULL,
                                     id_statut INT NOT NULL,
                                     date_creation TIMESTAMP DEFAULT now(),
                                     date_mis_a_jour TIMESTAMP,
                                     date_suppression TIMESTAMP,
                                     PRIMARY KEY (id_utilisateur, id_statut),
                                     FOREIGN KEY (id_utilisateur) REFERENCES utilisateurs(id),
                                     FOREIGN KEY (id_statut) REFERENCES statuts(id)
);


CREATE TABLE fournisseurs_auth_utilisateur (
                                               id_utilisateur INT NOT NULL,
                                               id_fournisseur_auth INT NOT NULL,
                                               date_creation TIMESTAMP DEFAULT now(),
                                               date_mis_a_jour TIMESTAMP,
                                               date_suppression TIMESTAMP,
                                               PRIMARY KEY (id_utilisateur, id_fournisseur_auth),
                                               FOREIGN KEY (id_utilisateur) REFERENCES utilisateurs(id),
                                               FOREIGN KEY (id_fournisseur_auth) REFERENCES fournisseurs_auth(id)
);


CREATE TABLE historique_statut_signalement (
                                               id_signalement SERIAL NOT NULL,
                                               id_statut_signalement INT NOT NULL,
                                               date_creation TIMESTAMP DEFAULT now(),
                                               date_mis_a_jour TIMESTAMP,
                                               date_suppression TIMESTAMP,
                                               PRIMARY KEY (id_signalement, id_statut_signalement),
                                               FOREIGN KEY (id_signalement) REFERENCES signalements(id),
                                               FOREIGN KEY (id_statut_signalement) REFERENCES statuts_signalement(id)
);

CREATE TABLE journal (
                             id SERIAL PRIMARY KEY,
                             id_entite INT,
                             type_entite TEXT,
                             operation TEXT CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
                             donnees JSONB,
                             version INT,
                             date_creation TIMESTAMP DEFAULT now(),
                             synchronise BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_roles_utilisateur_user ON roles_utilisateur(id_utilisateur);
CREATE INDEX idx_roles_utilisateur_role ON roles_utilisateur(id_role);

CREATE INDEX idx_statuts_utilisateur_user ON statuts_utilisateur(id_utilisateur);
CREATE INDEX idx_statuts_utilisateur_statut ON statuts_utilisateur(id_statut);

CREATE INDEX idx_fournisseurs_auth_user ON fournisseurs_auth_utilisateur(id_utilisateur);
CREATE INDEX idx_fournisseurs_auth_auth ON fournisseurs_auth_utilisateur(id_fournisseur_auth);

CREATE INDEX idx_signalement_entreprise ON signalements(id_entreprise);
CREATE INDEX idx_journal_sync ON journal(synchronise);
