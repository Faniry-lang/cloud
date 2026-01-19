package itu.cloud.repositories;

import itu.cloud.entities.Journal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalRepository extends JpaRepository<Journal, Integer> {
}