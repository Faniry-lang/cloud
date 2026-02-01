package itu.cloud.service;

import itu.cloud.entities.Role;
import itu.cloud.repositories.RoleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    @Value("${APP_ROLE}")
    private String appRoles;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<String> getValidRoles() {
        return Arrays.asList(appRoles.split(","));
    }

    public boolean isValidRole(String roleName) {
        return getValidRoles().contains(roleName);
    }

    public Role findOrCreateRole(String roleName) {
        if (!isValidRole(roleName)) {
            throw new RuntimeException("Role invalide");
        }

        Optional<Role> existingRole = roleRepository.findByNom(roleName);

        if (existingRole.isPresent()) {
            return existingRole.get();
        }

        Role newRole = new Role();
        newRole.setNom(roleName);
        return roleRepository.save(newRole);
    }

    public boolean isManagerRole(String roleName) {
        return "MANAGER".equals(roleName) || "MANAGER_WEB".equals(roleName);
    }
}
