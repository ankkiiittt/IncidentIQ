package com.incidentiq.incident.service;

import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.incident.entity.IncidentStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class IncidentStateMachine {

    public void validateTransition(IncidentStatus current, IncidentStatus target) {
        if (current == null || target == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Status cannot be null");
        }
        if (current == target) {
            return;
        }

        Set<IncidentStatus> allowed = current.getAllowedNextStatuses();
        if (!allowed.contains(target)) {
            throw new AppException(
                    ErrorCode.CONFLICT,
                    String.format("Invalid status transition from %s to %s. Allowed transitions: %s", current, target, allowed),
                    Map.of(
                            "currentStatus", current.name(),
                            "targetStatus", target.name(),
                            "allowedTransitions", allowed
                    )
            );
        }
    }

    public void validateResolveFields(String rootCause, String resolution) {
        if (rootCause == null || rootCause.trim().isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Root cause is required when resolving an incident");
        }
        if (resolution == null || resolution.trim().isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Resolution details are required when resolving an incident");
        }
    }
}
