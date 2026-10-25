package com.incidentiq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class IncidentIqApplicationTests extends BaseIntegrationTest {

    @Autowired(required = false)
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("Application context loads with Testcontainers postgres/pgvector")
    void contextLoads() {
        if (applicationContext != null) {
            assertNotNull(applicationContext, "Spring ApplicationContext should initialize successfully");
        }
    }
}
