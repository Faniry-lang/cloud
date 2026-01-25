# Cloud Project - API Backend Hybride

Une API REST Spring Boot avec authentification hybride Firebase/PostgreSQL et synchronisation bidirectionnelle.

## 📋 Description

Ce projet est un backend Java/Spring Boot qui permet de gérer des utilisateurs, entreprises, signalements, rôles et statuts. L'architecture est conçue pour fonctionner en mode **hybride** : 
- **En ligne (ONLINE)** : Les données sont stockées dans Firebase (Firestore) et l'authentification passe par Firebase Auth
- **Hors ligne (OFFLINE)** : Les données sont stockées localement dans PostgreSQL

Le système bascule automatiquement entre les deux modes selon la connectivité, ce qui est pratique si vous avez des utilisateurs dans des zones avec une connection internet instable.

## 🏗️ Architecture

```
src/main/java/itu/cloud/
├── config/           # Configuration (Security, Firebase, etc.)
├── controller/       # Contrôleurs REST
├── dto/              # Objets de transfert de données
├── entities/         # Entités JPA
├── repositories/     # Repositories Spring Data
└── service/          # Services métier
```

### Technologies utilisées

- **Java 17** 
- **Spring Boot 4.0.1**
- **PostgreSQL** (base de données locale)
- **Firebase Admin SDK** (authentification et Firestore)
- **Docker Compose** (pour le déploiement)
- **Lombok** (réduction du boilerplate)

## ⚙️ Configuration

### Fichier `application.properties`

```properties
spring.application.name=cloud
spring.datasource.url=jdbc:postgresql://localhost:5433/projet_cloud_s5
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update

firebase.apiKey=VOTRE_CLE_API_FIREBASE

# Mode d'authentification : AUTO | ONLINE | OFFLINE
app.mode=AUTO
```

**Modes disponibles :**
- `AUTO` : Détection automatique de la connectivité (recommandé)
- `ONLINE` : Force l'utilisation de Firebase
- `OFFLINE` : Force l'utilisation de PostgreSQL local

### Fichier Firebase

Placez votre fichier de credentials Firebase (`fir-getting-started-xxxx-firebase-adminsdk-xxxx.json`) dans le dossier `src/main/resources/`.

## 🚀 Lancement

### Prérequis

- Java 17+
- Maven
- Docker et Docker Compose (pour PostgreSQL)
- Un projet Firebase configuré

### Commandes

```bash
# Démarrer la base de données PostgreSQL
docker-compose up -d

# Compiler le projet
mvn clean compile

# Lancer l'application
mvn spring-boot:run
```

L'API sera disponible sur `http://localhost:8080`

## 📊 Base de données

### Schéma principal

Le projet utilise PostgreSQL avec les tables suivantes :

| Table | Description |
|-------|-------------|
| `utilisateurs` | Utilisateurs de l'application |
| `roles` | Rôles disponibles (admin, user, etc.) |
| `statuts` | Statuts utilisateur |
| `entreprises` | Entreprises enregistrées |
| `signalements` | Signalements liés aux entreprises |
| `journal` | Journal des modifications (pour la synchro) |
| `parametres` | Paramètres de configuration |

### Gestion du blocage utilisateur

Les utilisateurs peuvent être bloqués après trop de tentatives de connexion échouées. Les paramètres sont configurables via la table `parametres` :
- `MAX_FAILED_ATTEMPTS` : Nombre max de tentatives (défaut: 5)
- `LOCK_DURATION_MINUTES` : Durée du blocage en minutes (défaut: 15)

---

# 📚 Documentation API

Toutes les réponses de l'API suivent ce format standardisé :

```json
{
  "success": true,
  "data": { ... },
  "message": "Message optionnel",
  "mode": "ONLINE | OFFLINE"
}
```

## 🔐 Authentification (`/auth`)

### Health Check

Vérifie que l'API d'authentification fonctionne.

```
GET /auth/health
```

**Réponse :**
```json
{
  "status": "UP",
  "timestamp": "2026-01-20T10:30:00Z"
}
```

---

### Mode d'authentification

Retourne le mode d'authentification configuré et effectif.

