package com.incidentiq.ai.controller;

import com.incidentiq.ai.dto.AppSettingResponse;
import com.incidentiq.ai.dto.UpdateAppSettingRequest;
import com.incidentiq.ai.service.SettingsService;
import com.incidentiq.auth.security.UserPrincipal;
import com.incidentiq.user.entity.User;
import com.incidentiq.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettingsController {

    private final SettingsService settingsService;
    private final UserRepository userRepository;

    public AdminSettingsController(SettingsService settingsService, UserRepository userRepository) {
        this.settingsService = settingsService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<AppSettingResponse>> getAllSettings() {
        return ResponseEntity.ok(settingsService.getAllSettings());
    }

    @GetMapping("/{key}")
    public ResponseEntity<AppSettingResponse> getSetting(@PathVariable String key) {
        return ResponseEntity.ok(settingsService.getSetting(key));
    }

    @PutMapping("/{key}")
    public ResponseEntity<AppSettingResponse> updateSetting(
            @PathVariable String key,
            @Valid @RequestBody UpdateAppSettingRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = resolveUser(principal);
        return ResponseEntity.ok(settingsService.updateSetting(key, request.value(), request.description(), user));
    }

    private User resolveUser(UserPrincipal principal) {
        if (principal == null || principal.getUserId() == null) {
            return null;
        }
        return userRepository.findById(principal.getUserId()).orElse(null);
    }
}
