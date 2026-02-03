package itu.cloud.service;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public class BaseService<R extends JpaRepository<E, T>, E, T> {
    R repository;

    public List<E> findAll() {
        return this.repository.findAll();
    }

    public E findById(T id) {
        Optional<E> entity = this.repository.findById(id);
        return entity.orElse(null);
    }

    public E save(E entity) {
        return this.repository.save(entity);
    }

    public void delete(E entity) {
        this.repository.delete(entity);
    }
}
