package itu.cloud.repositories;

import itu.cloud.entities.Signalement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SignalementRepository extends JpaRepository<Signalement, Integer> {
}