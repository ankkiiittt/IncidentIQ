package com.incidentiq.auth.service;

import com.incidentiq.audit.service.AuditService;
import com.incidentiq.auth.dto.ChangePasswordRequest;
import com.incidentiq.auth.dto.LoginRequest;
import com.incidentiq.auth.dto.UserProfileResponse;
import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.user.entity.User;
import com.incidentiq.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginRateLimiter loginRateLimiter;
    private final AuditService auditService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            LoginRateLimiter loginRateLimiter,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.loginRateLimiter = loginRateLimiter;
        this.auditService = auditService;
    }

    public record AuthResult(String accessToken, String rawRefreshToken, UserProfileResponse userProfile) {}

    @Transactional
    public AuthResult login(LoginRequest request, String ipAddress) {
        loginRateLimiter.checkRateLimit(ipAddress, request.email());

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> {
                    log.warn("Login failed: email {} not found", request.email());
                    return new AppException(ErrorCode.INVALID_CREDENTIALS);
                });

        if (!user.isAccountNonLocked()) {
            long remainingMinutes = Math.max(1, Duration.between(Instant.now(), user.getLockedUntil()).toMinutes());
            auditService.record(user.getId(), "LOGIN_LOCKED_ATTEMPT", "USER", user.getId().toString(),
                    Map.of("lockedUntil", user.getLockedUntil().toString()), ipAddress);
            throw new AppException(ErrorCode.ACCOUNT_LOCKED,
                    "Account is locked due to multiple failed login attempts. Retry in " + remainingMinutes + " minutes.");
        }

        if (!user.isEnabled()) {
            auditService.record(user.getId(), "LOGIN_DISABLED_ATTEMPT", "USER", user.getId().toString(), Map.of(), ipAddress);
            throw new AppException(ErrorCode.FORBIDDEN, "Account has been deactivated. Please contact an administrator.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin(MAX_FAILED_ATTEMPTS, LOCKOUT_MINUTES);
            userRepository.save(user);

            boolean nowLocked = !user.isAccountNonLocked();
            auditService.record(user.getId(), nowLocked ? "ACCOUNT_LOCKED" : "LOGIN_FAILED", "USER", user.getId().toString(),
                    Map.of("failedAttempts", user.getFailedLoginCount()), ipAddress);

            if (nowLocked) {
                throw new AppException(ErrorCode.ACCOUNT_LOCKED,
                        "Account has been locked for 15 minutes due to 5 consecutive failed login attempts.");
            }
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        user.resetFailedLogin();
        userRepository.save(user);
        loginRateLimiter.clear(ipAddress, request.email());

        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = refreshTokenService.createRefreshToken(user);

        auditService.record(user.getId(), "LOGIN_SUCCESS", "USER", user.getId().toString(), Map.of(), ipAddress);

        UserProfileResponse profile = new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isMustChangePassword()
        );

        return new AuthResult(accessToken, rawRefreshToken, profile);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Current password does not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        auditService.record(user.getId(), "PASSWORD_CHANGED", "USER", user.getId().toString(), Map.of(), ipAddress);
    }
}
