package com.ntdhtcct.domain.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthTokenService {

    private static final String JWT_ALGORITHM = "HmacSHA256";

    private final AuthTokenRepository authTokenRepository;
    private final ObjectMapper objectMapper;
    private final byte[] jwtSecret;
    private final long tokenExpirationHours;

    public AuthTokenService(
            AuthTokenRepository authTokenRepository,
            ObjectMapper objectMapper,
            @Value("${app.auth.jwt-secret}") String jwtSecret,
            @Value("${app.auth.token-expiration-hours:168}") long tokenExpirationHours
    ) {
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET phải dài tối thiểu 32 byte");
        }
        if (tokenExpirationHours <= 0) {
            throw new IllegalArgumentException("app.auth.token-expiration-hours phải lớn hơn 0");
        }
        this.authTokenRepository = authTokenRepository;
        this.objectMapper = objectMapper;
        this.jwtSecret = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.tokenExpirationHours = tokenExpirationHours;
    }

    @Transactional
    public AuthToken createToken(UUID userId) {

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusHours(tokenExpirationHours);
        String token = createJwt(userId, now.toEpochSecond(), expiresAt.toEpochSecond());

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
        JwtClaims claims = parseJwt(token);
        if (claims == null) return;

                authTokenRepository.findByToken(token)
                .filter(authToken -> authToken.getUserId().equals(claims.userId()))
                .ifPresent(authToken -> {
                    authToken.setRevoked(true);
                    authTokenRepository.save(authToken);
                });
    }

    public boolean isTokenValid(String token) {
        JwtClaims claims = parseJwt(token);
        if (claims == null || !claims.expiresAt().isAfter(Instant.now())) return false;

                return authTokenRepository.findByTokenAndRevokedFalse(token)
                .map(authToken -> authToken.getUserId().equals(claims.userId())
                        && authToken.getExpiresAt().toEpochSecond() == claims.expiresAt().getEpochSecond()
                        && authToken.getExpiresAt().isAfter(OffsetDateTime.now()))
                .orElse(false);
    }

    private String createJwt(UUID userId, long issuedAt, long expiresAt) {
        try {
            String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
            String payload = encodeJson(Map.of(
                    "sub", userId.toString(),
                    "jti", UUID.randomUUID().toString(),
                    "iat", issuedAt,
                    "exp", expiresAt
            ));
            String unsignedToken = header + "." + payload;
            return unsignedToken + "." + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(sign(unsignedToken));
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo JWT", e);
        }
    }

    private String encodeJson(Object value) throws Exception {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private JwtClaims parseJwt(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;

            JsonNode header = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[0]));
            if (!"HS256".equals(header.path("alg").asText())) return null;

            String unsignedToken = parts[0] + "." + parts[1];
            byte[] expectedSignature = sign(unsignedToken);
            byte[] providedSignature = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(expectedSignature, providedSignature)) return null;

            JsonNode payload = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            UUID userId = UUID.fromString(payload.path("sub").asText());
            long expiresAt = payload.path("exp").asLong(0);
            if (expiresAt <= 0) return null;
            return new JwtClaims(userId, Instant.ofEpochSecond(expiresAt));
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] sign(String input) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(JWT_ALGORITHM);
        mac.init(new SecretKeySpec(jwtSecret, JWT_ALGORITHM));
        return mac.doFinal(input.getBytes(StandardCharsets.US_ASCII));
    }

    private record JwtClaims(UUID userId, Instant expiresAt) {
    }
}
