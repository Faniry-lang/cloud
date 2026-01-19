package itu.cloud.repositories;

import itu.cloud.entities.Signalement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SignalementRepository extends JpaRepository<Signalement, UUID> {
}