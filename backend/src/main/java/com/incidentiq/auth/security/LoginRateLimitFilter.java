package com.incidentiq.auth.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.common.filter.CachedBodyHttpServletRequest;
import com.incidentiq.common.ratelimit.RateLimitResult;
import com.incidentiq.common.ratelimit.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public LoginRateLimitFilter(
            @org.springframework.beans.factory.annotation.Autowired(required = false) RateLimiterService rateLimiterService,
            ObjectMapper objectMapper) {
        this.rateLimiterService = rateLimiterService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (rateLimiterService == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if ("POST".equalsIgnoreCase(request.getMethod()) && "/api/auth/login".equals(request.getRequestURI())) {
            String clientIp = getClientIp(request);
            String ipKey = "ratelimit:login:ip:" + clientIp;
            RateLimitResult ipResult = rateLimiterService.checkRateLimit(ipKey, 5, 60);

            if (!ipResult.allowed()) {
                sendRateLimitError(response, ipResult.resetSeconds(),
                        "Too many login attempts from this IP address. Please retry in " + ipResult.resetSeconds() + " seconds.");
                return;
            }

            byte[] body = request.getInputStream().readAllBytes();
            if (body.length > 0) {
                try {
                    JsonNode json = objectMapper.readTree(body);
                    String email = json.path("email").asText();
                    if (email != null && !email.isBlank()) {
                        String emailKey = "ratelimit:login:email:" + email.trim().toLowerCase();
                        RateLimitResult emailResult = rateLimiterService.checkRateLimit(emailKey, 5, 60);
                        if (!emailResult.allowed()) {
                            sendRateLimitError(response, emailResult.resetSeconds(),
                                    "Too many login attempts for this account. Please retry in " + emailResult.resetSeconds() + " seconds.");
                            return;
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            CachedBodyHttpServletRequest requestWithBody = new CachedBodyHttpServletRequest(request, body);
            filterChain.doFilter(requestWithBody, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendRateLimitError(HttpServletResponse response, long resetSeconds, String detail) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(resetSeconds));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, detail);
        problem.setType(URI.create("urn:problem-type:rate-limit-exceeded"));
        problem.setProperty("errorCode", ErrorCode.RATE_LIMIT_EXCEEDED.getCode());
        problem.setProperty("retryAfterSeconds", resetSeconds);
        problem.setProperty("timestamp", Instant.now().toString());

        objectMapper.writeValue(response.getOutputStream(), problem);
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
