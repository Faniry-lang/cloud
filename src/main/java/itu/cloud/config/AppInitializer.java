package itu.cloud.config;

import itu.cloud.collections.UtilisateurCollection;
import itu.cloud.dto.RegisterRequest;
import itu.cloud.entities.Role;
import itu.cloud.entities.RoleUtilisateur;
import itu.cloud.entities.StatutSignalement;
import itu.cloud.entities.Utilisateur;
import itu.cloud.exceptions.FirebaseAuthentificationException;
import itu.cloud.exceptions.FirebaseUnavailableException;
import itu.cloud.firebase.services.FirebaseService;
import itu.cloud.firebase.services.UtilisateurFirebaseService;
import itu.cloud.repositories.RoleUtilisateurRepository;
import itu.cloud.service.*;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Component
@DependsOn("firebaseConfig")
public class AppInitializer {

    private final UtilisateurService utilisateurService;
    private final UtilisateurFirebaseService utilsateurFirebaseService;
    private final FirebaseService firebaseService;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
    private final RoleUtilisateurRepository roleUtilisateurRepository;
    private String LOCAL_PWD = "tanatravaux";
    private String MANAGER_NAME = "Tana Travaux Manager";
    private String MANAGER_EMAIL = "tanatravaux@gmail.com";
    private String MANAGER_UID = "Lvhjr8D9juSLv9majX4ZkgG8ZuF3";
    private String MANAGER_DOC_ID = "My5f0B9oaBto2TW26bQj";

    public AppInitializer(UtilisateurService utilisateurService, UtilisateurFirebaseService utilsateurFirebaseService, FirebaseService firebaseService, PasswordEncoder passwordEncoder, RoleService roleService, RoleUtilisateurRepository roleUtilisateurRepository) {
        this.utilisateurService = utilisateurService;
        this.utilsateurFirebaseService = utilsateurFirebaseService;
        this.firebaseService = firebaseService;
        this.passwordEncoder = passwordEncoder;
        this.roleService = roleService;
        this.roleUtilisateurRepository = roleUtilisateurRepository;
    }

    @PostConstruct
    public void initializeDataInDb() throws Exception {

        Utilisateur u = isManagerPresentInDb();
        boolean managerIsPresentInDb = u != null ? true : false;

        if(managerIsPresentInDb) {
            return;
        } else  {
            initManagerInDb(MANAGER_DOC_ID, MANAGER_UID);
        }
    }

    public Utilisateur isManagerPresentInDb() {
        Optional<Utilisateur> u = utilisateurService.findByEmail(MANAGER_EMAIL);
        if(u.isPresent()) {
            return u.get();
        }
        return null;
    }
    
    public Utilisateur initManagerInDb(String docId, String firebaseUid) {
        Utilisateur u = new Utilisateur();
        u.setNom(MANAGER_NAME);
        u.setEmail(MANAGER_EMAIL);
        u.setDocId(docId);
        u.setFirebaseUid(firebaseUid);
        u.setVersion(1);
        u.setActif(true);
        u.setTentativesEchouees(0);
        u.setMotDePasseHash(passwordEncoder.encode(LOCAL_PWD));
        u.setDateCreation(LocalDateTime.now());

        u = utilisateurService.save(u);

        Role managerRole = roleService.findOrCreateRole("MANAGER");

        RoleUtilisateur roleUtilisateur = new RoleUtilisateur();
        roleUtilisateur.setIdUtilisateur(u);
        roleUtilisateur.setIdRole(managerRole);
        roleUtilisateur.setDateCreation(LocalDateTime.now());
        roleUtilisateurRepository.save(roleUtilisateur);

        return u;
    }
}
