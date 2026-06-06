package com.querySense.cache;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.type.TypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class QueryCacheService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private static final Duration TTL = Duration.ofMinutes(10);

    public QueryCacheService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    private String keyFor(String question, int page, int size) {
        return "querycache:" + question.trim().toLowerCase() + ":p" + page + ":s" + size;
    }

    public Optional<List<Map<String, Object>>> get(String question, int page, int size) {
        try {
            String json = redis.opsForValue().get(keyFor(question, page, size));
            if (json == null) {
                return Optional.empty();
            }
            List<Map<String, Object>> rows =
                    objectMapper.readValue(json, new TypeReference<>() {});
            return Optional.of(rows);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void put(String question, List<Map<String, Object>> rows, int page, int size) {
        try {
            String json = objectMapper.writeValueAsString(rows);
            redis.opsForValue().set(keyFor(question, page, size), json, TTL);
        } catch (Exception e) {
            // caching failures are non-fatal
        }
    }
}