package com.incidentiq.auth.service;

import com.incidentiq.user.entity.User;
import com.incidentiq.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("very-long-test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256", 15);
    }

    @Test
    @DisplayName("Should generate valid access token with user claims")
    void shouldGenerateValidAccessToken() {
        User user = new User("Jane Doe", "jane@example.com", "hash", UserRole.ENGINEER);
        user.setId(UUID.randomUUID());

        String token = jwtService.generateAccessToken(user);

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));

        Claims claims = jwtService.parseAndValidateToken(token);
        assertEquals(user.getId().toString(), claims.getSubject());
        assertEquals("ENGINEER", claims.get("role"));
        assertEquals("Jane Doe", claims.get("name"));
        assertEquals("jane@example.com", claims.get("email"));
        assertFalse(claims.get("mustChangePassword", Boolean.class));
    }

    @Test
    @DisplayName("Should detect expired token correctly")
    void shouldDetectExpiredToken() {
        JwtService shortLivedJwtService = new JwtService(
                "very-long-test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256",
                -1 // Expired 1 minute ago
        );

        User user = new User("Jane Doe", "jane@example.com", "hash", UserRole.ENGINEER);
        user.setId(UUID.randomUUID());

        String token = shortLivedJwtService.generateAccessToken(user);
        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    @DisplayName("Should compute consistent SHA-256 token hash")
    void shouldHashTokenConsistently() {
        String rawToken = "my-secret-raw-token-12345";
        String hash1 = jwtService.hashToken(rawToken);
        String hash2 = jwtService.hashToken(rawToken);

        assertNotNull(hash1);
        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2);
    }
}
