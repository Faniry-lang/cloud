package itu.cloud.repositories;

import itu.cloud.entities.Journal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JournalRepository extends JpaRepository<Journal, Integer> {
    List<Journal> findBySynchroniseFalse();
    long countBySynchroniseFalse();
}