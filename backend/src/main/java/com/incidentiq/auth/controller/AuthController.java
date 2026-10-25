package com.incidentiq.auth.controller;

import com.incidentiq.auth.dto.*;
import com.incidentiq.auth.security.UserPrincipal;
import com.incidentiq.auth.service.AuthService;
import com.incidentiq.auth.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and session management APIs")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user with email and password")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        AuthService.AuthResult result = authService.login(request, clientIp);

        ResponseCookie cookie = buildRefreshCookie(result.rawRefreshToken(), 7 * 24 * 60 * 60);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new LoginResponse(result.accessToken(), result.userProfile()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue new access token")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String rawRefreshToken,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        RefreshTokenService.RotationResult result = refreshTokenService.rotateRefreshToken(rawRefreshToken, clientIp);

        ResponseCookie cookie = buildRefreshCookie(result.rawRefreshToken(), 7 * 24 * 60 * 60);
        UserProfileResponse profile = new UserProfileResponse(
                result.user().getId(),
                result.user().getName(),
                result.user().getEmail(),
                result.user().getRole(),
                result.user().isMustChangePassword()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new LoginResponse(result.accessToken(), profile));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke session and clear refresh token")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String rawRefreshToken) {
        refreshTokenService.revokeToken(rawRefreshToken);
        ResponseCookie cookie = buildRefreshCookie("", 0);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change own password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        authService.changePassword(principal.getId(), request, clientIp);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal) {
        UserProfileResponse profile = new UserProfileResponse(
                principal.getId(),
                principal.getName(),
                principal.getEmail(),
                principal.getRole(),
                principal.isMustChangePassword()
        );
        return ResponseEntity.ok(profile);
    }

    private ResponseCookie buildRefreshCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(false) // Local dev false; set true behind SSL proxy in prod
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(maxAgeSeconds)
                .build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
