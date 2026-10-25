package com.incidentiq.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentiq.common.ratelimit.RateLimitResult;
import com.incidentiq.common.ratelimit.RateLimiterService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginRateLimitFilterTest {

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private FilterChain filterChain;

    private ObjectMapper objectMapper;
    private LoginRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        filter = new LoginRateLimitFilter(rateLimiterService, objectMapper);
    }

    @Test
    @DisplayName("Login request within limit proceeds down filter chain")
    void testRequestWithinLimitProceeds() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("10.0.0.1");
        request.setContent("{\"email\":\"user@example.com\",\"password\":\"Secret123!\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimiterService.checkRateLimit(contains("10.0.0.1"), anyInt(), anyInt()))
                .thenReturn(new RateLimitResult(true, 4, 60));
        when(rateLimiterService.checkRateLimit(contains("user@example.com"), anyInt(), anyInt()))
                .thenReturn(new RateLimitResult(true, 4, 60));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(), eq(response));
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Login request exceeding IP limit returns 429 Too Many Requests with Retry-After")
    void testRequestExceedingIpLimitReturns429() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("10.0.0.5");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimiterService.checkRateLimit(contains("10.0.0.5"), anyInt(), anyInt()))
                .thenReturn(new RateLimitResult(false, 0, 45));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("45");
        assertThat(response.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("Login request exceeding account email limit returns 429 Too Many Requests")
    void testRequestExceedingEmailLimitReturns429() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("10.0.0.1");
        request.setContent("{\"email\":\"targeted@example.com\",\"password\":\"Secret123!\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimiterService.checkRateLimit(contains("10.0.0.1"), anyInt(), anyInt()))
                .thenReturn(new RateLimitResult(true, 4, 60));
        when(rateLimiterService.checkRateLimit(contains("targeted@example.com"), anyInt(), anyInt()))
                .thenReturn(new RateLimitResult(false, 0, 30));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("30");
        assertThat(response.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }
}
