package com.ntdhtcct.domain.role;

import com.ntdhtcct.entity.Role;
import com.ntdhtcct.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RoleSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public RoleSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {

        createRoleIfNotExists("ADMIN");
        createRoleIfNotExists("STAFF");
        createRoleIfNotExists("CUSTOMER");
    }

    private void createRoleIfNotExists(String roleName) {

        if (!roleRepository.existsByName(roleName)) {
            roleRepository.save(new Role(roleName));
        }
    }
}