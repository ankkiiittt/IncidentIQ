package com.incidentiq.incident.service;

import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.incident.entity.IncidentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncidentStateMachineTest {

    private IncidentStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new IncidentStateMachine();
    }

    @ParameterizedTest
    @CsvSource({
            "OPEN, ACKNOWLEDGED",
            "OPEN, IN_PROGRESS",
            "ACKNOWLEDGED, IN_PROGRESS",
            "IN_PROGRESS, RESOLVED",
            "RESOLVED, CLOSED",
            "RESOLVED, IN_PROGRESS"
    })
    void validTransitions_succeed(IncidentStatus from, IncidentStatus to) {
        assertThatCode(() -> stateMachine.validateTransition(from, to))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "OPEN, RESOLVED",
            "OPEN, CLOSED",
            "ACKNOWLEDGED, OPEN",
            "ACKNOWLEDGED, RESOLVED",
            "ACKNOWLEDGED, CLOSED",
            "IN_PROGRESS, OPEN",
            "IN_PROGRESS, ACKNOWLEDGED",
            "IN_PROGRESS, CLOSED",
            "CLOSED, OPEN",
            "CLOSED, ACKNOWLEDGED",
            "CLOSED, IN_PROGRESS",
            "CLOSED, RESOLVED"
    })
    void invalidTransitions_throwConflict(IncidentStatus from, IncidentStatus to) {
        assertThatThrownBy(() -> stateMachine.validateTransition(from, to))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThatCode(() -> {}).doesNotThrowAnyException();
                    assert appEx.getErrorCode() == ErrorCode.CONFLICT;
                });
    }

    @Test
    void sameStatus_succeedsSilently() {
        assertThatCode(() -> stateMachine.validateTransition(IncidentStatus.OPEN, IncidentStatus.OPEN))
                .doesNotThrowAnyException();
    }

    @Test
    void nullStatus_throwsValidationFailed() {
        assertThatThrownBy(() -> stateMachine.validateTransition(null, IncidentStatus.OPEN))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assert appEx.getErrorCode() == ErrorCode.VALIDATION_FAILED;
                });
    }

    @Test
    void validateResolveFields_withValidInput_succeeds() {
        assertThatCode(() -> stateMachine.validateResolveFields("Postgres pool exhausted", "Increased pool size to 50"))
                .doesNotThrowAnyException();
    }

    @Test
    void validateResolveFields_withMissingRootCause_throwsValidationFailed() {
        assertThatThrownBy(() -> stateMachine.validateResolveFields("", "Fixed config"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assert appEx.getErrorCode() == ErrorCode.VALIDATION_FAILED;
                });

        assertThatThrownBy(() -> stateMachine.validateResolveFields(null, "Fixed config"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assert appEx.getErrorCode() == ErrorCode.VALIDATION_FAILED;
                });
    }

    @Test
    void validateResolveFields_withMissingResolution_throwsValidationFailed() {
        assertThatThrownBy(() -> stateMachine.validateResolveFields("Memory leak", "  "))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assert appEx.getErrorCode() == ErrorCode.VALIDATION_FAILED;
                });

        assertThatThrownBy(() -> stateMachine.validateResolveFields("Memory leak", null))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assert appEx.getErrorCode() == ErrorCode.VALIDATION_FAILED;
                });
    }
}
