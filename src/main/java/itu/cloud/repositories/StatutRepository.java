package itu.cloud.repositories;

import itu.cloud.entities.Statut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StatutRepository extends JpaRepository<Statut, Integer> {
    Optional<Statut> findByDescription(String description);
}