package itu.cloud.repositories;

import itu.cloud.entities.StatutSignalement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatutSignalementRepository extends JpaRepository<StatutSignalement, Integer> {
    Optional<StatutSignalement> findByNiveau(Integer niveau);
}