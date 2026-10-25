package com.incidentiq.user.controller;

import com.incidentiq.auth.security.UserPrincipal;
import com.incidentiq.user.dto.CreateUserRequest;
import com.incidentiq.user.dto.ResetPasswordResponse;
import com.incidentiq.user.dto.UpdateUserRequest;
import com.incidentiq.user.dto.UserResponse;
import com.incidentiq.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "User Administration", description = "Admin user management endpoints")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List users with pagination")
    public ResponseEntity<Page<UserResponse>> listUsers(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(userService.listUsers(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user details by ID")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @PostMapping
    @Operation(summary = "Create a new user")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal UserPrincipal admin,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        UserResponse response = userService.createUser(request, admin.getId(), clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update user role or enabled status")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UserPrincipal admin,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        UserResponse response = userService.updateUser(id, request, admin.getId(), clientIp);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Reset user password with temporary credentials")
    public ResponseEntity<ResetPasswordResponse> resetPassword(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal admin,
            HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        ResetPasswordResponse response = userService.resetPassword(id, admin.getId(), clientIp);
        return ResponseEntity.ok(response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
