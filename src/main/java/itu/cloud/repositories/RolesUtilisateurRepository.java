package itu.cloud.repositories;

import itu.cloud.entities.RolesUtilisateur;
import itu.cloud.entities.RolesUtilisateurId;
import itu.cloud.entities.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolesUtilisateurRepository extends JpaRepository<RolesUtilisateur, RolesUtilisateurId> {
    List<RolesUtilisateur> findByIdUtilisateurAndDateSuppressionIsNull(Utilisateur utilisateur);
}