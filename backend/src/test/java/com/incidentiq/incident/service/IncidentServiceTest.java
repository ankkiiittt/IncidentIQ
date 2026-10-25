package com.incidentiq.incident.service;

import com.incidentiq.audit.service.AuditService;
import com.incidentiq.catalog.entity.ServiceCriticality;
import com.incidentiq.catalog.entity.ServiceEntity;
import com.incidentiq.catalog.entity.ServiceEnvironment;
import com.incidentiq.catalog.service.CatalogService;
import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.incident.dto.*;
import com.incidentiq.incident.entity.*;
import com.incidentiq.incident.repository.CommentRepository;
import com.incidentiq.incident.repository.IncidentEventRepository;
import com.incidentiq.incident.repository.IncidentRepository;
import com.incidentiq.user.entity.User;
import com.incidentiq.user.entity.UserRole;
import com.incidentiq.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;
    @Mock
    private IncidentEventRepository incidentEventRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CatalogService catalogService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private jakarta.persistence.EntityManager entityManager;

    private SlaCalculator slaCalculator;
    private IncidentStateMachine incidentStateMachine;
    private IncidentService incidentService;

    private User adminUser;
    private User engineerUser;
    private User supportUser;
    private ServiceEntity activeService;

    @BeforeEach
    void setUp() {
        slaCalculator = new SlaCalculator();
        incidentStateMachine = new IncidentStateMachine();
        incidentService = new IncidentService(
                incidentRepository,
                incidentEventRepository,
                commentRepository,
                catalogService,
                userRepository,
                slaCalculator,
                incidentStateMachine,
                auditService,
                entityManager
        );

        adminUser = new User("Admin", "admin@incidentiq.io", "hash", UserRole.ADMIN);
        adminUser.setId(UUID.randomUUID());
        engineerUser = new User("Engineer", "eng@incidentiq.io", "hash", UserRole.ENGINEER);
        engineerUser.setId(UUID.randomUUID());
        supportUser = new User("Support", "sup@incidentiq.io", "hash", UserRole.SUPPORT);
        supportUser.setId(UUID.randomUUID());

        activeService = new ServiceEntity("auth-service", ServiceEnvironment.PROD, "SecOps", ServiceCriticality.CRITICAL, "Auth");
        activeService.setId(UUID.randomUUID());
    }

    @Test
    void createIncident_success_createsIncidentAndEvents() {
        CreateIncidentRequest request = new CreateIncidentRequest(
                "Auth down",
                "Cannot login",
                IncidentSeverity.CRITICAL,
                IncidentPriority.P1,
                IncidentCategory.SECURITY,
                activeService.getId(),
                List.of("auth", "prod"),
                null,
                "HTTP 500 error log snippet"
        );

        when(catalogService.findServiceEntity(activeService.getId())).thenReturn(activeService);
        when(incidentRepository.saveAndFlush(any(Incident.class))).thenAnswer(inv -> {
            Incident inc = inv.getArgument(0);
            inc.setId(UUID.randomUUID());
            return inc;
        });

        IncidentDetailResponse response = incidentService.createIncident(request, supportUser, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Auth down");
        assertThat(response.priority()).isEqualTo(IncidentPriority.P1);
        assertThat(response.slaResponseDueAt()).isNotNull();
        assertThat(response.slaResolveDueAt()).isNotNull();

        // Verifies timeline events: CREATED and LOG_ADDED
        verify(incidentEventRepository, times(2)).save(any(IncidentEvent.class));
        verify(auditService).record(eq(supportUser.getId()), eq("INCIDENT_CREATED"), eq("Incident"), anyString(), anyMap(), eq("127.0.0.1"));
    }

    @Test
    void createIncident_inactiveService_throwsConflict() {
        activeService.setActive(false);
        when(catalogService.findServiceEntity(activeService.getId())).thenReturn(activeService);

        CreateIncidentRequest request = new CreateIncidentRequest(
                "Auth down", "Cannot login", IncidentSeverity.CRITICAL,
                IncidentPriority.P1, IncidentCategory.SECURITY, activeService.getId(), null, null, null
        );

        assertThatThrownBy(() -> incidentService.createIncident(request, supportUser, "127.0.0.1"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void updateIncident_staleVersion_throwsStaleVersionConflict() {
        Incident incident = new Incident("Title", "Desc", IncidentSeverity.HIGH, IncidentPriority.P2, activeService, supportUser, null, null);
        incident.setId(UUID.randomUUID());
        incident.setVersion(2L);

        when(incidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));

        UpdateIncidentRequest request = new UpdateIncidentRequest("New title", "New desc", null, null, null, 1L);

        assertThatThrownBy(() -> incidentService.updateIncident(incident.getId(), request, supportUser, "127.0.0.1"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.STALE_VERSION));
    }

    @Test
    void transitionStatus_supportResolving_throwsForbidden() {
        Incident incident = new Incident("Title", "Desc", IncidentSeverity.HIGH, IncidentPriority.P2, activeService, supportUser, null, null);
        incident.setId(UUID.randomUUID());
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incident.setVersion(0L);

        when(incidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));

        TransitionStatusRequest request = new TransitionStatusRequest(IncidentStatus.RESOLVED, "Bug", "Fixed", 0L);

        // SUPPORT role cannot resolve or close
        assertThatThrownBy(() -> incidentService.transitionStatus(incident.getId(), request, supportUser, "127.0.0.1"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void transitionStatus_engineerResolvingWithoutRootCause_throwsValidationFailed() {
        Incident incident = new Incident("Title", "Desc", IncidentSeverity.HIGH, IncidentPriority.P2, activeService, engineerUser, null, null);
        incident.setId(UUID.randomUUID());
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incident.setVersion(0L);

        when(incidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));

        TransitionStatusRequest request = new TransitionStatusRequest(IncidentStatus.RESOLVED, "", "Fixed", 0L);

        assertThatThrownBy(() -> incidentService.transitionStatus(incident.getId(), request, engineerUser, "127.0.0.1"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    void transitionStatus_engineerResolving_succeedsAndRecordsResolvedAt() {
        Incident incident = new Incident("Title", "Desc", IncidentSeverity.HIGH, IncidentPriority.P2, activeService, engineerUser, null, null);
        incident.setId(UUID.randomUUID());
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incident.setVersion(0L);

        when(incidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));
        when(incidentRepository.saveAndFlush(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

        TransitionStatusRequest request = new TransitionStatusRequest(IncidentStatus.RESOLVED, "Disk full", "Expanded disk volume", 0L);
        IncidentDetailResponse response = incidentService.transitionStatus(incident.getId(), request, engineerUser, "127.0.0.1");

        assertThat(response.status()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(response.rootCause()).isEqualTo("Disk full");
        assertThat(response.resolution()).isEqualTo("Expanded disk volume");
        assertThat(response.resolvedAt()).isNotNull();

        verify(incidentEventRepository).save(any(IncidentEvent.class));
        verify(auditService).record(eq(engineerUser.getId()), eq("INCIDENT_STATUS_CHANGED"), eq("Incident"), anyString(), anyMap(), eq("127.0.0.1"));
    }

    @Test
    void escalateIncident_bumpsPriority_andRecordsEscalatedEvent() {
        Incident incident = new Incident("Title", "Desc", IncidentSeverity.HIGH, IncidentPriority.P3, activeService, engineerUser, null, null);
        incident.setId(UUID.randomUUID());
        incident.setVersion(0L);

        when(incidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));
        when(incidentRepository.saveAndFlush(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

        EscalateIncidentRequest request = new EscalateIncidentRequest("High customer impact", 0L);
        IncidentDetailResponse response = incidentService.escalateIncident(incident.getId(), request, supportUser, "127.0.0.1");

        assertThat(response.priority()).isEqualTo(IncidentPriority.P2);
        assertThat(response.escalationCount()).isEqualTo(1);
        assertThat(response.escalatedAt()).isNotNull();

        verify(auditService).record(eq(supportUser.getId()), eq("INCIDENT_ESCALATED"), eq("Incident"), anyString(), anyMap(), eq("127.0.0.1"));
    }

    @Test
    void addLogSnippet_oversize_throwsValidationFailed() {
        Incident incident = new Incident("Title", "Desc", IncidentSeverity.HIGH, IncidentPriority.P3, activeService, engineerUser, null, null);
        incident.setId(UUID.randomUUID());
        when(incidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));

        String hugeSnippet = "A".repeat(4097);
        AddLogSnippetRequest request = new AddLogSnippetRequest(hugeSnippet);

        assertThatThrownBy(() -> incidentService.addLogSnippet(incident.getId(), request, engineerUser))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }
}
