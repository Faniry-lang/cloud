package itu.cloud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import itu.cloud.entities.Journal;
import itu.cloud.repositories.JournalRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class JournalService {

    private final JournalRepository journalRepository;
    private final ObjectMapper objectMapper;

    public JournalService(JournalRepository journalRepository) {
        this.journalRepository = journalRepository;
        this.objectMapper = new ObjectMapper();
    }

    public void journaliser(String typeEntite, String operation, String idEntite, Object donnees, Integer version) {
        Journal journal = new Journal();
        journal.setTypeEntite(typeEntite);
        journal.setOperation(operation);
        journal.setIdEntite(idEntite);

        @SuppressWarnings("unchecked")
        Map<String, Object> donneesMap = objectMapper.convertValue(donnees, Map.class);
        journal.setDonnees(donneesMap);

        journal.setVersion(version);
        journal.setDateCreation(LocalDateTime.now());
        journal.setSynchronise(false);

        journalRepository.save(journal);
    }

    public void marquerCommeSynchronise(Integer journalId) {
        Journal journal = journalRepository.findById(journalId)
                .orElseThrow(() -> new RuntimeException("Journal non trouvé"));
        journal.setSynchronise(true);
        journalRepository.save(journal);
    }
}
