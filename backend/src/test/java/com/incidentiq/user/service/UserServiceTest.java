package com.incidentiq.user.service;

import com.incidentiq.audit.service.AuditService;
import com.incidentiq.auth.service.JwtService;
import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.user.dto.CreateUserRequest;
import com.incidentiq.user.dto.ResetPasswordResponse;
import com.incidentiq.user.dto.UpdateUserRequest;
import com.incidentiq.user.dto.UserResponse;
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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(12);
        jwtService = new JwtService("very-long-test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256", 15);
        userService = new UserService(userRepository, passwordEncoder, auditService, jwtService);
    }

    @Test
    @DisplayName("Should create user with temporary password and must_change_password true")
    void shouldCreateUserWithTemporaryPassword() {
        UUID adminId = UUID.randomUUID();
        CreateUserRequest request = new CreateUserRequest("Bob", "bob@example.com", UserRole.SUPPORT, null);

        when(userRepository.existsByEmailIgnoreCase("bob@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserResponse response = userService.createUser(request, adminId, "127.0.0.1");

        assertNotNull(response);
        assertEquals("Bob", response.name());
        assertEquals("bob@example.com", response.email());
        assertEquals(UserRole.SUPPORT, response.role());
        assertTrue(response.mustChangePassword());
        verify(auditService, times(1)).record(eq(adminId), eq("USER_CREATED"), anyString(), anyString(), anyMap(), anyString());
    }

    @Test
    @DisplayName("Should reject duplicate user email with 409 CONFLICT")
    void shouldRejectDuplicateEmail() {
        UUID adminId = UUID.randomUUID();
        CreateUserRequest request = new CreateUserRequest("Bob", "bob@example.com", UserRole.SUPPORT, null);
        when(userRepository.existsByEmailIgnoreCase("bob@example.com")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> userService.createUser(request, adminId, "127.0.0.1"));
        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
    }

    @Test
    @DisplayName("Last active ADMIN cannot be disabled or demoted")
    void shouldProtectLastActiveAdmin() {
        UUID adminId = UUID.randomUUID();
        User adminUser = new User("Admin", "admin@example.com", "hash", UserRole.ADMIN);
        adminUser.setId(adminId);
        adminUser.setEnabled(true);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(userRepository.countByRoleAndEnabledTrue(UserRole.ADMIN)).thenReturn(1L);

        // Attempt to demote to ENGINEER
        UpdateUserRequest demoteReq = new UpdateUserRequest(null, UserRole.ENGINEER, null);
        AppException exDemote = assertThrows(AppException.class, () ->
                userService.updateUser(adminId, demoteReq, adminId, "127.0.0.1")
        );
        assertEquals(ErrorCode.FORBIDDEN, exDemote.getErrorCode());

        // Attempt to disable
        UpdateUserRequest disableReq = new UpdateUserRequest(null, null, false);
        AppException exDisable = assertThrows(AppException.class, () ->
                userService.updateUser(adminId, disableReq, adminId, "127.0.0.1")
        );
        assertEquals(ErrorCode.FORBIDDEN, exDisable.getErrorCode());
    }

    @Test
    @DisplayName("Admin password reset issues temporary password and forces change")
    void shouldResetPasswordSuccessfully() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User user = new User("Dave", "dave@example.com", "oldHash", UserRole.ENGINEER);
        user.setId(targetId);

        when(userRepository.findById(targetId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        ResetPasswordResponse response = userService.resetPassword(targetId, adminId, "127.0.0.1");

        assertNotNull(response);
        assertNotNull(response.temporaryPassword());
        assertTrue(response.temporaryPassword().startsWith("TempReset@"));
        assertTrue(user.isMustChangePassword());
        verify(auditService, times(1)).record(eq(adminId), eq("PASSWORD_RESET"), anyString(), anyString(), anyMap(), anyString());
    }
}
