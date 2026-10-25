package com.incidentiq.auth.service;

import com.incidentiq.audit.service.AuditService;
import com.incidentiq.auth.entity.RefreshToken;
import com.incidentiq.auth.repository.RefreshTokenRepository;
import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final int refreshTokenDays;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            AuditService auditService,
            @Value("${REFRESH_TOKEN_EXPIRATION_DAYS:7}") int refreshTokenDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.refreshTokenDays = refreshTokenDays;
    }

    public record RotationResult(String rawRefreshToken, String accessToken, User user) {}

    @Transactional
    public String createRefreshToken(User user) {
        UUID familyId = UUID.randomUUID();
        String rawToken = jwtService.generateOpaqueToken();
        String tokenHash = jwtService.hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(refreshTokenDays, ChronoUnit.DAYS);

        RefreshToken refreshToken = new RefreshToken(user, familyId, tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Transactional
    public RotationResult rotateRefreshToken(String rawToken, String ipAddress) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED, "Refresh token is missing");
        }

        String tokenHash = jwtService.hashToken(rawToken);
        RefreshToken existingToken = refreshTokenRepository.findByTokenHashWithUser(tokenHash)
                .orElseThrow(() -> new AppException(ErrorCode.TOKEN_REVOKED, "Invalid refresh token"));

        // Check for token reuse! If already revoked or replaced, revoke entire family
        if (existingToken.isRevoked() || existingToken.getReplacedBy() != null) {
            log.warn("Refresh token reuse detected for familyId={} and userId={}. Revoking entire token family.",
                    existingToken.getFamilyId(), existingToken.getUser().getId());
            refreshTokenRepository.revokeFamily(existingToken.getFamilyId(), Instant.now());
            auditService.record(
                    existingToken.getUser().getId(),
                    "TOKEN_FAMILY_REVOKED_REUSE",
                    "REFRESH_TOKEN",
                    existingToken.getFamilyId().toString(),
                    Map.of("reason", "Token reuse detected"),
                    ipAddress
            );
            throw new AppException(ErrorCode.TOKEN_REVOKED, "Refresh token was already used. Session terminated.");
        }

        if (existingToken.isExpired()) {
            existingToken.revoke();
            refreshTokenRepository.save(existingToken);
            throw new AppException(ErrorCode.TOKEN_EXPIRED, "Refresh token expired. Please log in again.");
        }

        User user = existingToken.getUser();
        if (!user.isEnabled()) {
            throw new AppException(ErrorCode.FORBIDDEN, "User account is disabled");
        }

        // Issue new token in same family
        String newRawToken = jwtService.generateOpaqueToken();
        String newTokenHash = jwtService.hashToken(newRawToken);
        Instant expiresAt = Instant.now().plus(refreshTokenDays, ChronoUnit.DAYS);

        RefreshToken newToken = new RefreshToken(user, existingToken.getFamilyId(), newTokenHash, expiresAt);
        RefreshToken savedNewToken = refreshTokenRepository.save(newToken);

        // Mark existing token replaced & revoked
        existingToken.setReplacedBy(savedNewToken.getId());
        existingToken.revoke();
        refreshTokenRepository.save(existingToken);

        String newAccessToken = jwtService.generateAccessToken(user);
        return new RotationResult(newRawToken, newAccessToken, user);
    }

    @Transactional
    public void revokeToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String tokenHash = jwtService.hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.revoke();
            refreshTokenRepository.save(token);
        });
    }
}
