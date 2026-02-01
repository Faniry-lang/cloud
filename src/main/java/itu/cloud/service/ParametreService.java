package itu.cloud.service;

import itu.cloud.entities.Parametre;
import itu.cloud.repositories.ParametreRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ParametreService {

    private final ParametreRepository parametreRepository;

    public ParametreService(ParametreRepository parametreRepository) {
        this.parametreRepository = parametreRepository;
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

        return parametreRepository.save(parametre);
    }
}
