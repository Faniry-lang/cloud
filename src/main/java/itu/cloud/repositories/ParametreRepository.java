package itu.cloud.repositories;

import itu.cloud.entities.Parametre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParametreRepository extends JpaRepository<Parametre, Integer> {
    Optional<Parametre> findByNomAndDateSuppressionIsNull(String nom);
}