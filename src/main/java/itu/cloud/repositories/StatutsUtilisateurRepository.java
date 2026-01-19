package itu.cloud.repositories;

import itu.cloud.entities.StatutsUtilisateur;
import itu.cloud.entities.StatutsUtilisateurId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatutsUtilisateurRepository extends JpaRepository<StatutsUtilisateur, StatutsUtilisateurId> {
}