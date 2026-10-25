package com.incidentiq.incident.repository;

import com.incidentiq.incident.entity.Incident;
import com.incidentiq.incident.entity.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID>, JpaSpecificationExecutor<Incident> {

    Optional<Incident> findByNumber(Long number);

    boolean existsByServiceIdAndStatusNotIn(UUID serviceId, Collection<IncidentStatus> statuses);

    long countByServiceId(UUID serviceId);
}
