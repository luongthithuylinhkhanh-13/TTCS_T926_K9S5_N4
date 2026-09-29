package com.ntdhtcct.domain.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

    @Mock
    private AuthTokenRepository authTokenRepository;

    private AuthTokenService authTokenService;

    @BeforeEach
    void setUp() {
        authTokenService = new AuthTokenService(
                authTokenRepository,
                new ObjectMapper(),
                "test-secret-with-at-least-32-bytes-long",
                168L
        );
    }

    @Test
    void createsSignedJwtAndRevokesItOnLogout() {
        UUID userId = UUID.randomUUID();
        when(authTokenRepository.save(any(AuthToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthToken authToken = authTokenService.createToken(userId);
        when(authTokenRepository.findByTokenAndRevokedFalse(authToken.getToken()))
                .thenReturn(Optional.of(authToken));
        when(authTokenRepository.findByToken(authToken.getToken()))
                .thenReturn(Optional.of(authToken));

        assertThat(authToken.getToken().split("\\.")).hasSize(3);
        assertThat(authTokenService.isTokenValid(authToken.getToken())).isTrue();
        assertThat(authTokenService.isTokenValid(authToken.getToken() + "x")).isFalse();

        authTokenService.logout(authToken.getToken());

        assertThat(authToken.isRevoked()).isTrue();
    }

    @Test
    void createsTokenThatStaysValidLongerThanOneDay() {
        UUID userId = UUID.randomUUID();
        when(authTokenRepository.save(any(AuthToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthToken authToken = authTokenService.createToken(userId);

        assertThat(authToken.getExpiresAt())
                .isAfter(OffsetDateTime.now().plusDays(6));
    }
}