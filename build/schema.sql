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

-- ============================================
-- DONNEES DE TEST
-- ============================================

-- Roles (USER et MANAGER)
INSERT INTO roles (nom, date_creation) VALUES
('USER', now()),
('MANAGER', now());

-- Statuts utilisateur (bloque et debloque)
INSERT INTO statuts (description, date_creation) VALUES
('BLOQUE', now()),
('DEBLOQUE', now()),
('ACTIF', now()),
('INACTIF', now());

-- Statuts signalement
INSERT INTO statuts_signalement (nom, date_creation) VALUES
('EN_ATTENTE', now()),
('EN_COURS', now()),
('VALIDE', now()),
('REJETE', now()),
('CLOTURE', now());

-- Parametres (valeurs par defaut du ParametreService)
INSERT INTO parametres (nom, valeur, type, date_creation) VALUES
('MAX_FAILED_ATTEMPTS', '5', 'INTEGER', now()),
('BLOCK_DURATION_MINUTES', '30', 'INTEGER', now()),
('DEFAULT_ROLE', 'USER', 'STRING', now()),
('DEFAULT_STATUS', 'ACTIF', 'STRING', now());

-- Entreprises de test
INSERT INTO entreprises (nom, date_creation) VALUES
('Entreprise Alpha', now()),
('Entreprise Beta', now()),
('Societe Gamma', now());

-- Utilisateurs de test (firebase_uid = NULL, mot de passe = "password123" hashé en bcrypt)
-- Hash bcrypt pour "password123" : $2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqzGVzALy6v6T8Kj7YKj8KkE.g6Iu
INSERT INTO utilisateurs (email, nom, mot_de_passe_hash, firebase_uid, tentatives_echouees, bloque_jusqua, actif, version, date_creation) VALUES
('admin@test.com', 'Admin Test', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqzGVzALy6v6T8Kj7YKj8KkE.g6Iu', NULL, 0, NULL, true, 1, now()),
('manager@test.com', 'Manager Test', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqzGVzALy6v6T8Kj7YKj8KkE.g6Iu', NULL, 0, NULL, true, 1, now()),
('user@test.com', 'User Test', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqzGVzALy6v6T8Kj7YKj8KkE.g6Iu', NULL, 0, NULL, true, 1, now()),
('bloque@test.com', 'Utilisateur Bloque', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqzGVzALy6v6T8Kj7YKj8KkE.g6Iu', NULL, 5, now() + interval '30 minutes', true, 1, now()),
('inactif@test.com', 'Utilisateur Inactif', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqzGVzALy6v6T8Kj7YKj8KkE.g6Iu', NULL, 0, NULL, false, 1, now());

-- Attribution des roles aux utilisateurs
-- admin@test.com (id=1) -> MANAGER (id=2)
-- manager@test.com (id=2) -> MANAGER (id=2)
-- user@test.com (id=3) -> USER (id=1)
-- bloque@test.com (id=4) -> USER (id=1)
-- inactif@test.com (id=5) -> USER (id=1)
INSERT INTO roles_utilisateur (id_utilisateur, id_role, date_creation) VALUES
(1, 2, now()),
(2, 2, now()),
(3, 1, now()),
(4, 1, now()),
(5, 1, now());

-- Attribution des statuts aux utilisateurs
-- admin@test.com (id=1) -> ACTIF (id=3)
-- manager@test.com (id=2) -> ACTIF (id=3)
-- user@test.com (id=3) -> ACTIF (id=3)
-- bloque@test.com (id=4) -> BLOQUE (id=1)
-- inactif@test.com (id=5) -> INACTIF (id=4)
INSERT INTO statuts_utilisateur (id_utilisateur, id_statut, date_creation) VALUES
(1, 3, now()),
(2, 3, now()),
(3, 3, now()),
(4, 1, now()),
(5, 4, now());

-- Signalements de test (avec coordonnées GPS - points en WGS84/SRID 4326)
INSERT INTO signalements (description, surface_m2, budget, points, id_entreprise, version, date_creation) VALUES
('Signalement zone industrielle nord', 1500.50, 250000.00, ST_SetSRID(ST_MakePoint(47.5162, -18.9137), 4326), 1, 1, now()),
('Signalement terrain agricole', 5000.00, 150000.00, ST_SetSRID(ST_MakePoint(47.5234, -18.9256), 4326), 1, 1, now()),
('Signalement batiment commercial', 800.75, 500000.00, ST_SetSRID(ST_MakePoint(47.5089, -18.9078), 4326), 2, 1, now()),
('Signalement parcelle residentielle', 350.00, 120000.00, ST_SetSRID(ST_MakePoint(47.5301, -18.9189), 4326), 2, 1, now()),
('Signalement entrepot logistique', 2500.00, 750000.00, ST_SetSRID(ST_MakePoint(47.4978, -18.9345), 4326), 3, 1, now());

-- Historique des statuts des signalements
-- Signalement 1 : EN_ATTENTE -> EN_COURS -> VALIDE
-- Signalement 2 : EN_ATTENTE
-- Signalement 3 : EN_ATTENTE -> REJETE
-- Signalement 4 : EN_ATTENTE -> EN_COURS
-- Signalement 5 : EN_ATTENTE -> EN_COURS -> VALIDE -> CLOTURE
INSERT INTO historique_statut_signalement (id_signalement, id_statut_signalement, date_creation) VALUES
(1, 1, now() - interval '10 days'),
(1, 2, now() - interval '5 days'),
(1, 3, now() - interval '1 day'),
(2, 1, now() - interval '2 days'),
(3, 1, now() - interval '7 days'),
(3, 4, now() - interval '3 days'),
(4, 1, now() - interval '4 days'),
(4, 2, now() - interval '1 day'),
(5, 1, now() - interval '15 days'),
(5, 2, now() - interval '10 days'),
(5, 3, now() - interval '5 days'),
(5, 5, now());

-- Quelques entrees dans le journal pour tracer les actions
INSERT INTO journal (id_entite, type_entite, operation, donnees, version, date_creation, synchronise) VALUES
(1, 'utilisateurs', 'INSERT', '{"action": "REGISTER", "email": "admin@test.com", "nom": "Admin Test"}', 1, now() - interval '30 days', false),
(2, 'utilisateurs', 'INSERT', '{"action": "REGISTER", "email": "manager@test.com", "nom": "Manager Test"}', 1, now() - interval '25 days', false),
(3, 'utilisateurs', 'INSERT', '{"action": "REGISTER", "email": "user@test.com", "nom": "User Test"}', 1, now() - interval '20 days', false),
(4, 'utilisateurs', 'INSERT', '{"action": "REGISTER", "email": "bloque@test.com", "nom": "Utilisateur Bloque"}', 1, now() - interval '15 days', false),
(4, 'utilisateurs', 'UPDATE', '{"action": "ACCOUNT_BLOCKED", "email": "bloque@test.com", "raison": "Trop de tentatives echouees"}', 1, now(), false),
(1, 'signalements', 'INSERT', '{"description": "Signalement zone industrielle nord", "id_entreprise": 1}', 1, now() - interval '10 days', false),
(1, 'entreprises', 'INSERT', '{"nom": "Entreprise Alpha"}', 1, now() - interval '35 days', false);

