package itu.cloud.service;

import itu.cloud.entities.Role;
import itu.cloud.entities.RoleUtilisateur;
import itu.cloud.entities.Utilisateur;
import itu.cloud.repositories.RoleUtilisateurRepository;
import itu.cloud.repositories.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final ParametreService parametreService;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
    private final RoleUtilisateurRepository roleUtilisateurRepository;

    public UtilisateurService(UtilisateurRepository utilisateurRepository,
                             ParametreService parametreService,
                             PasswordEncoder passwordEncoder,
                             RoleService roleService,
                             RoleUtilisateurRepository roleUtilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.parametreService = parametreService;
        this.passwordEncoder = passwordEncoder;
        this.roleService = roleService;
        this.roleUtilisateurRepository = roleUtilisateurRepository;
    }

    public Map<String, String> registerWithDatabase(String email, String password, String nom, String roleName) {
        if (utilisateurRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà");
        }

        Role role = roleService.findOrCreateRole(roleName);

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(email);
        utilisateur.setNom(nom != null ? nom : email);
        utilisateur.setMotDePasseHash(passwordEncoder.encode(password));
        utilisateur.setActif(true);
        utilisateur.setTentativesEchouees(0);
        utilisateur.setVersion(1);
        utilisateur.setDateCreation(LocalDateTime.now());

        Utilisateur savedUser = utilisateurRepository.save(utilisateur);

        RoleUtilisateur roleUtilisateur = new RoleUtilisateur();
        roleUtilisateur.setIdUtilisateur(savedUser);
        roleUtilisateur.setIdRole(role);
        roleUtilisateur.setDateCreation(LocalDateTime.now());
        roleUtilisateurRepository.save(roleUtilisateur);

        Map<String, String> userData = new HashMap<>();
        userData.put("email", savedUser.getEmail());
        userData.put("displayName", savedUser.getNom());
        userData.put("localId", String.valueOf(savedUser.getId()));
        userData.put("role", roleName);

        return userData;
    }

    public Map<String, String> authenticateWithDatabase(String email, String password) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (utilisateur.getBloqueJusqua() != null &&
            utilisateur.getBloqueJusqua().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Compte bloqué jusqu'à " + utilisateur.getBloqueJusqua());
        }

        if (utilisateur.getActif() == null || !utilisateur.getActif()) {
            throw new RuntimeException("Compte inactif");
        }

        if (utilisateur.getMotDePasseHash() == null ||
            !passwordEncoder.matches(password, utilisateur.getMotDePasseHash())) {
            handleFailedLogin(utilisateur);
            throw new RuntimeException("Identifiants invalides");
        }

        if (utilisateur.getTentativesEchouees() != null && utilisateur.getTentativesEchouees() > 0) {
            utilisateur.setTentativesEchouees(0);
            utilisateur.setBloqueJusqua(null);
            utilisateurRepository.save(utilisateur);
        }

        List<RoleUtilisateur> rolesUtilisateur = roleUtilisateurRepository.findByIdUtilisateur(utilisateur);
        String roleName = null;
        if (!rolesUtilisateur.isEmpty()) {
            roleName = rolesUtilisateur.get(0).getIdRole().getNom();
        }

        Map<String, String> userData = new HashMap<>();
        userData.put("email", utilisateur.getEmail());
        userData.put("displayName", utilisateur.getNom() != null ? utilisateur.getNom() : utilisateur.getEmail());
        userData.put("localId", utilisateur.getFirebaseUid() != null ? utilisateur.getFirebaseUid() : String.valueOf(utilisateur.getId()));
        userData.put("role", roleName);

        return userData;
    }

    private void handleFailedLogin(Utilisateur utilisateur) {
        Integer maxFailedAttempts = parametreService.getValeurAsInteger("MAX_FAILED_ATTEMPTS");
        Integer blockDurationMinutes = parametreService.getValeurAsInteger("BLOCK_DURATION_MINUTES");

        if (maxFailedAttempts == null) maxFailedAttempts = 5;
        if (blockDurationMinutes == null) blockDurationMinutes = 30;
        int tentatives = utilisateur.getTentativesEchouees() != null ? utilisateur.getTentativesEchouees() : 0;
        tentatives++;
        utilisateur.setTentativesEchouees(tentatives);
        if (tentatives >= maxFailedAttempts) {
            utilisateur.setBloqueJusqua(LocalDateTime.now().plusMinutes(blockDurationMinutes));
        }

        utilisateurRepository.save(utilisateur);
    }
}
