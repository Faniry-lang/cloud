package itu.cloud.repositories;

import org.springframework.data.jpa.domain.AbstractAuditable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AbstractAuditableRepository<T extends AbstractAuditable> extends JpaRepository<T, PK> {
}