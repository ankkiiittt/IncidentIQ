package com.incidentiq.incident.repository;

import com.incidentiq.incident.dto.IncidentSearchCriteria;
import com.incidentiq.incident.entity.Incident;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class IncidentSpecifications {

    private IncidentSpecifications() {}

    public static Specification<Incident> withCriteria(IncidentSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria == null) {
                return cb.conjunction();
            }

            if (criteria.q() != null && !criteria.q().trim().isEmpty()) {
                String pattern = "%" + criteria.q().trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(titleMatch, descMatch));
            }

            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }

            if (criteria.priority() != null) {
                predicates.add(cb.equal(root.get("priority"), criteria.priority()));
            }

            if (criteria.severity() != null) {
                predicates.add(cb.equal(root.get("severity"), criteria.severity()));
            }

            if (criteria.category() != null) {
                predicates.add(cb.equal(root.get("category"), criteria.category()));
            }

            if (criteria.serviceId() != null) {
                predicates.add(cb.equal(root.get("service").get("id"), criteria.serviceId()));
            }

            if (criteria.assigneeId() != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), criteria.assigneeId()));
            }

            if (criteria.tag() != null && !criteria.tag().trim().isEmpty()) {
                String tagPattern = "%" + criteria.tag().trim() + "%";
                predicates.add(cb.like(
                        cb.function("array_to_string", String.class, root.get("tags"), cb.literal(",")),
                        tagPattern
                ));
            }

            if (criteria.createdAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.createdAfter()));
            }

            if (criteria.createdBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.createdBefore()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
