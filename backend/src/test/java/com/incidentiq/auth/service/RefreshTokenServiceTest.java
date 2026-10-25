package com.incidentiq.auth.service;

import com.incidentiq.audit.service.AuditService;
import com.incidentiq.auth.entity.RefreshToken;
import com.incidentiq.auth.repository.RefreshTokenRepository;
import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.user.entity.User;
import com.incidentiq.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AuditService auditService;

    private JwtService jwtService;
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("very-long-test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256", 15);
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtService, auditService, 7);
    }

    @Test
    @DisplayName("Should successfully rotate valid refresh token")
    void shouldRotateValidRefreshToken() {
        User user = new User("Alice", "alice@example.com", "hash", UserRole.ENGINEER);
        user.setId(UUID.randomUUID());

        UUID familyId = UUID.randomUUID();
        String rawToken = "valid-raw-refresh-token";
        String tokenHash = jwtService.hashToken(rawToken);

        RefreshToken existing = new RefreshToken(user, familyId, tokenHash, Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByTokenHashWithUser(tokenHash)).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshTokenService.RotationResult result = refreshTokenService.rotateRefreshToken(rawToken, "127.0.0.1");

        assertNotNull(result);
        assertNotNull(result.rawRefreshToken());
        assertNotNull(result.accessToken());
        assertEquals(user, result.user());
        assertTrue(existing.isRevoked());
    }

    @Test
    @DisplayName("Reuse of a rotated refresh token revokes whole token family")
    void shouldRevokeFamilyOnTokenReuse() {
        User user = new User("Alice", "alice@example.com", "hash", UserRole.ENGINEER);
        user.setId(UUID.randomUUID());

        UUID familyId = UUID.randomUUID();
        String rawToken = "already-used-token";
        String tokenHash = jwtService.hashToken(rawToken);

        RefreshToken reusedToken = new RefreshToken(user, familyId, tokenHash, Instant.now().plusSeconds(3600));
        reusedToken.revoke(); // already revoked!

        when(refreshTokenRepository.findByTokenHashWithUser(tokenHash)).thenReturn(Optional.of(reusedToken));

        AppException ex = assertThrows(AppException.class, () ->
                refreshTokenService.rotateRefreshToken(rawToken, "127.0.0.1")
        );

        assertEquals(ErrorCode.TOKEN_REVOKED, ex.getErrorCode());
        // Verify family revocation was invoked
        verify(refreshTokenRepository, times(1)).revokeFamily(eq(familyId), any(Instant.class));
        verify(auditService, times(1)).record(eq(user.getId()), eq("TOKEN_FAMILY_REVOKED_REUSE"), anyString(), anyString(), anyMap(), anyString());
    }

    @Test
    @DisplayName("Should reject expired refresh token")
    void shouldRejectExpiredRefreshToken() {
        User user = new User("Alice", "alice@example.com", "hash", UserRole.ENGINEER);
        user.setId(UUID.randomUUID());

        UUID familyId = UUID.randomUUID();
        String rawToken = "expired-token";
        String tokenHash = jwtService.hashToken(rawToken);

        RefreshToken expiredToken = new RefreshToken(user, familyId, tokenHash, Instant.now().minusSeconds(100));
        when(refreshTokenRepository.findByTokenHashWithUser(tokenHash)).thenReturn(Optional.of(expiredToken));

        AppException ex = assertThrows(AppException.class, () ->
                refreshTokenService.rotateRefreshToken(rawToken, "127.0.0.1")
        );

        assertEquals(ErrorCode.TOKEN_EXPIRED, ex.getErrorCode());
    }
}
