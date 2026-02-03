package itu.cloud.firebase.services;

import itu.cloud.collections.FirestoreCollection;
import itu.cloud.collections.ParametreCollection;
import itu.cloud.entities.Journal;
import itu.cloud.firebase.enums.FirestoreOperator;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParametreFirebaseService extends FirestoreCollectionService<ParametreCollection> {
    public ParametreFirebaseService(FirebaseService firebaseService) {
        super(firebaseService, "parametres", ParametreCollection.class);
    }


    @Override
    public ParametreCollection saveFromJournal(Journal journal) throws Exception {
        if(journal.getOperation().equalsIgnoreCase("UPDATE"))
        {
            if(journal.getDonnees().get("nom") == null) {
                throw new Exception("Le champ 'nom' de l'entite 'ParametreCollection' journalisé est null");
            }

            String nom = (String) journal.getDonnees().get("nom");
            String valeur = (String) journal.getDonnees().get("valeur");
            List<ParametreCollection> parametres = findWhere("nom", FirestoreOperator.EQUALS, nom);
            if(parametres.size() == 0) {
                return super.saveFromJournal(journal);
            } else if(parametres.size() > 1) {
                throw new Exception("Deux paramètres du même nom trouvés dans Firestore lors de la journalisation");
            }
            ParametreCollection pc = parametres.get(0);
            pc.setValeur(valeur);
            pc = update(pc.getDocId(), pc);
            return pc;
        } else {
            return super.saveFromJournal(journal);
        }
    }
}
