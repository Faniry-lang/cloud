package itu.cloud.repositories;

import itu.cloud.entities.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {
    Optional<Utilisateur> findByEmail(String email);
    Optional<Utilisateur> findByFirebaseUid(String firebaseUid);

    @Query("SELECT u FROM Utilisateur u WHERE u.bloqueJusqua IS NOT NULL AND u.bloqueJusqua > :now")
    List<Utilisateur> findBlockedUsers(LocalDateTime now);
}