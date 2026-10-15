package com.ntdhtcct.domain.user;

import com.ntdhtcct.entity.Role;
import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.RoleRepository;
import com.ntdhtcct.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class UserSeeder implements CommandLineRunner {

    private static final String DEFAULT_PASSWORD = "12345678";

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
        ensureUser("test@test.com", "Test User", "CUSTOMER");

        ensureUser("lanc5676@gmail.com", "Lanc", "PROJECT_MANAGER");
        ensureUser("linh.work@gmail.com", "Linh", "PROJECT_MANAGER");
        ensureUser("kien.work@gmail.com", "Kiên", "PROJECT_MANAGER");
        ensureUser("bach.work@gmail.com", "Bách", "PROJECT_MANAGER");
        ensureUser("chinh.work@gmail.com", "Chinh", "PROJECT_MANAGER");
        ensureUser("lan.work@gmail.com", "Lan", "PROJECT_MANAGER");
    }

    private void ensureUser(String email, String fullName, String roleName) {
        if (!userRepository.existsByEmailIgnoreCase(email)) {
            Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName)));

            User user = new User(
                email,
                passwordEncoder.encode(DEFAULT_PASSWORD),
                fullName
            );
            user.setRole(role);
            userRepository.save(user);
            return;
        }

        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            roleRepository.findByName(roleName).ifPresent(role -> {
                user.setRole(role);
                userRepository.save(user);
            });
        });
    }
}
