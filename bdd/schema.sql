CREATE TABLE user (
                       id COUNTER,
                       email VARCHAR(50) NOT NULL,
                       name VARCHAR(50),
                       password_hash VARCHAR(50) NOT NULL,
                       firebase_uuid VARCHAR(128),
                       enabled LOGICAL,
                       created_at DATETIME,
                       PRIMARY KEY (id),
                       UNIQUE (email)
);

CREATE TABLE role (
                      id COUNTER,
                      name VARCHAR(50),
                      PRIMARY KEY (id)
);

CREATE TABLE status (
                        id COUNTER,
                        description VARCHAR(50) NOT NULL,
                        PRIMARY KEY (id),
                        UNIQUE (description)
);

CREATE TABLE auth_provider (
                               id COUNTER,
                               name VARCHAR(50),
                               PRIMARY KEY (id)
);

CREATE TABLE sync_status (
                             id COUNTER,
                             name VARCHAR(50),
                             PRIMARY KEY (id)
);

CREATE TABLE sync_status_history (
                                     id COUNTER,
                                     table_name VARCHAR(50),
                                     object_id VARCHAR(50),
                                     synced_at DATETIME,
                                     id_sync_status INT NOT NULL,
                                     PRIMARY KEY (id),
                                     FOREIGN KEY (id_sync_status) REFERENCES sync_status(id)
);

CREATE TABLE company (
                         id COUNTER,
                         name VARCHAR(50),
                         created_at DATETIME,
                         PRIMARY KEY (id)
);

CREATE TABLE report_status (
                               id COUNTER,
                               name VARCHAR(50),
                               PRIMARY KEY (id)
);

CREATE TABLE setting (
                         id COUNTER,
                         name VARCHAR(50) NOT NULL,
                         value_ VARCHAR(50) NOT NULL,
                         created_at DATETIME,
                         type VARCHAR(50),
                         PRIMARY KEY (id),
                         UNIQUE (name)
);

CREATE TABLE report (
                        id COUNTER,
                        created_at DATETIME,
                        budget DECIMAL(15,2),
                        description TEXT,
                        points GEOMETRY,
                        id_company INT NOT NULL,
                        PRIMARY KEY (id),
                        FOREIGN KEY (id_company) REFERENCES company(id)
);

CREATE TABLE role_user (
                           id_user INT,
                           id_role INT,
                           PRIMARY KEY (id_user, id_role),
                           FOREIGN KEY (id_user) REFERENCES user(id),
                           FOREIGN KEY (id_role) REFERENCES role(id)
);

CREATE TABLE user_status (
                             id_user INT,
                             id_status INT,
                             updated_at DATETIME,
                             PRIMARY KEY (id_user, id_status),
                             FOREIGN KEY (id_user) REFERENCES user(id),
                             FOREIGN KEY (id_status) REFERENCES status(id)
);

CREATE TABLE user_auth_provider (
                                    id_user INT,
                                    id_auth_provider INT,
                                    PRIMARY KEY (id_user, id_auth_provider),
                                    FOREIGN KEY (id_user) REFERENCES user(id),
                                    FOREIGN KEY (id_auth_provider) REFERENCES auth_provider(id)
);

CREATE TABLE report_status_history (
                                       id_report INT,
                                       id_report_status INT,
                                       updated_at DATETIME,
                                       PRIMARY KEY (id_report, id_report_status),
                                       FOREIGN KEY (id_report) REFERENCES report(id),
                                       FOREIGN KEY (id_report_status) REFERENCES report_status(id)
);
