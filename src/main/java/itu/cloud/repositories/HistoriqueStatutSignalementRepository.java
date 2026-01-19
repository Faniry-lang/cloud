package itu.cloud.repositories;

import itu.cloud.entities.HistoriqueStatutSignalement;
import itu.cloud.entities.HistoriqueStatutSignalementId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoriqueStatutSignalementRepository extends JpaRepository<HistoriqueStatutSignalement, HistoriqueStatutSignalementId> {
}