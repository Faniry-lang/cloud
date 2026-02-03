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
