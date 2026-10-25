package com.incidentiq.auth.service;

import com.incidentiq.audit.service.AuditService;
import com.incidentiq.auth.dto.ChangePasswordRequest;
import com.incidentiq.auth.dto.LoginRequest;
import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.user.entity.User;
import com.incidentiq.user.entity.UserRole;
import com.incidentiq.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuditService auditService;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private LoginRateLimiter loginRateLimiter;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(12);
        jwtService = new JwtService("very-long-test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256", 15);
        loginRateLimiter = new LoginRateLimiter();
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtService,
                refreshTokenService,
                loginRateLimiter,
                auditService
        );
    }

    @Test
    @DisplayName("Should successfully authenticate valid user and generate tokens")
    void shouldLoginSuccessfully() {
        String rawPassword = "ValidPassword123!";
        User user = new User("John Doe", "john@example.com", passwordEncoder.encode(rawPassword), UserRole.ENGINEER);
        user.setId(UUID.randomUUID());

        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(user));
        when(refreshTokenService.createRefreshToken(user)).thenReturn("raw-refresh-token-xyz");

        LoginRequest request = new LoginRequest("john@example.com", rawPassword);
        AuthService.AuthResult result = authService.login(request, "127.0.0.1");

        assertNotNull(result);
        assertNotNull(result.accessToken());
        assertEquals("raw-refresh-token-xyz", result.rawRefreshToken());
        assertEquals("john@example.com", result.userProfile().email());
        assertEquals(UserRole.ENGINEER, result.userProfile().role());
        assertEquals(0, user.getFailedLoginCount());
    }

    @Test
    @DisplayName("Should lock account after 5 failed login attempts for 15 minutes")
    void shouldLockAccountAfter5FailedAttempts() {
        String correctPassword = "CorrectPassword123!";
        User user = new User("John Doe", "john@example.com", passwordEncoder.encode(correctPassword), UserRole.ENGINEER);
        user.setId(UUID.randomUUID());

        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(user));

        LoginRequest wrongRequest = new LoginRequest("john@example.com", "WrongPassword!");

        // 4 failed attempts
        for (int i = 1; i <= 4; i++) {
            assertThrows(AppException.class, () -> authService.login(wrongRequest, "127.0.0.1"));
            assertEquals(i, user.getFailedLoginCount());
            assertTrue(user.isAccountNonLocked());
        }

        // 5th failed attempt should lock account
        AppException ex = assertThrows(AppException.class, () -> authService.login(wrongRequest, "127.0.0.1"));
        assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        assertEquals(5, user.getFailedLoginCount());
        assertFalse(user.isAccountNonLocked());
        assertNotNull(user.getLockedUntil());
        assertTrue(user.getLockedUntil().isAfter(Instant.now().plusSeconds(14 * 60)));
    }

    @Test
    @DisplayName("Should reject login when account is locked even with correct password")
    void shouldRejectWhenLockedEvenWithCorrectPassword() {
        String correctPassword = "CorrectPassword123!";
        User user = new User("John Doe", "john@example.com", passwordEncoder.encode(correctPassword), UserRole.ENGINEER);
        user.setId(UUID.randomUUID());
        user.setLockedUntil(Instant.now().plusSeconds(600)); // locked for 10 more minutes

        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest("john@example.com", correctPassword);
        AppException ex = assertThrows(AppException.class, () -> authService.login(request, "127.0.0.1"));
        assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Should successfully change password and clear must_change_password")
    void shouldChangePasswordSuccessfully() {
        String oldPassword = "OldPassword123!";
        String newPassword = "BrandNewSecurePassword123!";
        User user = new User("John Doe", "john@example.com", passwordEncoder.encode(oldPassword), UserRole.ENGINEER);
        user.setId(UUID.randomUUID());
        user.setMustChangePassword(true);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        ChangePasswordRequest request = new ChangePasswordRequest(oldPassword, newPassword);
        authService.changePassword(user.getId(), request, "127.0.0.1");

        assertFalse(user.isMustChangePassword());
        assertTrue(passwordEncoder.matches(newPassword, user.getPasswordHash()));
        verify(userRepository, times(1)).save(user);
    }
}
