package com.incidentiq.catalog.service;

import com.incidentiq.audit.service.AuditService;
import com.incidentiq.catalog.dto.*;
import com.incidentiq.catalog.entity.DeploymentEntity;
import com.incidentiq.catalog.entity.ServiceEntity;
import com.incidentiq.catalog.repository.DeploymentRepository;
import com.incidentiq.catalog.repository.ServiceRepository;
import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.incident.entity.IncidentStatus;
import com.incidentiq.incident.repository.IncidentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class CatalogService {

    private final ServiceRepository serviceRepository;
    private final DeploymentRepository deploymentRepository;
    private final IncidentRepository incidentRepository;
    private final AuditService auditService;

    public CatalogService(ServiceRepository serviceRepository,
                          DeploymentRepository deploymentRepository,
                          IncidentRepository incidentRepository,
                          AuditService auditService) {
        this.serviceRepository = serviceRepository;
        this.deploymentRepository = deploymentRepository;
        this.incidentRepository = incidentRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<ServiceResponse> listServices(boolean activeOnly, Pageable pageable) {
        if (activeOnly) {
            return serviceRepository.findByActiveTrue(pageable).map(ServiceResponse::fromEntity);
        }
        return serviceRepository.findAll(pageable).map(ServiceResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public ServiceResponse getService(UUID id) {
        return ServiceResponse.fromEntity(findServiceEntity(id));
    }

    public ServiceResponse createService(CreateServiceRequest request, UUID actorId, String ipAddress) {
        if (serviceRepository.existsByNameIgnoreCaseAndEnvironment(request.name(), request.environment())) {
            throw new AppException(
                    ErrorCode.CONFLICT,
                    String.format("Service with name '%s' already exists in environment %s", request.name(), request.environment())
            );
        }

        ServiceEntity service = new ServiceEntity(
                request.name(),
                request.environment(),
                request.ownerTeam(),
                request.criticality(),
                request.description()
        );
        ServiceEntity saved = serviceRepository.save(service);

        auditService.record(
                actorId,
                "SERVICE_CREATED",
                "Service",
                saved.getId().toString(),
                Map.of("name", saved.getName(), "environment", saved.getEnvironment().name()),
                ipAddress
        );

        return ServiceResponse.fromEntity(saved);
    }

    public ServiceResponse updateService(UUID id, UpdateServiceRequest request, UUID actorId, String ipAddress) {
        ServiceEntity service = findServiceEntity(id);

        if (request.name() != null && request.environment() != null) {
            if (serviceRepository.existsByNameIgnoreCaseAndEnvironmentAndIdNot(request.name(), request.environment(), id)) {
                throw new AppException(
                        ErrorCode.CONFLICT,
                        String.format("Another service with name '%s' already exists in environment %s", request.name(), request.environment())
                );
            }
        }

        if (request.name() != null) service.setName(request.name());
        if (request.environment() != null) service.setEnvironment(request.environment());
        if (request.ownerTeam() != null) service.setOwnerTeam(request.ownerTeam());
        if (request.criticality() != null) service.setCriticality(request.criticality());
        if (request.description() != null) service.setDescription(request.description());
        if (request.active() != null) {
            if (!request.active() && service.isActive()) {
                validateNoActiveIncidents(id);
            }
            service.setActive(request.active());
        }

        ServiceEntity updated = serviceRepository.save(service);

        auditService.record(
                actorId,
                "SERVICE_UPDATED",
                "Service",
                updated.getId().toString(),
                Map.of("name", updated.getName(), "active", updated.isActive()),
                ipAddress
        );

        return ServiceResponse.fromEntity(updated);
    }

    public void deactivateService(UUID id, UUID actorId, String ipAddress) {
        ServiceEntity service = findServiceEntity(id);
        validateNoActiveIncidents(id);
        service.setActive(false);
        serviceRepository.save(service);

        auditService.record(
                actorId,
                "SERVICE_DEACTIVATED",
                "Service",
                id.toString(),
                Map.of("name", service.getName()),
                ipAddress
        );
    }

    public DeploymentResponse recordDeployment(UUID serviceId, CreateDeploymentRequest request, UUID actorId, String ipAddress) {
        ServiceEntity service = findServiceEntity(serviceId);

        DeploymentEntity deployment = new DeploymentEntity(
                service,
                request.version(),
                request.deployedAt() != null ? request.deployedAt() : Instant.now(),
                request.deployedBy(),
                request.notes()
        );

        DeploymentEntity saved = deploymentRepository.save(deployment);

        auditService.record(
                actorId,
                "DEPLOYMENT_RECORDED",
                "Deployment",
                saved.getId().toString(),
                Map.of("serviceId", serviceId.toString(), "version", saved.getVersion()),
                ipAddress
        );

        return DeploymentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<DeploymentResponse> listDeployments(UUID serviceId, Pageable pageable) {
        findServiceEntity(serviceId); // ensures service exists
        return deploymentRepository.findByServiceIdOrderByDeployedAtDesc(serviceId, pageable)
                .map(DeploymentResponse::fromEntity);
    }

    public ServiceEntity findServiceEntity(UUID id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Service not found with id: " + id));
    }

    private void validateNoActiveIncidents(UUID serviceId) {
        boolean hasActive = incidentRepository.existsByServiceIdAndStatusNotIn(
                serviceId,
                List.of(IncidentStatus.RESOLVED, IncidentStatus.CLOSED)
        );
        if (hasActive) {
            throw new AppException(ErrorCode.CONFLICT, "Cannot deactivate service with active incidents");
        }
    }
}
