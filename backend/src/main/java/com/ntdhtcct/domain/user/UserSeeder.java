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

        User user = new User(
            "test@test.com",
            passwordEncoder.encode("12345678"),
            "Test User"
        );
        user.setRole(role);

        userRepository.save(user);
    }
}
