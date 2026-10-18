package com.ntdhtcct.domain.health;

import com.ntdhtcct.common.exception.ForbiddenException;
import com.ntdhtcct.common.exception.UnauthorizedException;
import com.ntdhtcct.domain.auth.AuthTokenService;
import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class SystemHealthController {

    private final SystemHealthService systemHealthService;
    private final AuthTokenService authTokenService;
    private final UserRepository userRepository;

    public SystemHealthController(
            SystemHealthService systemHealthService,
            AuthTokenService authTokenService,
            UserRepository userRepository
    ) {
        this.systemHealthService = systemHealthService;
        this.authTokenService = authTokenService;
        this.userRepository = userRepository;
    }

    @GetMapping("/system-health")
    public SystemHealthResponse getSystemHealth(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("AUTH_MISSING_TOKEN", "Cần đăng nhập để xem trạng thái hệ thống.");
        }

        String token = authorization.substring(7).trim();
        if (!authTokenService.isTokenValid(token)) {
            throw new UnauthorizedException("AUTH_INVALID_TOKEN", "Phiên đăng nhập không hợp lệ hoặc đã hết hạn.");
        }

        UUID userId = authTokenService.getUserIdFromToken(token);
        User user = userId == null ? null : userRepository.findById(userId).orElse(null);
        if (user == null || user.getRole() == null || !"ADMIN".equalsIgnoreCase(user.getRole().getName())) {
            throw new ForbiddenException("ADMIN_REQUIRED", "Chỉ quản trị viên mới được xem trạng thái hệ thống.");
        }

        return systemHealthService.checkHealth();
    }
}
