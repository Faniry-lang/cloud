package itu.cloud.service;

import itu.cloud.entities.TypeSignalement;
import itu.cloud.repositories.TypeSignalementRepository;
import org.springframework.stereotype.Service;

@Service
public class TypeSignalementService extends BaseService<TypeSignalementRepository, TypeSignalement, Integer> {
    public TypeSignalementService(TypeSignalementRepository typeSignalementRepository) {
        this.repository = typeSignalementRepository;
    }
}
