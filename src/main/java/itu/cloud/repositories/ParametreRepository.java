package itu.cloud.repositories;

import itu.cloud.entities.Parametre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParametreRepository extends JpaRepository<Parametre, Integer> {
    Optional<Parametre> findByNom(String nom);
}