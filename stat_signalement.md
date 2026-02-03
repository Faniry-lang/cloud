# Fonctionnalité : Statistiques de Traitement des Signalements

## Vue d'ensemble

Cette fonctionnalité permet de calculer et d'afficher le **délai de traitement moyen** des travaux (signalements) depuis leur création (statut NOUVEAU) jusqu'à leur achèvement (statut TERMINÉ).

## Architecture

L'implémentation suit l'architecture MVC avec les couches suivantes :
- **Repository** : Requête SQL native pour calculer la moyenne
- **DTO** : Objet de transfert de données pour la réponse
- **Service** : Logique métier et formatage
- **Controller** : Exposition de l'endpoint REST API

---

## 1. Couche Repository

**Fichier :** `HistoriqueStatutSignalementRepository.java`

```java
package itu.cloud.repositories;

import itu.cloud.entities.HistoriqueStatutSignalement;
import itu.cloud.entities.Signalement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HistoriqueStatutSignalementRepository extends JpaRepository<HistoriqueStatutSignalement, Integer> {
    Optional<HistoriqueStatutSignalement> findFirstByIdSignalementOrderByDateCreationDesc(Signalement signalement);

    @Query(value = """
                SELECT AVG(EXTRACT(EPOCH FROM (h2.date_creation - h1.date_creation)))
                FROM historique_statut_signalement h1
                JOIN historique_statut_signalement h2 ON h1.id_signalement = h2.id_signalement
                JOIN statut_signalement s1 ON h1.id_statut_signalement = s1.id
                JOIN statut_signalement s2 ON h2.id_statut_signalement = s2.id
                WHERE s1.niveau = 1 AND s2.niveau = 3
            """, nativeQuery = true)
    Double getAverageProcessingTimeInSeconds();
}
```

**Explication de la requête SQL :**
- Calcule la différence de temps entre deux historiques d'un même signalement
- `h1` : Historique avec statut niveau 1 (NOUVEAU)
- `h2` : Historique avec statut niveau 3 (TERMINÉ)
- `EXTRACT(EPOCH FROM ...)` : Convertit la différence en secondes
- `AVG(...)` : Calcule la moyenne sur tous les signalements traités

---

## 2. Couche DTO (Data Transfer Object)

**Fichier :** `StatistiquesTraitementDTO.java`

```java
package itu.cloud.dto;

import lombok.Data;

@Data
public class StatistiquesTraitementDTO {
    private Double dureeMoyenneSecondes;
    private String dureeMoyenneLisible;
}
```

**Champs :**
- `dureeMoyenneSecondes` : Durée en secondes (format brut)
- `dureeMoyenneLisible` : Durée formatée en texte lisible (ex: "3 jours 2 h 15 min")

---

## 3. Couche Service

**Fichier :** `SignalementService.java`

```java
@Transactional(readOnly = true)
public StatistiquesTraitementDTO getStatistiquesTraitement() {
    Double avgSeconds = historiqueRepository.getAverageProcessingTimeInSeconds();
    StatistiquesTraitementDTO stats = new StatistiquesTraitementDTO();

    if (avgSeconds == null) {
        stats.setDureeMoyenneSecondes(0.0);
        stats.setDureeMoyenneLisible("Aucune donnée suffisante");
        return stats;
    }

    stats.setDureeMoyenneSecondes(avgSeconds);
    stats.setDureeMoyenneLisible(formatDuration(avgSeconds.longValue()));
    return stats;
}

private String formatDuration(long seconds) {
    long days = seconds / (24 * 3600);
    long hours = (seconds % (24 * 3600)) / 3600;
    long minutes = (seconds % 3600) / 60;

    StringBuilder sb = new StringBuilder();
    if (days > 0)
        sb.append(days).append(" jours ");
    if (hours > 0)
        sb.append(hours).append(" h ");
    if (minutes > 0)
        sb.append(minutes).append(" min");

    if (sb.isEmpty())
        return "Moins d'une minute";
    return sb.toString().trim();
}
```

**Logique :**
1. Récupère la moyenne en secondes depuis le repository
2. Gère le cas où aucune donnée n'est disponible
3. Formate la durée en format lisible (jours, heures, minutes)

---

## 4. Couche Controller

**Fichier :** `SignalementController.java`

```java
@GetMapping("/stats/traitement")
public ResponseEntity<StatistiquesTraitementDTO> getStatistiquesTraitement() {
    return ResponseEntity.ok(signalementService.getStatistiquesTraitement());
}
```

**Import nécessaire :**
```java
import itu.cloud.dto.StatistiquesTraitementDTO;
```

---

## 5. Utilisation de l'API

### Endpoint
```
GET /api/signalements/stats/traitement
```

### Exemple de requête avec cURL
```bash
curl http://localhost:8080/api/signalements/stats/traitement
```

### Exemple de réponse JSON

**Cas avec données :**
```json
{
  "dureeMoyenneSecondes": 259200.5,
  "dureeMoyenneLisible": "3 jours"
}
```

**Cas sans données :**
```json
{
  "dureeMoyenneSecondes": 0.0,
  "dureeMoyenneLisible": "Aucune donnée suffisante"
}
```

**Autre exemple :**
```json
{
  "dureeMoyenneSecondes": 91800.0,
  "dureeMoyenneLisible": "1 jours 1 h 30 min"
}
```

---

## 6. Prérequis Base de Données

Cette fonctionnalité nécessite :
- Table `historique_statut_signalement` avec les champs :
  - `id_signalement` : Référence au signalement
  - `id_statut_signalement` : Référence au statut
  - `date_creation` : Date de création de l'historique
  
- Table `statut_signalement` avec :
  - `id` : Identifiant du statut
  - `niveau` : Niveau du statut (1=NOUVEAU, 2=EN_COURS, 3=TERMINÉ)

---

## 7. Cas d'Usage

**Scénario 1 : Tableau de bord de gestion**
```javascript
fetch('/api/signalements/stats/traitement')
  .then(response => response.json())
  .then(data => {
    console.log(`Délai moyen : ${data.dureeMoyenneLisible}`);
  });
```

**Scénario 2 : Rapport de performance**
- Permet aux gestionnaires de surveiller l'efficacité du traitement
- Identifie les goulots d'étranglement si le délai augmente
- Compare les performances sur différentes périodes

---

## 8. Améliorations Futures Possibles

1. **Statistiques par période** : Filtrer par mois/année
2. **Statistiques par type de signalement** : Comparer les délais par catégorie
3. **Statistiques par entreprise** : Performance de chaque entreprise
4. **Percentiles** : P50, P95, P99 pour mieux comprendre la distribution
5. **Tendances** : Évolution du délai moyen dans le temps

---

## Date d'implémentation
**3 février 2026**
