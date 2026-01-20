package itu.cloud.repositories;

import itu.cloud.entities.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {
    Optional<Utilisateur> findByEmail(String email);
    boolean existsByEmail(String email);

    /**
     * Trouve tous les utilisateurs bloqués (bloqueJusqua > maintenant)
     */
    List<Utilisateur> findByBloqueJusquaAfterAndDateSuppressionIsNull(Instant now);

    /**
     * Trouve tous les utilisateurs bloqués (bloqueJusqua non null)
     */
    List<Utilisateur> findByBloqueJusquaIsNotNullAndDateSuppressionIsNull();
}