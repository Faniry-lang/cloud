package itu.cloud.repositories;

import itu.cloud.entities.Signalement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignalementRepository extends JpaRepository<Signalement, Integer> {
}