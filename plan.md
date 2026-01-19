# Plan d’implémentation – Authentification locale (sans synchro pour l’instant)

## Contexte

Ce document décrit le **plan d’implémentation du module d’authentification en mode local uniquement**, basé sur la base de données PostgreSQL fournie.

La **synchronisation online (Firebase Auth)** n’est **PAS implémentée dans cette version**, mais le design **prévoit explicitement des points d’extension (placeholders)** pour l’ajouter plus tard **sans refonte majeure**.

Le module est conçu comme une **API REST** consommée par un frontend (web ou mobile).

---

## Objectifs de cette version (V1 – Locale)

* Authentification par **email + mot de passe**
* Stockage sécurisé du mot de passe (hash)
* Gestion des tentatives échouées et du blocage
* Gestion des rôles et statuts utilisateur
* Architecture compatible avec une future auth online (Firebase)

---

## Hypothèses techniques

* PostgreSQL est toujours accessible (pas de vrai offline client)
* Firebase **n’est pas appelé** dans cette version
* `firebase_uid` est **NULL** pour tous les utilisateurs
* Le champ `version` est utilisé pour le versioning métier

---

## Modèle de données concerné

### Table `utilisateurs`

Champs utilisés dans V1 :

* `email`
* `nom`
* `mot_de_passe_hash`
* `tentatives_echouees`
* `bloque_jusqua`
* `actif`
* `version`

Champs réservés (placeholder V2) :

* `firebase_uid`

---

## Flux fonctionnels

### 1. Inscription utilisateur (locale)

#### Entrée API

```
POST /auth/register
```

Payload :

* email
* mot_de_passe
* nom

#### Logique backend

1. Vérifier unicité de l’email
2. Hasher le mot de passe (bcrypt / argon2)
3. Créer l’utilisateur dans `utilisateurs`
4. Initialiser :

    * tentatives_echouees = 0
    * actif = true
    * version = 1
5. Associer :

    * rôle par défaut (table `roles_utilisateur`)
    * statut initial (table `statuts_utilisateur`)

#### Placeholder V2 (non implémenté)

```text
// TODO: Créer le compte Firebase et renseigner firebase_uid
```

---

### 2. Connexion utilisateur (locale)

#### Entrée API

```
POST /auth/login
```

Payload :

* email
* mot_de_passe

#### Logique backend

1. Charger l’utilisateur par email
2. Vérifier :

    * actif = true
    * bloque_jusqua est NULL ou expiré
3. Comparer le hash du mot de passe
4. Si succès :

    * reset tentatives_echouees
    * retourner un token de session (JWT local)
5. Si échec :

    * incrémenter tentatives_echouees
    * bloquer si seuil atteint

#### Placeholder V2

```text
// TODO: Si online, déléguer l’authentification à Firebase Auth
```

---

### 3. Gestion du blocage

Règles :

* X tentatives échouées → blocage temporaire
* Blocage stocké dans `bloque_jusqua`

API admin possible :

```
POST /auth/debloquer-utilisateur
```

---

## Sécurité

* Aucun mot de passe stocké en clair
* Hash recommandé : argon2id
* Comparaison sécurisée des hash
* Messages d’erreur neutres ("Email ou mot de passe incorrect")

---

## Rôles et statuts

* Rôles : `roles`, `roles_utilisateur`
* Statuts : `statuts`, `statuts_utilisateur`
* Utilisés pour :

    * autorisations
    * activation / suspension

---

## Journalisation (préparation sync)

### Table `journal`

Utilisation en V1 :

* Journaliser la création utilisateur
* Journaliser les changements de statut

Utilisation future (V2) :

```text
// TODO: synchroniser journal avec backend distant
```

---

## Placeholders explicites pour la V2 (Online)

À laisser dans le code :

* Service `AuthProvider`

    * `LocalAuthProvider` (implémenté)
    * `FirebaseAuthProvider` (TODO)

* Champs ignorés pour l’instant :

    * utilisateurs.firebase_uid
    * fournisseurs_auth
    * fournisseurs_auth_utilisateur

* Feature flag :

```text
auth.mode = LOCAL | HYBRID
```

---

## UX attendue (V1)

* Un seul formulaire email + mot de passe
* Aucune mention de Firebase
* Messages simples :

    * "Connexion réussie"
    * "Email ou mot de passe incorrect"
    * "Compte bloqué temporairement"

---

## Phrase clé pour documentation / soutenance

> "La première version du module d’authentification repose sur une
> authentification locale sécurisée, conçue dès l’origine pour être
> étendue vers un modèle hybride avec fournisseur externe."

---

## Résultat attendu V1

* Authentification locale fonctionnelle
* Code prêt pour extension online
* Base de données déjà compatible
* Aucune dette technique bloquante

# Plan d'implementation - Architecture Hybride Online/Offline

## Contexte

