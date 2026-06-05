package com.querySense.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitService {

    private final StringRedisTemplate redis;

    private static final int MAX_REQUESTS = 10;        // per window
    private static final Duration WINDOW = Duration.ofMinutes(1);

    public RateLimitService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Returns true if the request is allowed, false if the limit is exceeded. */
    public boolean allow(String clientId) {
        String key = "ratelimit:" + clientId;

        Long count = redis.opsForValue().increment(key);  // atomic +1, creates at 1

        if (count != null && count == 1L) {
            // first request in this window — start the expiry clock
            redis.expire(key, WINDOW);
        }

        return count != null && count <= MAX_REQUESTS;
    }
}