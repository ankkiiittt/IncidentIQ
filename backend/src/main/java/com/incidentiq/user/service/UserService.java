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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final JwtService jwtService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User not found"));
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request, UUID adminId, String ipAddress) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new AppException(ErrorCode.CONFLICT, "User with email '" + request.email() + "' already exists");
        }

        String rawPassword = (request.temporaryPassword() != null && !request.temporaryPassword().isBlank())
                ? request.temporaryPassword()
                : "TempPass@" + jwtService.generateOpaqueToken().substring(0, 10) + "!";

        User user = new User(
                request.name(),
                request.email().toLowerCase().trim(),
                passwordEncoder.encode(rawPassword),
                request.role()
        );
        user.setMustChangePassword(true);

        User saved = userRepository.save(user);

        auditService.record(
                adminId,
                "USER_CREATED",
                "USER",
                saved.getId().toString(),
                Map.of("email", saved.getEmail(), "role", saved.getRole().name()),
                ipAddress
        );

        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request, UUID adminId, String ipAddress) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User not found"));

        // Safeguard: The last active ADMIN cannot be disabled or demoted
        boolean isDemoting = request.role() != null && request.role() != UserRole.ADMIN && user.getRole() == UserRole.ADMIN;
        boolean isDisabling = Boolean.FALSE.equals(request.enabled()) && user.isEnabled() && user.getRole() == UserRole.ADMIN;

        if (isDemoting || isDisabling) {
            long activeAdminCount = userRepository.countByRoleAndEnabledTrue(UserRole.ADMIN);
            if (activeAdminCount <= 1) {
                throw new AppException(
                        ErrorCode.FORBIDDEN,
                        "Action rejected: Cannot disable or demote the last active administrator."
                );
            }
        }

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }

        User updated = userRepository.save(user);

        auditService.record(
                adminId,
                "USER_UPDATED",
                "USER",
                updated.getId().toString(),
                Map.of(
                        "name", updated.getName(),
                        "role", updated.getRole().name(),
                        "enabled", updated.isEnabled()
                ),
                ipAddress
        );

        return UserResponse.fromEntity(updated);
    }

    @Transactional
    public ResetPasswordResponse resetPassword(UUID id, UUID adminId, String ipAddress) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User not found"));

        String temporaryPassword = "TempReset@" + jwtService.generateOpaqueToken().substring(0, 10) + "!";
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        user.resetFailedLogin();
        userRepository.save(user);

        auditService.record(
                adminId,
                "PASSWORD_RESET",
                "USER",
                user.getId().toString(),
                Map.of("resetBy", adminId.toString()),
                ipAddress
        );

        return new ResetPasswordResponse(
                temporaryPassword,
                "Password reset successfully. User must change password upon next login."
        );
    }
}
