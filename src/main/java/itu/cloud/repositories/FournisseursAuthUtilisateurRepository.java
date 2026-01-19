package itu.cloud.repositories;

import itu.cloud.entities.FournisseursAuthUtilisateur;
import itu.cloud.entities.FournisseursAuthUtilisateurId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FournisseursAuthUtilisateurRepository extends JpaRepository<FournisseursAuthUtilisateur, FournisseursAuthUtilisateurId> {
}