Ce document decrit le **plan d'implementation du module d'authentification en mode hybride**, 
permettant de fonctionner avec **Firebase (mode Online)** ou **PostgreSQL local (mode Offline)**.

---

## Architecture Implementee (V2)

### Modes de fonctionnement

| Mode | Description | Configuration |
|------|-------------|---------------|
| **ONLINE** | Firebase Auth + Firestore par defaut | `app.mode=ONLINE` |
| **OFFLINE** | PostgreSQL local par defaut | `app.mode=OFFLINE` |
| **AUTO** | Detection automatique de la connectivite | `app.mode=AUTO` (defaut) |

### Comportement par mode

#### Mode ONLINE (Firebase par defaut)
- **Inscription** : Cree le compte dans Firebase Auth + Firestore
- **Connexion** : Authentification via Firebase Auth
- **Journalisation** : Toujours en local pour backup/audit

#### Mode OFFLINE (Local par defaut)
- **Inscription** : Cree le compte dans PostgreSQL (`firebase_uid = NULL`)
- **Connexion** : Authentification locale (hash bcrypt)
- **Journalisation** : En local, en attente de synchronisation

#### Mode AUTO
- Detecte automatiquement si Firebase est accessible
- Bascule vers OFFLINE si Firebase indisponible

---

## Synchronisation

### Push (Local -> Firebase)
- Envoie les donnees locales vers Firestore
- **Cree les comptes Firebase Auth** pour les utilisateurs sans `firebase_uid`
- Marque les entrees du journal comme synchronisees
- Endpoint: `POST /sync/push`

### Pull (Firebase -> Local)
- Importe les donnees depuis Firestore vers PostgreSQL
- Gestion des conflits par version
- Endpoint: `POST /sync/pull`

### Sync All (Bidirectionnel)
- Combine Push + Pull
- Endpoint: `POST /sync/all`

---

## Endpoints API

### Authentification

| Methode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/auth/health` | Health check + mode actuel |
| `GET` | `/auth/mode` | Mode configure et effectif |
| `POST` | `/auth/register` | Inscription utilisateur |
| `POST` | `/auth/login` | Connexion utilisateur |
| `POST` | `/auth/debloquer-utilisateur` | Deblocage compte |

### Synchronisation

| Methode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/sync/status` | Statut de synchronisation |
| `POST` | `/sync/push` | Local -> Firebase |
| `POST` | `/sync/pull` | Firebase -> Local |
| `POST` | `/sync/all` | Bidirectionnel |
| `POST` | `/sync/utilisateurs` | Sync utilisateurs uniquement |

---

## Services implementes

| Service | Role |
|---------|------|
| `ConnectivityService` | Detection du mode Online/Offline |
| `HybridAuthService` | Orchestration auth selon le mode |
| `LocalAuthService` | Auth locale PostgreSQL |
| `FirebaseAuthService` | Auth Firebase (REST API) |
| `FirestoreService` | CRUD Firestore |
| `SyncService` | Synchronisation bidirectionnelle |
| `JournalService` | Journalisation des operations |
| `ParametreService` | Parametres depuis la BDD |

---

## Journalisation

Toutes les operations sont journalisees dans la table `journal`:

```sql
CREATE TABLE journal (
    id SERIAL PRIMARY KEY,
    id_entite INT,
    type_entite TEXT,           -- nom de la table (utilisateurs, roles, etc.)
    operation TEXT,             -- INSERT, UPDATE, DELETE
    donnees JSONB,              -- donnees de l'operation
    version INT,
    date_creation TIMESTAMP,
    synchronise BOOLEAN DEFAULT FALSE
);
```

Les entrees non synchronisees (`synchronise = FALSE`) sont envoyees vers Firestore lors du Push.

---

## Configuration

### application.properties

```properties
# Mode de fonctionnement: ONLINE | OFFLINE | AUTO
app.mode=AUTO

# Firebase
firebase.apiKey=<votre-api-key>
```

### Parametres en BDD (table `parametres`)

| Nom | Description | Defaut |
|-----|-------------|--------|
| `MAX_FAILED_ATTEMPTS` | Tentatives avant blocage | 5 |
| `BLOCK_DURATION_MINUTES` | Duree du blocage | 30 |
| `DEFAULT_ROLE` | Role par defaut | USER |
| `DEFAULT_STATUS` | Statut par defaut | ACTIF |

---

## Schema de donnees

### Utilisateurs synchronises

Le champ `firebase_uid` est renseigne pour les utilisateurs synchronises avec Firebase Auth.

| Champ | Local Only | Synced |
|-------|------------|--------|
| `firebase_uid` | NULL | uid Firebase |
| `version` | 1+ | incremente a chaque sync |

---

## Gestion des conflits

Lors de la synchronisation, les conflits sont resolus par **version**:
- La version la plus elevee gagne
- En cas d'egalite, la donnee locale est mise a jour depuis Firebase

