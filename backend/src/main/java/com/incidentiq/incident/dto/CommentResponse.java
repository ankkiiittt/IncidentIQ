package com.incidentiq.incident.dto;

import com.incidentiq.incident.entity.Comment;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID incidentId,
        UUID userId,
        String userName,
        String body,
        Instant createdAt,
        Instant editedAt
) {
    public static CommentResponse fromEntity(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getIncident() != null ? comment.getIncident().getId() : null,
                comment.getUser() != null ? comment.getUser().getId() : null,
                comment.getUser() != null ? comment.getUser().getFullName() : null,
                comment.getBody(),
                comment.getCreatedAt(),
                comment.getEditedAt()
        );
    }
}
