package com.incidentiq.common.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("In-memory rate limiter allows up to limit and blocks subsequent requests")
    void testInMemoryLimiter() {
        RateLimiterService limiter = new RateLimiterService(null);
        String key = "test:ip:192.168.1.100";

        for (int i = 1; i <= 5; i++) {
            RateLimitResult result = limiter.checkRateLimit(key, 5, 60);
            assertThat(result.allowed()).isTrue();
            assertThat(result.remaining()).isEqualTo(5 - i);
        }

        // 6th request should be blocked
        RateLimitResult blocked = limiter.checkRateLimit(key, 5, 60);
        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.remaining()).isEqualTo(0);
        assertThat(blocked.resetSeconds()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Redis rate limiter increments counter and sets TTL on first hit")
    void testRedisRateLimiter() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("test:redis:key")).thenReturn(1L);
        when(redisTemplate.getExpire(eq("test:redis:key"), eq(TimeUnit.SECONDS))).thenReturn(59L);

        RateLimiterService limiter = new RateLimiterService(redisTemplate);
        RateLimitResult result = limiter.checkRateLimit("test:redis:key", 5, 60);

        assertThat(result.allowed()).isTrue();
        assertThat(result.remaining()).isEqualTo(4);
        assertThat(result.resetSeconds()).isEqualTo(59);

        verify(redisTemplate).expire(eq("test:redis:key"), any());
    }

    @Test
    @DisplayName("Redis failure gracefully falls back to in-memory counter")
    void testRedisFailureFallback() {
        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("Connection refused"));

        RateLimiterService limiter = new RateLimiterService(redisTemplate);
        RateLimitResult result = limiter.checkRateLimit("fallback:key", 5, 60);

        // Does not throw and returns allowed result via fallback
        assertThat(result.allowed()).isTrue();
        assertThat(result.remaining()).isEqualTo(4);
    }
}
