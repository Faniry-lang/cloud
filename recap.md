# Récapitulatif du Projet - TanaTravaux

## 1. Présentation du Projet
**TanaTravaux** est une application web destinée à la gestion et au suivi des travaux routiers dans la ville d'Antananarivo. Elle permet une visualisation cartographique interactive pour le grand public et offre des outils d'administration avancés pour les gestionnaires.

**Objectifs :**
*   Transparence sur l'avancement des travaux.
*   Centralisation des données (Budget, Surface, Entreprises).
*   Gestion des accès et synchronisation des données (Mode Connecté/Déconnecté).

---

## 2. Modèle Conceptuel de Données (MCD)

### A. Niveau Affichage (Frontend State)
Structures de données utilisées dans l'interface React :

*   **State Signalement** (Visualisation Carte & Table) :
    *   `id`: Identifiant unique.
    *   `lat`, `lng`: Coordonnées géographiques (%).
    *   `type`: Type de travaux (Nid de poule, Drainage, etc.).
    *   `iconType`: Icône visuelle (Marteau, Eau, Pont, Alerte).
    *   `status`: État actuel (NOUVEAU, EN_COURS, TERMINE).
    *   `budget`: Coût estimé (Ar).
    *   `surface`: Surface affectée (m²).
    *   `company`: Entreprise prestataire.

*   **State Utilisateur** (Contexte Auth) :
    *   `email`: Identifiant de connexion.
    *   `role`: Niveau d'accès (MANAGER, USER, VISITOR).
    *   `status`: État du compte (ACTIVE, BLOCKED).

### B. Niveau Métier (Services Frontend & API)
Logique de gestion des interactions :

*   **Service Authentification (`auth.service.js`)** :
    *   `login(email, password)` : Vérification des identifiants via API.
    *   `unlockUser(email)`, `blockUser(email)` : Gestion des interdictions.
    *   `changeRole(email, role)` : Promotion/Rétrogradation des utilisateurs.
*   **Service Synchronisation (API Java)** :
    *   `pushSync()` : Envoi des données locales vers Firebase.
    *   `pullSync()` : Récupération des données depuis Firebase.
    *   `getSyncStatus()` : Monitoring des tâches en attente.

### C. Niveau Base de Données (Backend Java)
Schéma relationnel (PostgreSQL) et Documentaire (Firestore) :

*   **Table `utilisateurs`** :
    *   `id` (PK), `email` (Unique), `password_hash`, `role`, `status`, `firebase_uid`.
*   **Table `signalements`** :
    *   `id` (PK), `type`, `date_creation`, `statut`, `surface`, `budget`, `entreprise_id`, `latitude`, `longitude`.
*   **Table `journal_sync`** :
    *   `id`, `action_type`, `payload`, `timestamp`, `synced_boolean`.

---

## 3. Scénarios d'Utilisation et Maquettes

### Scénario 1 : Consultation Grand Public (Visiteur)
**Acteur** : Citoyen non connecté.
**Flux** :
1.  L'utilisateur arrive sur la page d'accueil.
2.  Il consulte la carte interactive d'Antananarivo.
3.  Il survole les points pour voir les détails rapides (Type, Entreprise).
4.  Il clique sur l'onglet "Détails" pour voir les statistiques globales.

**Description des Écrans :**
> **Tableau de Bord Visiteur (Accueil)**
> *   **Barre de Navigation** : Logo "TanaTravaux" à gauche, boutons "Carte", "Détails" au centre, bouton "Login" à droite. Bouton changement de thème (Soleil/Lune).
> *   **Cartes de Statistiques (Haut)** : 4 cartes transparentes affichant "Signalements Total", "Surface Totale", "Avancement Global (%)", "Budget Total".
> *   **Carte Centrale** : Fond sombre stylisé (Dark Mode) avec des routes abstraites. Des icônes colorées (Marteau, Vagues, Attention) sont positionnées sur la carte.
>   *   *Interaction* : Au survol d'un point, une info-bulle "Glassmorphism" apparaît avec le détail du chantier.

> **Page Détails Statistiques**
> *   **Résumé** : Gros chiffres clés sur le budget et les surfaces.
> *   **Tableau** : Liste complète des travaux avec colonnes triables : Date, Type, Lieu, Status (badge coloré), Entreprise, Budget, Surface.

---

### Scénario 2 : Administration et Synchronisation (Manager)
**Acteur** : Manager authentifié.
**Flux** :
1.  Le manager se connecte via la page de login.
2.  Il accède au "Manager Panel".
3.  Il vérifie l'état de la synchronisation avec le serveur central (Firebase).
4.  Il modifie le statut d'un signalement de "NOUVEAU" à "EN_COURS".
5.  Il débloque un utilisateur qui a été restreint.

**Description des Écrans :**
> **Écran de Connexion**
> *   Carte centrale effet verre dépoli. Champ Email et Mot de passe. Bouton gradient "Se Connecter".

> **Tableau de Bord Manager (Onglet Synchronisation)**
> *   **Indicateurs** : État de la connexion Firestore (Connecté/Déconnecté), Nombre d'uploads en attente.
> *   **Actions** : Trois boutons principaux : "Synchroniser Tout" (Bleu), "Push" (Gris), "Pull" (Gris).

> **Gestion des Utilisateurs (Onglet Utilisateurs)**
> *   **Tableau** : Liste des inscrits (Nom, Email).
> *   **Contrôles** :
>     *   *Menu déroulant* pour le Rôle (USER/MANAGER).
>     *   *Bouton d'action* : "Bloquer" (Rouge) si actif, "Débloquer" (Vert) si bloqué.

> **Gestion des Signalements (Onglet Signalements)**
> *   **Filtres (Haut)** : Barre de recherche textuelle, Menu déroulant "Status" (Tous/Nouveau/...), Champ filtre "Entreprise".
> *   **Tableau Éditable** : Liste des travaux. Bouton "Modifier" ouvre un formulaire inline pour changer le budget, la surface ou le statut.
