package com.incidentiq.catalog.controller;

import com.incidentiq.auth.security.UserPrincipal;
import com.incidentiq.catalog.dto.*;
import com.incidentiq.catalog.service.CatalogService;
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
@RequestMapping("/api/services")
public class ServiceController {

    private final CatalogService catalogService;

    public ServiceController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ServiceResponse>> listServices(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(catalogService.listServices(activeOnly, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceResponse> getService(@PathVariable UUID id) {
        return ResponseEntity.ok(catalogService.getService(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceResponse> createService(
            @Valid @RequestBody CreateServiceRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        ServiceResponse response = catalogService.createService(
                request,
                principal != null ? principal.getUserId() : null,
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateServiceRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        ServiceResponse response = catalogService.updateService(
                id,
                request,
                principal != null ? principal.getUserId() : null,
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateService(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        catalogService.deactivateService(
                id,
                principal != null ? principal.getUserId() : null,
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deployments")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<DeploymentResponse> recordDeployment(
            @PathVariable UUID id,
            @Valid @RequestBody CreateDeploymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        DeploymentResponse response = catalogService.recordDeployment(
                id,
                request,
                principal != null ? principal.getUserId() : null,
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/deployments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<DeploymentResponse>> listDeployments(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(catalogService.listDeployments(id, pageable));
    }
}
