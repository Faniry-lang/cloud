package itu.cloud.repositories;

import itu.cloud.entities.RolesUtilisateur;
import itu.cloud.entities.RolesUtilisateurId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolesUtilisateurRepository extends JpaRepository<RolesUtilisateur, RolesUtilisateurId> {
}