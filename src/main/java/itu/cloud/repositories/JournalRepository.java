package itu.cloud.repositories;

import itu.cloud.entities.Journal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JournalRepository extends JpaRepository<Journal, UUID> {
}