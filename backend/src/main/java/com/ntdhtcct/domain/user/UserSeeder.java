package com.ntdhtcct.domain.user;

import com.ntdhtcct.domain.role.Role;
import com.ntdhtcct.domain.role.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserSeeder(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmailIgnoreCase("test@test.com")) {
            return;
        }

        Role role = roleRepository.findByName("CUSTOMER")
                .orElseGet(() -> roleRepository.save(new Role("CUSTOMER")));

        User user = new User();
        user.setEmail("test@test.com");
        user.setPassword(passwordEncoder.encode("12345678"));
        user.setFullName("Test User");
        user.setRoleId(role.getId());

        userRepository.save(user);
    }
}