```
GET /auth/mode
```

**Réponse :**
```json
{
  "configuredMode": "AUTO",
  "effectiveMode": "ONLINE"
}
```

---

### Inscription

Créé un nouveau compte utilisateur.

```
POST /auth/register
Content-Type: application/json
```

**Body :**
```json
{
  "email": "utilisateur@example.com",
  "password": "motdepasse123",
  "nom": "Jean Dupont"
}
```

**Réponse (succès) :**
```json
{
  "success": true,
  "message": "Inscription reussie",
  "user": {
    "id": 1,
    "email": "utilisateur@example.com",
    "nom": "Jean Dupont"
  },
  "mode": "ONLINE"
}
```

**Réponse (erreur) :**
```json
{
  "success": false,
  "error": "Un compte existe deja avec cet email"
}
```

---

### Connexion

Authentifie un utilisateur existant.

```
POST /auth/login
Content-Type: application/json
```

**Body :**
```json
{
  "email": "utilisateur@example.com",
  "password": "motdepasse123"
}
```

**Réponse (succès) :**
```json
{
  "success": true,
  "data": {
    "idToken": "eyJhbGciOiJSUzI1NiIs...",
    "refreshToken": "AMf-vBwU3...",
    "expiresIn": "3600",
    "localId": "abc123",
    "email": "utilisateur@example.com"
  },
  "mode": "ONLINE"
}
```

**Réponse (compte bloqué) :**
```json
{
  "success": false,
  "error": "Compte bloque. Reessayez dans 15 minutes",
  "bloqueJusqua": "2026-01-20T11:00:00Z"
}
```

---

### Débloquer un utilisateur

Débloque manuellement un utilisateur (fonctionnalité admin).

```
POST /auth/debloquer-utilisateur
Content-Type: application/json
```

**Body :**
```json
{
  "email": "utilisateur@example.com"
}
```

**Réponse :**
```json
{
  "success": true,
  "message": "Utilisateur debloque avec succes"
}
```

---

## 👥 Utilisateurs (`/api/utilisateurs`)

### Liste des utilisateurs

```
GET /api/utilisateurs
```

