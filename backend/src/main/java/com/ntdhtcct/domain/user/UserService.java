package com.ntdhtcct.domain.user;

import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class UserService {

    // Số lần đăng nhập sai tối đa
    private static final int MAX_FAILED_ATTEMPTS = 5;

    // Thời gian khóa tài khoản
    private static final long LOCK_DURATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Đăng nhập
     */
    public User login(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Email hoặc password không chính xác"
                        )
                );

        // Kiểm tra tài khoản có đang bị khóa hay không
        if (user.getLockedUntil() != null
                && user.getLockedUntil().isAfter(OffsetDateTime.now())) {

            throw new RuntimeException(
                    "Tài khoản đang bị khóa. Vui lòng thử lại sau."
            );
        }

        // Kiểm tra password
        if (!passwordEncoder.matches(
                password,
                user.getPassword()
        )) {

            int failedAttempts =
                    user.getFailedLoginAttempts() + 1;

            user.setFailedLoginAttempts(failedAttempts);

            // Sai đủ 5 lần → khóa 15 phút
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {

                OffsetDateTime lockedUntil =
                        OffsetDateTime.now()
                                .plusMinutes(
                                        LOCK_DURATION_MINUTES
                                );

                user.setLockedUntil(lockedUntil);
                user.setUpdatedAt(OffsetDateTime.now());

                userRepository.save(user);

                throw new RuntimeException(
                        "Tài khoản đã bị khóa trong 15 phút " +
                        "do đăng nhập sai quá nhiều lần"
                );
            }

            user.setUpdatedAt(OffsetDateTime.now());
            userRepository.save(user);

            throw new RuntimeException(
                    "Email hoặc password không chính xác"
            );
        }

        // Đăng nhập thành công
        // Reset số lần đăng nhập sai
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setUpdatedAt(OffsetDateTime.now());

        userRepository.save(user);

        return user;
    }

    /**
     * Lấy user theo ID
     *
     * Dùng cho API /api/auth/me
     */
    public User findById(UUID userId) {

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy người dùng"
                        )
                );
    }
}
