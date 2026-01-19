package itu.cloud.repositories;

import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AbstractPersistableRepository<T extends AbstractPersistable> extends JpaRepository<T, PK> {
}