**Réponse :**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "email": "user1@example.com",
      "nom": "Jean Dupont",
      "actif": true,
      "version": 1,
      "dateCreation": "2026-01-15T08:00:00Z"
    }
  ],
  "mode": "ONLINE"
}
```

---

### Récupérer un utilisateur par ID

```
GET /api/utilisateurs/{id}
```

---

### Récupérer un utilisateur par email

```
GET /api/utilisateurs/email/{email}
```

---

### Modifier un utilisateur

```
PUT /api/utilisateurs/{id}
Content-Type: application/json
```

**Body :**
```json
{
  "nom": "Nouveau Nom",
  "actif": true
}
```

---

### Désactiver un utilisateur (soft delete)

```
DELETE /api/utilisateurs/{id}
```

---

## 🏢 Entreprises (`/api/entreprises`)

### Liste des entreprises

```
GET /api/entreprises
```

---

### Récupérer une entreprise

```
GET /api/entreprises/{id}
```

---

### Créer une entreprise

```
POST /api/entreprises
Content-Type: application/json
```

**Body :**
```json
{
  "nom": "Ma Nouvelle Entreprise"
}
```

---

### Modifier une entreprise

```
PUT /api/entreprises/{id}
Content-Type: application/json
```

**Body :**
```json
{
  "nom": "Nom Modifié"
}
```

---

### Supprimer une entreprise

```
DELETE /api/entreprises/{id}
```

---

## 📍 Signalements (`/api/signalements`)

### Liste des signalements

```
GET /api/signalements
```

---

### Récupérer un signalement

```
GET /api/signalements/{id}
```

---

### Signalements par entreprise

```
GET /api/signalements/entreprise/{idEntreprise}
```

---

### Créer un signalement

```
POST /api/signalements
Content-Type: application/json
```

**Body :**
```json
{
  "description": "Description du signalement",
  "surfaceM2": 150.50,
  "budget": 25000.00,
  "idEntreprise": 1
}
```

---

### Modifier un signalement

```
PUT /api/signalements/{id}
Content-Type: application/json
```

---

### Supprimer un signalement

```
DELETE /api/signalements/{id}
```

---

## 🎭 Rôles (`/api/roles`)

### Liste des rôles

```
GET /api/roles
```

---

### Créer un rôle

```
POST /api/roles
Content-Type: application/json
```

**Body :**
```json
{
  "nom": "ADMIN"
}
```

---

### Modifier un rôle

```
PUT /api/roles/{id}
```

---

### Supprimer un rôle

```
DELETE /api/roles/{id}
```

---

## 📊 Statuts (`/api/statuts`)

### Liste des statuts

```
GET /api/statuts
```

---

### Créer un statut

```
POST /api/statuts
Content-Type: application/json
```

**Body :**
```json
{
  "description": "Actif"
}
```

---

### Modifier un statut

```
PUT /api/statuts/{id}
```

---

### Supprimer un statut

```
DELETE /api/statuts/{id}
```

---

## 🔄 Synchronisation (`/sync`)

La synchronisation permet de maintenir les données cohérentes entre PostgreSQL local et Firebase Firestore.

### Statut de synchronisation

Affiche le nombre d'entrées en attente de synchronisation et la disponibilité de Firestore.

```
GET /sync/status
```

**Réponse :**
```json
{
  "success": true,
  "pendingEntries": 5,
  "firestoreAvailable": true,
  "lastSyncDate": "2026-01-20T09:00:00Z"
}
```

---

### Push (Local → Firebase)

Envoie les données locales vers Firestore. Cette opération :
- Synchronise les utilisateurs, entreprises, signalements, etc.
- Créé les comptes Firebase Auth pour les utilisateurs qui n'en ont pas encore
- Marque les entrées du journal comme synchronisées

```
POST /sync/push
```

**Réponse :**
```json
{
  "success": true,
  "message": "Synchronisation terminee",
  "utilisateursSynchonises": 10,
  "journalEntriesSynced": 25
}
```

---

### Pull (Firebase → Local)

Importe les données depuis Firestore vers PostgreSQL local.

```
POST /sync/pull
```

---

### Synchronisation complète

Effectue un push puis un pull (synchronisation bidirectionnelle).

```
POST /sync/all
```

---

### Synchroniser uniquement les utilisateurs

```
POST /sync/utilisateurs
```

---

## 🔒 Sécurité

### CORS

L'API accepte les requêtes depuis n'importe quelle origine en mode développement. Pour la production, configurez les origines autorisées dans `SecurityConfig.java`.

### Endpoints publics

Les endpoints suivants sont accessibles sans authentification :
- `/auth/**` - Authentification
- `/sync/**` - Synchronisation
- `/api/**` - API REST (à sécuriser en production)

---

## 📝 Journal des modifications

Toutes les opérations CRUD sont automatiquement enregistrées dans la table `journal`. Ce mécanisme permet :
- La traçabilité des actions
- La synchronisation différée vers Firebase
- La résolution de conflits en cas de modifications concurrentes

Structure d'une entrée de journal :
```json
{
  "id": 1,
  "idEntite": 5,
  "typeEntite": "utilisateurs",
  "operation": "INSERT",
  "donnees": {"email": "test@example.com", "nom": "Test"},
  "version": 1,
  "dateCreation": "2026-01-20T10:00:00Z",
  "synchronise": false
}
```

---

## 🐛 Dépannage

### L'application ne démarre pas

1. Vérifiez que Docker est lancé (pour PostgreSQL)
2. Vérifiez que le port 8080 n'est pas déja utilisé
3. Vérifiez que le fichier Firebase credentials est présent

### Erreur "Connection refused" lors des requêtes

Assurez-vous que l'application est bien démarrée et écoute sur le bon port.

### Les données ne se synchronisent pas

1. Vérifiez la connectivité internet
2. Vérifiez les credentials Firebase
3. Consultez `/sync/status` pour voir l'état de la synchronisation

---

## 📄 License

Ce projet est développé dans le cadre du cours Cloud S5 - ITU.

---

*Documentation générée le 20 janvier 2026*

