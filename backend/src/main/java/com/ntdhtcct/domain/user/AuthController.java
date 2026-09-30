package com.ntdhtcct.domain.user;

import com.ntdhtcct.domain.auth.AuthToken;
import com.ntdhtcct.domain.auth.AuthTokenService;
import com.ntdhtcct.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
                                    : null
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

    /**
     * =========================
     * REQUEST / RESPONSE
     * =========================
     */

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

    public record LoginResponse(
            boolean success,
            String message,
            UUID userId,
            String email,
            UUID roleId,
            String token,
            OffsetDateTime expiresAt
    ) {
    }

    public record MeResponse(
            boolean success,
            String message,
            UUID userId,
            String email,
            UUID roleId
    ) {
    }

    public record ErrorResponse(
            boolean success,
            String message
    ) {
    }
}
