package com.ntdhtcct.domain.user;

import com.ntdhtcct.entity.Role;
import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.RoleRepository;
import com.ntdhtcct.repository.UserRepository;
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

        // Nếu đã có tài khoản test thì không tạo lại
        if (userRepository.existsByEmail("test@test.com")) {
            return;
        }

        // Lấy role CUSTOMER, nếu chưa có thì tạo
        Role role = roleRepository.findByName("CUSTOMER")
                .orElseGet(() ->
                        roleRepository.save(new Role("CUSTOMER"))
                );

        // Tạo user test
        User user = new User();

        user.setEmail("test@test.com");
        user.setPassword(passwordEncoder.encode("12345678"));
        user.setFullName("Test User");
        user.setRole(role);

        userRepository.save(user);
    }
}