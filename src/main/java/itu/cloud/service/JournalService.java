package itu.cloud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import itu.cloud.entities.Journal;
import itu.cloud.repositories.JournalRepository;
import lombok.Getter;
import lombok.Setter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class JournalService extends BaseService<JournalRepository, Journal, Integer> {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public JournalService(JournalRepository journalRepository, SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.repository = journalRepository;
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

        repository.save(journal);

        messagingTemplate.convertAndSend(
                "/topic/journal",
                new HistoriqueJournal(journal)
        );
    }

    public void marquerCommeSynchronise(Integer journalId) {
        Journal journal = repository.findById(journalId)
                .orElseThrow(() -> new RuntimeException("Journal non trouvé"));
        journal.setSynchronise(true);
        repository.save(journal);
    }

    public int getJournalNonSync() {
        List<Journal> journalList = repository.findBySynchroniseFalse();
        return journalList.size();
    }

    public List<HistoriqueJournal> getAllJournalHistoryUnsync() {
        List<Journal> journalList = repository.findBySynchroniseFalse();
        List<HistoriqueJournal> historiqueJournals = new ArrayList<>();
        for(Journal journal : journalList) {
            historiqueJournals.add(new HistoriqueJournal(journal));
        }
        return historiqueJournals;
    }

    @Getter
    @Setter
    public class HistoriqueJournal {
        String description;

        public HistoriqueJournal(Journal journal) {
            String operation = journal.getOperation();
            String entite = journal.getTypeEntite().replaceAll("Collection", "");
            LocalDateTime time = journal.getDateCreation();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH-mm dd-MM-yyyy");
            String formatted = time.format(formatter);
            switch(operation) {
                case "INSERT":
                    description = "Création d'un objet "+entite+" à "+formatted;
                    break;
                case "UPDATE":
                    description = "Mise à jour d'un objet "+entite+" à "+formatted;
                    break;
                case "DELETE":
                    description = "Suppression d'un objet "+entite+" à "+formatted;
                    break;
                default:
                    description = "Operation du journal non synchronisée";
                    break;
            }
        }
    }
}
