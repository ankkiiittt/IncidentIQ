package com.incidentiq.catalog.service;

import com.incidentiq.audit.service.AuditService;
import com.incidentiq.catalog.dto.CreateDeploymentRequest;
import com.incidentiq.catalog.dto.CreateServiceRequest;
import com.incidentiq.catalog.dto.DeploymentResponse;
import com.incidentiq.catalog.dto.ServiceResponse;
import com.incidentiq.catalog.entity.DeploymentEntity;
import com.incidentiq.catalog.entity.ServiceCriticality;
import com.incidentiq.catalog.entity.ServiceEntity;
import com.incidentiq.catalog.entity.ServiceEnvironment;
import com.incidentiq.catalog.repository.DeploymentRepository;
import com.incidentiq.catalog.repository.ServiceRepository;
import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.incident.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private DeploymentRepository deploymentRepository;
    @Mock
    private IncidentRepository incidentRepository;
    @Mock
    private AuditService auditService;

    private CatalogService catalogService;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(serviceRepository, deploymentRepository, incidentRepository, auditService);
        actorId = UUID.randomUUID();
    }

    @Test
    void createService_duplicateNameAndEnvironment_throwsConflict() {
        CreateServiceRequest request = new CreateServiceRequest(
                "payments-api", ServiceEnvironment.PROD, "Payments", ServiceCriticality.CRITICAL, "Payment processing"
        );
        when(serviceRepository.existsByNameIgnoreCaseAndEnvironment("payments-api", ServiceEnvironment.PROD)).thenReturn(true);

        assertThatThrownBy(() -> catalogService.createService(request, actorId, "127.0.0.1"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void createService_success_savesAndAudits() {
        CreateServiceRequest request = new CreateServiceRequest(
                "payments-api", ServiceEnvironment.PROD, "Payments", ServiceCriticality.CRITICAL, "Payment processing"
        );
        when(serviceRepository.existsByNameIgnoreCaseAndEnvironment("payments-api", ServiceEnvironment.PROD)).thenReturn(false);
        when(serviceRepository.save(any(ServiceEntity.class))).thenAnswer(inv -> {
            ServiceEntity entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        ServiceResponse response = catalogService.createService(request, actorId, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("payments-api");
        assertThat(response.environment()).isEqualTo(ServiceEnvironment.PROD);
        verify(auditService).record(eq(actorId), eq("SERVICE_CREATED"), eq("Service"), anyString(), anyMap(), eq("127.0.0.1"));
    }

    @Test
    void deactivateService_withActiveIncidents_throwsConflict() {
        UUID serviceId = UUID.randomUUID();
        ServiceEntity entity = new ServiceEntity("orders", ServiceEnvironment.PROD, "Order Team", ServiceCriticality.HIGH, "Desc");
        entity.setId(serviceId);

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(entity));
        when(incidentRepository.existsByServiceIdAndStatusNotIn(eq(serviceId), any())).thenReturn(true);

        assertThatThrownBy(() -> catalogService.deactivateService(serviceId, actorId, "127.0.0.1"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void deactivateService_withoutActiveIncidents_succeeds() {
        UUID serviceId = UUID.randomUUID();
        ServiceEntity entity = new ServiceEntity("orders", ServiceEnvironment.PROD, "Order Team", ServiceCriticality.HIGH, "Desc");
        entity.setId(serviceId);

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(entity));
        when(incidentRepository.existsByServiceIdAndStatusNotIn(eq(serviceId), any())).thenReturn(false);
        when(serviceRepository.save(any(ServiceEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        catalogService.deactivateService(serviceId, actorId, "127.0.0.1");

        assertThat(entity.isActive()).isFalse();
        verify(auditService).record(eq(actorId), eq("SERVICE_DEACTIVATED"), eq("Service"), eq(serviceId.toString()), anyMap(), eq("127.0.0.1"));
    }

    @Test
    void recordDeployment_success_savesAndAudits() {
        UUID serviceId = UUID.randomUUID();
        ServiceEntity entity = new ServiceEntity("orders", ServiceEnvironment.PROD, "Order Team", ServiceCriticality.HIGH, "Desc");
        entity.setId(serviceId);

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(entity));
        when(deploymentRepository.save(any(DeploymentEntity.class))).thenAnswer(inv -> {
            DeploymentEntity d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        CreateDeploymentRequest request = new CreateDeploymentRequest("v1.2.3", Instant.now(), "deployer-bot", "Bugfixes");
        DeploymentResponse response = catalogService.recordDeployment(serviceId, request, actorId, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.version()).isEqualTo("v1.2.3");
        verify(auditService).record(eq(actorId), eq("DEPLOYMENT_RECORDED"), eq("Deployment"), anyString(), anyMap(), eq("127.0.0.1"));
    }
}
