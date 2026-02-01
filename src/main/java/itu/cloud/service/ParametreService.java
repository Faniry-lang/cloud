package itu.cloud.service;

import itu.cloud.entities.Parametre;
import itu.cloud.repositories.ParametreRepository;
import org.springframework.stereotype.Service;

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
}
