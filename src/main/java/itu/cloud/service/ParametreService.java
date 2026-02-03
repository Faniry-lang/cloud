package itu.cloud.service;

import itu.cloud.collections.ParametreCollection;
import itu.cloud.entities.Parametre;
import itu.cloud.repositories.ParametreRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ParametreService {

    private final ParametreRepository parametreRepository;
    private final JournalService journalService;

    public ParametreService(ParametreRepository parametreRepository, JournalService journalService) {
        this.parametreRepository = parametreRepository;
        this.journalService = journalService;
    }

    public String getValeur(String nom) {
        return parametreRepository.findByNom(nom)
                .map(Parametre::getValeur)
                .orElse(null);
    }

    public Integer getValeurAsInteger(String nom) {
        String valeur = getValeur(nom);
        return valeur != null ? Integer.parseInt(valeur) : null;
    }

    public List<Parametre> getAllParametres() {
        return parametreRepository.findAll();
    }

    public Parametre getParametreById(Integer id) {
        return parametreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paramètre non trouvé"));
    }

    public Parametre updateParametre(Integer id, String nouvelleValeur) {
        Parametre parametre = parametreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paramètre non trouvé"));

        parametre.setValeur(nouvelleValeur);
        parametre.setDateMisAJour(LocalDateTime.now());

        ParametreCollection pc = new ParametreCollection();
        pc.setId(parametre.getId());
        pc.setType(parametre.getType());
        pc.setNom(parametre.getNom());
        pc.setValeur(parametre.getValeur());
        pc.setDateMisAJour(LocalDateTime.now().toString());

        journalService.journaliser(
                "ParametreCollection",
                "UPDATE",
                parametre.getId().toString(),
                pc,
                1
        );

        return parametreRepository.save(parametre);
    }
}
