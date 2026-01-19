package itu.cloud.repositories;

import itu.cloud.entities.StatutsSignalement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatutsSignalementRepository extends JpaRepository<StatutsSignalement, Integer> {
}