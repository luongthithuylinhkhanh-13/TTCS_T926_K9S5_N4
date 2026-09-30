package com.ntdhtcct.domain.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AuthTokenService {

    // Token có hiệu lực 24 giờ
    private static final long TOKEN_EXPIRATION_HOURS = 24;

    private final AuthTokenRepository authTokenRepository;

    public AuthTokenService(AuthTokenRepository authTokenRepository) {
        this.authTokenRepository = authTokenRepository;
    }

    @Transactional
    public AuthToken createToken(UUID userId) {

        String token = UUID.randomUUID().toString();

        OffsetDateTime expiresAt =
                OffsetDateTime.now()
                        .plusHours(TOKEN_EXPIRATION_HOURS);

        AuthToken authToken =
                new AuthToken(
                        token,
                        userId,
                        expiresAt
                );

        return authTokenRepository.save(authToken);
    }

    @Transactional
    public void logout(String token) {

        authTokenRepository.findByToken(token)
                .ifPresent(authToken -> {
                    authToken.setRevoked(true);
                    authTokenRepository.save(authToken);
                });
    }
    public boolean isTokenValid(String token) {

    return authTokenRepository
            .findByTokenAndRevokedFalse(token)
            .map(authToken ->
                    authToken.getExpiresAt().isAfter(OffsetDateTime.now())
            )
            .orElse(false);
    }
}
