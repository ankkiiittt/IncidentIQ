package com.incidentiq.catalog.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "services", uniqueConstraints = {
        @UniqueConstraint(name = "uq_services_name_environment", columnNames = {"name", "environment"})
})
public class ServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServiceEnvironment environment;

    @Column(name = "owner_team", nullable = false, length = 100)
    private String ownerTeam;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ServiceCriticality criticality;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ServiceEntity() {
    }

    public ServiceEntity(String name, ServiceEnvironment environment, String ownerTeam, ServiceCriticality criticality, String description) {
        this.name = name;
        this.environment = environment;
        this.ownerTeam = ownerTeam;
        this.criticality = criticality;
        this.description = description;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ServiceEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(ServiceEnvironment environment) {
        this.environment = environment;
    }

    public String getOwnerTeam() {
        return ownerTeam;
    }

    public void setOwnerTeam(String ownerTeam) {
        this.ownerTeam = ownerTeam;
    }

    public ServiceCriticality getCriticality() {
        return criticality;
    }

    public void setCriticality(ServiceCriticality criticality) {
        this.criticality = criticality;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
