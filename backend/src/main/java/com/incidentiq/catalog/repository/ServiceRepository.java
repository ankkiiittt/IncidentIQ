package com.incidentiq.catalog.repository;

import com.incidentiq.catalog.entity.ServiceEntity;
import com.incidentiq.catalog.entity.ServiceEnvironment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, UUID> {

    Optional<ServiceEntity> findByNameIgnoreCaseAndEnvironment(String name, ServiceEnvironment environment);

    boolean existsByNameIgnoreCaseAndEnvironment(String name, ServiceEnvironment environment);

    boolean existsByNameIgnoreCaseAndEnvironmentAndIdNot(String name, ServiceEnvironment environment, UUID id);

    Page<ServiceEntity> findByActiveTrue(Pageable pageable);
}
