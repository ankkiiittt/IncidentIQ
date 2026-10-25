package com.incidentiq.incident.service;

import com.incidentiq.catalog.entity.ServiceEntity;
import com.incidentiq.catalog.entity.ServiceEnvironment;
import com.incidentiq.catalog.entity.ServiceCriticality;
import com.incidentiq.incident.entity.Incident;
import com.incidentiq.incident.entity.IncidentPriority;
import com.incidentiq.incident.entity.IncidentSeverity;
import com.incidentiq.user.entity.User;
import com.incidentiq.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SlaCalculatorTest {

    private SlaCalculator slaCalculator;
    private Incident incident;

    @BeforeEach
    void setUp() {
        slaCalculator = new SlaCalculator();
        ServiceEntity service = new ServiceEntity("orders-service", ServiceEnvironment.PROD, "Order Team", ServiceCriticality.CRITICAL, "desc");
        User reporter = new User("Alice", "alice@example.com", "hash", UserRole.ENGINEER);
        incident = new Incident("Checkout down", "Errors on checkout", IncidentSeverity.CRITICAL, IncidentPriority.P1, service, reporter, List.of("checkout"), Instant.now());
    }

    @Test
    void targetDurations_matchSpec() {
        assertThat(slaCalculator.getResponseTarget(IncidentPriority.P1)).isEqualTo(Duration.ofMinutes(15));
        assertThat(slaCalculator.getResolveTarget(IncidentPriority.P1)).isEqualTo(Duration.ofMinutes(240));

        assertThat(slaCalculator.getResponseTarget(IncidentPriority.P2)).isEqualTo(Duration.ofMinutes(30));
        assertThat(slaCalculator.getResolveTarget(IncidentPriority.P2)).isEqualTo(Duration.ofMinutes(480));

        assertThat(slaCalculator.getResponseTarget(IncidentPriority.P3)).isEqualTo(Duration.ofMinutes(240));
        assertThat(slaCalculator.getResolveTarget(IncidentPriority.P3)).isEqualTo(Duration.ofMinutes(4320));

        assertThat(slaCalculator.getResponseTarget(IncidentPriority.P4)).isEqualTo(Duration.ofMinutes(1440));
        assertThat(slaCalculator.getResolveTarget(IncidentPriority.P4)).isEqualTo(Duration.ofMinutes(10080));
    }

    @Test
    void responseDue_calculatedCorrectly() {
        Instant now = Instant.parse("2026-10-25T10:00:00Z");
        Instant responseDue = slaCalculator.calculateResponseDue(IncidentPriority.P1, now);
        assertThat(responseDue).isEqualTo(Instant.parse("2026-10-25T10:15:00Z"));
    }

    @Test
    void resolveDue_calculatedCorrectly() {
        Instant now = Instant.parse("2026-10-25T10:00:00Z");
        Instant resolveDue = slaCalculator.calculateResolveDue(IncidentPriority.P2, now);
        assertThat(resolveDue).isEqualTo(Instant.parse("2026-10-25T18:00:00Z"));
    }

    @Test
    void isResponseBreached_unacknowledgedAfterDeadline_returnsTrue() {
        Instant base = Instant.parse("2026-10-25T10:00:00Z");
        incident.setSlaResponseDueAt(base.plus(Duration.ofMinutes(15)));
        incident.setAcknowledgedAt(null);

        // Before deadline
        assertThat(slaCalculator.isResponseBreached(incident, base.plus(Duration.ofMinutes(10)))).isFalse();
        // After deadline
        assertThat(slaCalculator.isResponseBreached(incident, base.plus(Duration.ofMinutes(16)))).isTrue();
    }

    @Test
    void isResponseBreached_acknowledgedInTime_returnsFalse() {
        Instant base = Instant.parse("2026-10-25T10:00:00Z");
        incident.setSlaResponseDueAt(base.plus(Duration.ofMinutes(15)));
        incident.setAcknowledgedAt(base.plus(Duration.ofMinutes(10)));

        // Even 2 hours later, since it was acknowledged on time, breach is false
        assertThat(slaCalculator.isResponseBreached(incident, base.plus(Duration.ofHours(2)))).isFalse();
    }

    @Test
    void isResponseBreached_acknowledgedLate_returnsTrue() {
        Instant base = Instant.parse("2026-10-25T10:00:00Z");
        incident.setSlaResponseDueAt(base.plus(Duration.ofMinutes(15)));
        incident.setAcknowledgedAt(base.plus(Duration.ofMinutes(20)));

        assertThat(slaCalculator.isResponseBreached(incident, base.plus(Duration.ofHours(2)))).isTrue();
    }

    @Test
    void isResolveBreached_unresolvedAfterDeadline_returnsTrue() {
        Instant base = Instant.parse("2026-10-25T10:00:00Z");
        incident.setSlaResolveDueAt(base.plus(Duration.ofHours(4)));
        incident.setResolvedAt(null);

        assertThat(slaCalculator.isResolveBreached(incident, base.plus(Duration.ofHours(3)))).isFalse();
        assertThat(slaCalculator.isResolveBreached(incident, base.plus(Duration.ofHours(5)))).isTrue();
    }

    @Test
    void isResolveBreached_resolvedInTime_returnsFalse() {
        Instant base = Instant.parse("2026-10-25T10:00:00Z");
        incident.setSlaResolveDueAt(base.plus(Duration.ofHours(4)));
        incident.setResolvedAt(base.plus(Duration.ofHours(3)));

        assertThat(slaCalculator.isResolveBreached(incident, base.plus(Duration.ofDays(1)))).isFalse();
    }

    @Test
    void isSlaBreached_eitherResponseOrResolveBreached_returnsTrue() {
        Instant base = Instant.parse("2026-10-25T10:00:00Z");
        incident.setSlaResponseDueAt(base.plus(Duration.ofMinutes(15)));
        incident.setSlaResolveDueAt(base.plus(Duration.ofHours(4)));
        incident.setAcknowledgedAt(base.plus(Duration.ofMinutes(10))); // response OK
        incident.setResolvedAt(null);

        // At 5 hours, resolve is breached -> SLA breached
        assertThat(slaCalculator.isSlaBreached(incident, base.plus(Duration.ofHours(5)))).isTrue();
    }
}
