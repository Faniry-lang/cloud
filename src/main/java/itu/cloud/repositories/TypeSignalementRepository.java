package itu.cloud.repositories;

import itu.cloud.entities.TypeSignalement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TypeSignalementRepository extends JpaRepository<TypeSignalement, Integer> {
    Optional<TypeSignalement> findByFirebaseUid(String firebaseUid);
}