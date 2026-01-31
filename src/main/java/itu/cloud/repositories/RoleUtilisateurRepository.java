package itu.cloud.repositories;

import itu.cloud.entities.RoleUtilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleUtilisateurRepository extends JpaRepository<RoleUtilisateur, Integer> {
}