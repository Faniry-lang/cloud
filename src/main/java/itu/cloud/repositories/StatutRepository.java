package itu.cloud.repositories;

import itu.cloud.entities.Statut;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatutRepository extends JpaRepository<Statut, Integer> {
}