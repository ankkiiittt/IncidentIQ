package com.incidentiq.common.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final StringRedisTemplate redisTemplate;
    private final Map<String, WindowCounter> inMemoryCounters = new ConcurrentHashMap<>();

    public RateLimiterService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public RateLimitResult checkRateLimit(String key, int maxRequests, int windowSeconds) {
        if (redisTemplate != null) {
            try {
                Long count = redisTemplate.opsForValue().increment(key);
                if (count != null && count == 1) {
                    redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
                }

                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                long resetSeconds = (ttl != null && ttl > 0) ? ttl : windowSeconds;

                boolean allowed = count != null && count <= maxRequests;
                long remaining = Math.max(0, maxRequests - (count != null ? count : 0));

                return new RateLimitResult(allowed, remaining, resetSeconds);
            } catch (Exception e) {
                log.debug("Redis rate limiter unavailable ({}). Falling back to in-memory rate limiter.", e.getMessage());
            }
        }

        // Fallback to in-memory fixed-window bucket
        return checkInMemoryLimit(key, maxRequests, windowSeconds);
    }

    private RateLimitResult checkInMemoryLimit(String key, int maxRequests, int windowSeconds) {
        Instant now = Instant.now();
        WindowCounter counter = inMemoryCounters.compute(key, (k, existing) -> {
            if (existing == null || now.isAfter(existing.expiresAt)) {
                return new WindowCounter(new AtomicInteger(1), now.plusSeconds(windowSeconds));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        int currentCount = counter.count.get();
        long resetSeconds = Math.max(1, Duration.between(now, counter.expiresAt).toSeconds());
        boolean allowed = currentCount <= maxRequests;
        long remaining = Math.max(0, maxRequests - currentCount);

        return new RateLimitResult(allowed, remaining, resetSeconds);
    }

    private static class WindowCounter {
        final AtomicInteger count;
        final Instant expiresAt;

        WindowCounter(AtomicInteger count, Instant expiresAt) {
            this.count = count;
            this.expiresAt = expiresAt;
        }
    }
}
