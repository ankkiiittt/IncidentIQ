package com.incidentiq.common.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Should translate AppException into RFC 7807 ProblemDetail")
    void shouldHandleAppException() {
        AppException ex = new AppException(
                ErrorCode.NOT_FOUND,
                "Incident INC-0001 not found",
                Map.of("incidentId", "INC-0001")
        );

        ResponseEntity<ProblemDetail> response = handler.handleAppException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ProblemDetail body = response.getBody();
        assertNotNull(body);
        assertEquals("Incident INC-0001 not found", body.getDetail());
        assertEquals("NOT_FOUND", body.getProperties().get("errorCode"));
        assertNotNull(body.getProperties().get("details"));
    }

    @Test
    @DisplayName("Should handle generic unexpected exception as INTERNAL_ERROR")
    void shouldHandleGenericException() {
        RuntimeException ex = new RuntimeException("Unexpected database failure");

        ResponseEntity<ProblemDetail> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ProblemDetail body = response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.INTERNAL_ERROR.getDefaultMessage(), body.getDetail());
        assertEquals("INTERNAL_ERROR", body.getProperties().get("errorCode"));
    }
}
