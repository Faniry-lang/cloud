package itu.cloud.repositories;

import itu.cloud.entities.HistoriqueStatutSignalement;
import itu.cloud.entities.Signalement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HistoriqueStatutSignalementRepository extends JpaRepository<HistoriqueStatutSignalement, Integer> {
    Optional<HistoriqueStatutSignalement> findFirstByIdSignalementOrderByDateCreationDesc(Signalement signalement);
}