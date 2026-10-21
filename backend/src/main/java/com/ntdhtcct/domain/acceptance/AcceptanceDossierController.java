package com.ntdhtcct.domain.acceptance;

import com.ntdhtcct.common.exception.UnauthorizedException;
import com.ntdhtcct.domain.acceptance.dto.AcceptanceDossierResponse;
import com.ntdhtcct.domain.auth.AuthTokenService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/acceptance-reports")
public class AcceptanceDossierController {

    private final AcceptanceDossierService dossierService;
    private final AuthTokenService authTokenService;

    public AcceptanceDossierController(
            AcceptanceDossierService dossierService,
            AuthTokenService authTokenService
    ) {
        this.dossierService = dossierService;
        this.authTokenService = authTokenService;
    }

    @GetMapping("/{reportId}")
    public AcceptanceDossierResponse getDossier(
            @PathVariable UUID reportId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException(
                    "AUTH_MISSING_TOKEN",
                    "Cần đăng nhập để tải hồ sơ nghiệm thu"
            );
        }

        String token = authorization.substring(7).trim();
        if (!authTokenService.isTokenValid(token)) {
            throw new UnauthorizedException(
                    "AUTH_INVALID_TOKEN",
                    "Phiên đăng nhập không hợp lệ hoặc đã hết hạn"
            );
        }

        UUID userId = authTokenService.getUserIdFromToken(token);
        return dossierService.getDossier(reportId, userId);
    }
}
