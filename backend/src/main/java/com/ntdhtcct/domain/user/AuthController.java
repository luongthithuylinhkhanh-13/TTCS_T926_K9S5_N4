package com.ntdhtcct.domain.user;

import com.ntdhtcct.domain.auth.AuthToken;
import com.ntdhtcct.domain.auth.AuthTokenService;
import com.ntdhtcct.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthTokenService authTokenService;

    public AuthController(
            UserService userService,
            AuthTokenService authTokenService
    ) {
        this.userService = userService;
        this.authTokenService = authTokenService;
    }

    /**
     * =========================
     * LOGIN
     * POST /api/auth/login
     * =========================
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request
    ) {

        try {

            User user = userService.login(
                    request.email(),
                    request.password()
            );

            AuthToken authToken =
                    authTokenService.createToken(user.getId());

            return ResponseEntity.ok(
                    new LoginResponse(
                            true,
                            "Đăng nhập thành công",
                            user.getId(),
                            user.getEmail(),
                            user.getRole() != null
                                    ? user.getRole().getId()
                                    : null,
                            user.getRole() != null
                                    ? user.getRole().getName()
                                    : null,
                            user.getFullName(),
                            authToken.getToken(),
                            authToken.getExpiresAt()
                    )
            );

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest().body(
                    new ErrorResponse(
                            false,
                            e.getMessage()
                    )
            );
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegistrationRequest request
    ) {
        if (!request.password().equals(request.confirmPassword())) {
            return ResponseEntity.badRequest().body(
                    new ErrorResponse(false, "Mật khẩu xác nhận không khớp.")
            );
        }

        try {
            User user = userService.register(
                    request.fullName(),
                    request.email(),
                    request.password()
            );
            return ResponseEntity.status(201).body(
                    new RegistrationResponse(
                            true,
                            "Đăng ký thành công. Bạn có thể đăng nhập.",
                            user.getEmail()
                    )
            );
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(
                    new ErrorResponse(false, e.getMessage())
            );
        }
    }

        /**
         * =========================
         * LOGOUT
         * POST /api/auth/logout
         * =========================
         */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization
    ) {

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            return ResponseEntity.badRequest().body(
                    new ErrorResponse(
                            false,
                            "Token không hợp lệ hoặc không được cung cấp"
                    )
            );
        }

        String token = authorization.substring(7);

        authTokenService.logout(token);

        return ResponseEntity.ok(
                new ErrorResponse(
                        true,
                        "Đăng xuất thành công"
                )
        );
    }

    /**
     * =========================
     * VALIDATE TOKEN
     * GET /api/auth/validate
     * =========================
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization
    ) {

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            return ResponseEntity.badRequest().body(
                    new ErrorResponse(
                            false,
                            "Token không hợp lệ hoặc không được cung cấp"
                    )
            );
        }

        String token = authorization.substring(7);

        boolean valid =
                authTokenService.isTokenValid(token);

        if (!valid) {

            return ResponseEntity.status(401).body(
                    new ErrorResponse(
                            false,
                            "Token không hợp lệ hoặc đã hết hạn"
                    )
            );
        }

        return ResponseEntity.ok(
                new ErrorResponse(
                        true,
                        "Token hợp lệ"
                )
        );
    }

    /**
     * =========================
     * CURRENT USER
     * GET /api/auth/me
     * =========================
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization
    ) {

        // Không có Authorization
        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            return ResponseEntity.status(401).body(
                    new ErrorResponse(
                            false,
                            "Token không hợp lệ hoặc không được cung cấp"
                    )
            );
        }

        String token = authorization.substring(7);

        try {

            // Lấy userId từ token
            UUID userId =
                    authTokenService.getUserIdFromToken(token);

            // Lấy User từ database
            User user =
                    userService.findById(userId);

            // Trả thông tin user
            return ResponseEntity.ok(
                    new MeResponse(
                            true,
                            "Lấy thông tin người dùng thành công",
                            user.getId(),
                            user.getEmail(),
                            user.getRole() != null
                                    ? user.getRole().getId()
                                     : null,
                            user.getRole() != null
                                    ? user.getRole().getName()
                                    : null,
                            user.getFullName()
                    )
            );

        } catch (RuntimeException e) {

            return ResponseEntity.status(401).body(
                    new ErrorResponse(
                            false,
                            e.getMessage()
                    )
            );
        }
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(
                    new ErrorResponse(false, "Token không hợp lệ hoặc không được cung cấp")
            );
        }

        try {
            UUID userId = authTokenService.getUserIdFromToken(authorization.substring(7));
            User user = userService.updateProfile(
                    userId,
                    request.fullName(),
                    request.email(),
                    request.currentPassword(),
                    request.newPassword()
            );
            return ResponseEntity.ok(new MeResponse(
                    true,
                    "Cập nhật thông tin cá nhân thành công",
                    user.getId(),
                    user.getEmail(),
                    user.getRole() != null ? user.getRole().getId() : null,
                    user.getRole() != null ? user.getRole().getName() : null,
                    user.getFullName()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(false, e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(new ErrorResponse(false, e.getMessage()));
        }
    }
    public record ProfileUpdateRequest(
            String fullName,
            String email,
            String currentPassword,
            String newPassword
    ) {
    }

    public record LoginRequest(

            @NotBlank(
                    message = "Email không được để trống"
            )
            @Email(
                    message = "Email không đúng định dạng"
            )
            String email,

            @NotBlank(
                    message = "Password không được để trống"
            )
            String password
    ) {
    }

    public record RegistrationRequest(
            @NotBlank(message = "Họ và tên không được để trống")
            @Size(max = 255, message = "Họ và tên không được vượt quá 255 ký tự")
            String fullName,

            @NotBlank(message = "Email không được để trống")
            @Email(message = "Email không đúng định dạng")
            @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
            String email,

            @NotBlank(message = "Mật khẩu không được để trống")
            @Size(min = 8, max = 128, message = "Mật khẩu phải có từ 8 đến 128 ký tự")
            String password,

            @NotBlank(message = "Vui lòng xác nhận mật khẩu")
            String confirmPassword
    ) {
    }

    public record LoginResponse(
            boolean success,
            String message,
            UUID userId,
            String email,
            UUID roleId,
            String roleName,
            String fullName,
            String token,
            OffsetDateTime expiresAt
    ) {
    }

    public record MeResponse(
            boolean success,
            String message,
            UUID userId,
            String email,
            UUID roleId,
            String roleName,
            String fullName
    ) {
    }

    public record RegistrationResponse(
            boolean success,
            String message,
            String email
    ) {
    }

    public record ErrorResponse(
            boolean success,
            String message
    ) {
    }
}
