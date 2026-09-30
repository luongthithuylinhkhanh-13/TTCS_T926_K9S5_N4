package com.ntdhtcct.domain.user;

import com.ntdhtcct.domain.role.Role;
import com.ntdhtcct.domain.role.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;

@Service
public class UserService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String fullName, String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalStateException("Email này đã được đăng ký.");
        }

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseGet(() -> roleRepository.save(new Role("CUSTOMER")));

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName.trim());
        user.setRoleId(customerRole.getId());
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        return userRepository.save(user);
    }

    public User login(String email, String password) {

        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() ->
                        new RuntimeException("Email hoặc password không chính xác"));

        // Kiểm tra tài khoản có đang bị khóa hay không
        if (user.getLockedUntil() != null
                && user.getLockedUntil().isAfter(OffsetDateTime.now())) {

            throw new RuntimeException(
                    "Tài khoản đang bị khóa. Vui lòng thử lại sau.");
        }

        // Kiểm tra password
        if (!passwordEncoder.matches(password, user.getPassword())) {

            int failedAttempts = user.getFailedLoginAttempts() + 1;

            user.setFailedLoginAttempts(failedAttempts);

            // Sai đủ 5 lần → khóa 15 phút
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
          OffsetDateTime lockedUntil = OffsetDateTime.now()
            .plusMinutes(LOCK_DURATION_MINUTES);

             user.setLockedUntil(lockedUntil);
             user.setUpdatedAt(OffsetDateTime.now());

             userRepository.save(user);

             throw new RuntimeException(
             "Tài khoản đã bị khóa trong 15 phút do đăng nhập sai quá nhiều lần"
    );
}

            user.setUpdatedAt(OffsetDateTime.now());
            userRepository.save(user);

            throw new RuntimeException(
                    "Email hoặc password không chính xác");
        }

        // Đăng nhập thành công → reset số lần sai
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setUpdatedAt(OffsetDateTime.now());

        userRepository.save(user);

        return user;
    }
}