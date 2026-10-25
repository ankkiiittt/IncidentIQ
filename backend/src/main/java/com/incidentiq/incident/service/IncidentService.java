package com.incidentiq.incident.service;

import com.incidentiq.audit.service.AuditService;
import com.incidentiq.catalog.entity.ServiceEntity;
import com.incidentiq.catalog.service.CatalogService;
import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.incident.dto.*;
import com.incidentiq.incident.entity.*;
import com.incidentiq.incident.repository.CommentRepository;
import com.incidentiq.incident.repository.IncidentEventRepository;
import com.incidentiq.incident.repository.IncidentRepository;
import com.incidentiq.incident.repository.IncidentSpecifications;
import com.incidentiq.messaging.dto.*;
import com.incidentiq.user.entity.User;
import com.incidentiq.user.entity.UserRole;
import com.incidentiq.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final CommentRepository commentRepository;
    private final CatalogService catalogService;
    private final UserRepository userRepository;
    private final SlaCalculator slaCalculator;
    private final IncidentStateMachine incidentStateMachine;
    private final AuditService auditService;
    private final jakarta.persistence.EntityManager entityManager;
    private final com.incidentiq.messaging.service.OutboxService outboxService;

    @org.springframework.beans.factory.annotation.Autowired
    public IncidentService(IncidentRepository incidentRepository,
                           IncidentEventRepository incidentEventRepository,
                           CommentRepository commentRepository,
                           CatalogService catalogService,
                           UserRepository userRepository,
                           SlaCalculator slaCalculator,
                           IncidentStateMachine incidentStateMachine,
                           AuditService auditService,
                           jakarta.persistence.EntityManager entityManager,
                           @org.springframework.beans.factory.annotation.Autowired(required = false) com.incidentiq.messaging.service.OutboxService outboxService) {
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.commentRepository = commentRepository;
        this.catalogService = catalogService;
        this.userRepository = userRepository;
        this.slaCalculator = slaCalculator;
        this.incidentStateMachine = incidentStateMachine;
        this.auditService = auditService;
        this.entityManager = entityManager;
        this.outboxService = outboxService;
    }

    public IncidentService(IncidentRepository incidentRepository,
                           IncidentEventRepository incidentEventRepository,
                           CommentRepository commentRepository,
                           CatalogService catalogService,
                           UserRepository userRepository,
                           SlaCalculator slaCalculator,
                           IncidentStateMachine incidentStateMachine,
                           AuditService auditService,
                           jakarta.persistence.EntityManager entityManager) {
        this(incidentRepository, incidentEventRepository, commentRepository, catalogService, userRepository,
                slaCalculator, incidentStateMachine, auditService, entityManager, null);
    }

    public IncidentDetailResponse createIncident(CreateIncidentRequest request, User reporter, String ipAddress) {
        ServiceEntity service = catalogService.findServiceEntity(request.serviceId());
        if (!service.isActive()) {
            throw new AppException(ErrorCode.CONFLICT, "Cannot create incident for inactive service: " + service.getName());
        }

        IncidentPriority priority = request.priority() != null ? request.priority() : IncidentPriority.P3;
        Instant baseTime = request.startedAt() != null ? request.startedAt() : Instant.now();

        Incident incident = new Incident(
                request.title(),
                request.description(),
                request.severity(),
                priority,
                service,
                reporter,
                request.tags(),
                request.startedAt()
        );
        incident.setCategory(request.category());
        incident.setSlaResponseDueAt(slaCalculator.calculateResponseDue(priority, baseTime));
        incident.setSlaResolveDueAt(slaCalculator.calculateResolveDue(priority, baseTime));
        incident.setSlaBreached(slaCalculator.isSlaBreached(incident, Instant.now()));

        Incident saved = incidentRepository.saveAndFlush(incident);
        if (entityManager != null) {
            entityManager.refresh(saved);
        }

        // Timeline event: CREATED
        Map<String, Object> createdPayload = new HashMap<>();
        createdPayload.put("title", saved.getTitle());
        createdPayload.put("priority", saved.getPriority().name());
        createdPayload.put("severity", saved.getSeverity().name());
        incidentEventRepository.save(new IncidentEvent(saved, IncidentEventType.CREATED, reporter, createdPayload));

        // If logSnippet is provided
        if (request.logSnippet() != null && !request.logSnippet().trim().isEmpty()) {
            validateSnippetSize(request.logSnippet());
            incidentEventRepository.save(new IncidentEvent(
                    saved,
                    IncidentEventType.LOG_ADDED,
                    reporter,
                    Map.of("snippet", request.logSnippet())
            ));
        }

        auditService.record(
                reporter.getId(),
                "INCIDENT_CREATED",
                "Incident",
                saved.getId().toString(),
                Map.of("number", saved.getFormattedNumber(), "priority", saved.getPriority().name()),
                ipAddress
        );

        publishOutboxEvent(
                "INCIDENT",
                saved.getId(),
                KafkaTopics.INCIDENT_CREATED,
                new IncidentCreatedPayload(
                        saved.getId(),
                        saved.getService().getId(),
                        saved.getPriority().name(),
                        saved.getSeverity().name(),
                        reporter.getId()
                )
        );

        return IncidentDetailResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public IncidentDetailResponse getIncident(UUID id) {
        Incident incident = findIncidentEntity(id);
        return IncidentDetailResponse.fromEntity(incident);
    }

    @Transactional(readOnly = true)
    public Page<IncidentSummaryResponse> searchIncidents(IncidentSearchCriteria criteria, Pageable pageable) {
        int pageSize = Math.min(pageable.getPageSize(), 100);
        Pageable cappedPageable = PageRequest.of(pageable.getPageNumber(), pageSize, pageable.getSort());
        return incidentRepository.findAll(IncidentSpecifications.withCriteria(criteria), cappedPageable)
                .map(IncidentSummaryResponse::fromEntity);
    }

    public IncidentDetailResponse updateIncident(UUID id, UpdateIncidentRequest request, User actor, String ipAddress) {
        Incident incident = findIncidentEntity(id);
        validateVersion(incident, request.version());

        if (request.title() != null) incident.setTitle(request.title());
        if (request.description() != null) incident.setDescription(request.description());
        if (request.severity() != null) incident.setSeverity(request.severity());
        if (request.category() != null) incident.setCategory(request.category());
        if (request.tags() != null) incident.setTags(request.tags());

        Incident updated = incidentRepository.saveAndFlush(incident);

        auditService.record(
                actor.getId(),
                "INCIDENT_UPDATED",
                "Incident",
                id.toString(),
                Map.of("title", updated.getTitle()),
                ipAddress
        );

        return IncidentDetailResponse.fromEntity(updated);
    }

    public IncidentDetailResponse assignIncident(UUID id, AssignIncidentRequest request, User actor, String ipAddress) {
        Incident incident = findIncidentEntity(id);
        validateVersion(incident, request.version());

        User assignee = userRepository.findById(request.assigneeId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Assignee user not found"));

        if (!assignee.isActive()) {
            throw new AppException(ErrorCode.CONFLICT, "Cannot assign incident to inactive user");
        }

        incident.setAssignee(assignee);
        Incident saved = incidentRepository.saveAndFlush(incident);

        incidentEventRepository.save(new IncidentEvent(
                saved,
                IncidentEventType.ASSIGNED,
                actor,
                Map.of("assigneeId", assignee.getId().toString(), "assigneeName", assignee.getFullName())
        ));

        auditService.record(
                actor.getId(),
                "INCIDENT_ASSIGNED",
                "Incident",
                id.toString(),
                Map.of("assigneeId", assignee.getId().toString()),
                ipAddress
        );

        publishOutboxEvent(
                "INCIDENT",
                saved.getId(),
                KafkaTopics.INCIDENT_UPDATED,
                new IncidentUpdatedPayload(
                        saved.getId(),
                        saved.getStatus().name(),
                        saved.getPriority().name(),
                        actor.getId(),
                        "ASSIGNED"
                )
        );

        return IncidentDetailResponse.fromEntity(saved);
    }

    public IncidentDetailResponse escalateIncident(UUID id, EscalateIncidentRequest request, User actor, String ipAddress) {
        Incident incident = findIncidentEntity(id);
        validateVersion(incident, request.version());

        IncidentPriority oldPriority = incident.getPriority();
        IncidentPriority newPriority = oldPriority.escalate();

        incident.setPriority(newPriority);
        incident.setEscalatedAt(Instant.now());
        incident.setEscalationCount(incident.getEscalationCount() + 1);

        // Recalculate SLA targets for tighter SLA
        if (incident.getAcknowledgedAt() == null) {
            incident.setSlaResponseDueAt(slaCalculator.calculateResponseDue(newPriority, incident.getCreatedAt()));
        }
        if (incident.getResolvedAt() == null) {
            incident.setSlaResolveDueAt(slaCalculator.calculateResolveDue(newPriority, incident.getCreatedAt()));
        }
        incident.setSlaBreached(slaCalculator.isSlaBreached(incident, Instant.now()));

        Incident saved = incidentRepository.saveAndFlush(incident);

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("from", oldPriority.name());
        eventPayload.put("to", newPriority.name());
        if (request.reason() != null && !request.reason().trim().isEmpty()) {
            eventPayload.put("reason", request.reason());
        }

        incidentEventRepository.save(new IncidentEvent(saved, IncidentEventType.ESCALATED, actor, eventPayload));
        if (oldPriority != newPriority) {
            incidentEventRepository.save(new IncidentEvent(saved, IncidentEventType.PRIORITY_CHANGED, actor, eventPayload));
        }

        auditService.record(
                actor.getId(),
                "INCIDENT_ESCALATED",
                "Incident",
                id.toString(),
                Map.of("oldPriority", oldPriority.name(), "newPriority", newPriority.name()),
                ipAddress
        );

        publishOutboxEvent(
                "INCIDENT",
                saved.getId(),
                KafkaTopics.INCIDENT_ESCALATED,
                new IncidentEscalatedPayload(
                        saved.getId(),
                        oldPriority.name(),
                        newPriority.name(),
                        actor.getId(),
                        request.reason()
                )
        );

        return IncidentDetailResponse.fromEntity(saved);
    }

    public IncidentDetailResponse transitionStatus(UUID id, TransitionStatusRequest request, User actor, String ipAddress) {
        Incident incident = findIncidentEntity(id);
        validateVersion(incident, request.version());

        IncidentStatus oldStatus = incident.getStatus();
        IncidentStatus targetStatus = request.status();

        incidentStateMachine.validateTransition(oldStatus, targetStatus);

        // RBAC check: resolving or closing requires ADMIN or ENGINEER
        if ((targetStatus == IncidentStatus.RESOLVED || targetStatus == IncidentStatus.CLOSED) &&
                actor.getRole() != UserRole.ADMIN && actor.getRole() != UserRole.ENGINEER) {
            throw new AppException(ErrorCode.FORBIDDEN, "Only ADMIN or ENGINEER can resolve or close incidents");
        }

        if (targetStatus == IncidentStatus.RESOLVED) {
            incidentStateMachine.validateResolveFields(request.rootCause(), request.resolution());
            incident.setRootCause(request.rootCause().trim());
            incident.setResolution(request.resolution().trim());
            incident.setResolvedAt(Instant.now());
        } else if (targetStatus == IncidentStatus.CLOSED) {
            incident.setClosedAt(Instant.now());
        } else if (targetStatus == IncidentStatus.ACKNOWLEDGED) {
            if (incident.getAcknowledgedAt() == null) {
                incident.setAcknowledgedAt(Instant.now());
            }
        } else if (targetStatus == IncidentStatus.IN_PROGRESS) {
            if (incident.getAcknowledgedAt() == null) {
                incident.setAcknowledgedAt(Instant.now());
            }
            if (oldStatus == IncidentStatus.RESOLVED) {
                // Reopening
                incident.setResolvedAt(null);
            }
        }

        incident.setStatus(targetStatus);
        incident.setSlaBreached(slaCalculator.isSlaBreached(incident, Instant.now()));

        Incident saved = incidentRepository.saveAndFlush(incident);

        incidentEventRepository.save(new IncidentEvent(
                saved,
                IncidentEventType.STATUS_CHANGED,
                actor,
                Map.of("from", oldStatus.name(), "to", targetStatus.name())
        ));

        auditService.record(
                actor.getId(),
                "INCIDENT_STATUS_CHANGED",
                "Incident",
                id.toString(),
                Map.of("from", oldStatus.name(), "to", targetStatus.name()),
                ipAddress
        );

        publishOutboxEvent(
                "INCIDENT",
                saved.getId(),
                KafkaTopics.INCIDENT_UPDATED,
                new IncidentUpdatedPayload(
                        saved.getId(),
                        targetStatus.name(),
                        saved.getPriority().name(),
                        actor.getId(),
                        "STATUS_TRANSITION"
                )
        );

        return IncidentDetailResponse.fromEntity(saved);
    }

    public CommentResponse addComment(UUID id, AddCommentRequest request, User author) {
        Incident incident = findIncidentEntity(id);

        Comment comment = new Comment(incident, author, request.body().trim());
        Comment saved = commentRepository.save(comment);

        incidentEventRepository.save(new IncidentEvent(
                incident,
                IncidentEventType.COMMENT_ADDED,
                author,
                Map.of("commentId", saved.getId().toString())
        ));

        return CommentResponse.fromEntity(saved);
    }

    public IncidentEventResponse addLogSnippet(UUID id, AddLogSnippetRequest request, User author) {
        Incident incident = findIncidentEntity(id);
        validateSnippetSize(request.snippet());

        IncidentEvent event = new IncidentEvent(
                incident,
                IncidentEventType.LOG_ADDED,
                author,
                Map.of("snippet", request.snippet())
        );
        IncidentEvent saved = incidentEventRepository.save(event);
        return IncidentEventResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<IncidentEventResponse> getTimeline(UUID id) {
        findIncidentEntity(id); // verify existence
        return incidentEventRepository.findByIncidentIdOrderByCreatedAtAsc(id).stream()
                .map(IncidentEventResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(UUID id) {
        findIncidentEntity(id); // verify existence
        return commentRepository.findByIncidentIdOrderByCreatedAtAsc(id).stream()
                .map(CommentResponse::fromEntity)
                .toList();
    }

    public Incident findIncidentEntity(UUID id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Incident not found with id: " + id));
    }

    private void validateVersion(Incident incident, Long version) {
        if (!incident.getVersion().equals(version)) {
            throw new AppException(ErrorCode.STALE_VERSION, "The incident was modified by another user. Please reload.");
        }
    }

    private void validateSnippetSize(String snippet) {
        if (snippet != null && snippet.getBytes(StandardCharsets.UTF_8).length > 4096) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Log snippet exceeds 4 KB maximum size");
        }
    }

    private void publishOutboxEvent(String aggregateType, UUID aggregateId, String eventType, Object payload) {
        if (outboxService != null) {
            outboxService.publish(aggregateType, aggregateId, eventType, payload);
        }
    }
}
