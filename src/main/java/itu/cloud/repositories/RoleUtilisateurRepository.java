package itu.cloud.repositories;

import itu.cloud.entities.RoleUtilisateur;
import itu.cloud.entities.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleUtilisateurRepository extends JpaRepository<RoleUtilisateur, Integer> {
    List<RoleUtilisateur> findByIdUtilisateur(Utilisateur utilisateur);
}