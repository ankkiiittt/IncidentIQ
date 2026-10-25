package com.incidentiq.catalog.repository;

import com.incidentiq.catalog.entity.DeploymentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface DeploymentRepository extends JpaRepository<DeploymentEntity, UUID> {

    Page<DeploymentEntity> findByServiceIdOrderByDeployedAtDesc(UUID serviceId, Pageable pageable);

    List<DeploymentEntity> findByServiceIdAndDeployedAtAfterOrderByDeployedAtDesc(UUID serviceId, Instant deployedAt);
}
