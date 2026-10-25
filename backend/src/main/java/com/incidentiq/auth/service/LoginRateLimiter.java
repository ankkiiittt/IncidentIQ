package com.incidentiq.auth.service;

import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS_PER_MINUTE = 5;
    private static final long WINDOW_SECONDS = 60;

    private static class AttemptWindow {
        int count;
        long windowStartSeconds;

        AttemptWindow(long windowStartSeconds) {
            this.count = 1;
            this.windowStartSeconds = windowStartSeconds;
        }
    }

    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public void checkRateLimit(String ipAddress, String email) {
        String key = (ipAddress != null ? ipAddress : "unknown") + ":" + (email != null ? email.toLowerCase() : "unknown");
        long currentSeconds = Instant.now().getEpochSecond();

        attempts.compute(key, (k, window) -> {
            if (window == null || (currentSeconds - window.windowStartSeconds) >= WINDOW_SECONDS) {
                return new AttemptWindow(currentSeconds);
            }
            if (window.count >= MAX_ATTEMPTS_PER_MINUTE) {
                long retryAfter = WINDOW_SECONDS - (currentSeconds - window.windowStartSeconds);
                throw new AppException(
                        ErrorCode.RATE_LIMIT_EXCEEDED,
                        "Too many login attempts. Please retry after " + Math.max(1, retryAfter) + " seconds."
                );
            }
            window.count++;
            return window;
        });
    }

    public void clear(String ipAddress, String email) {
        String key = (ipAddress != null ? ipAddress : "unknown") + ":" + (email != null ? email.toLowerCase() : "unknown");
        attempts.remove(key);
    }
}
