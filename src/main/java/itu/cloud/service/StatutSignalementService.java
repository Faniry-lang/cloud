package itu.cloud.service;

import itu.cloud.entities.StatutSignalement;
import itu.cloud.repositories.StatutSignalementRepository;
import org.springframework.stereotype.Service;

@Service
public class StatutSignalementService extends BaseService<StatutSignalementRepository, StatutSignalement, Integer> {

    public StatutSignalementService(StatutSignalementRepository statutSignalementRepository) {
        this.repository = statutSignalementRepository;
    }
}
