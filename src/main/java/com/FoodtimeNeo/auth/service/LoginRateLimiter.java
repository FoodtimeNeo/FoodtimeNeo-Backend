package com.FoodtimeNeo.auth.service;

import com.FoodtimeNeo.config.properties.AuthProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
public class LoginRateLimiter {
    // Both keys share a hash slot; increments and expiration are atomic even with concurrent requests.
    private static final DefaultRedisScript<Long> LIMIT = new DefaultRedisScript<>("""
            local a = tonumber(redis.call('GET', KEYS[1]) or '0')
            local b = tonumber(redis.call('GET', KEYS[2]) or '0')
            local retry = 0
            if a >= tonumber(ARGV[1]) then retry = math.max(retry, redis.call('TTL', KEYS[1])) end
            if b >= tonumber(ARGV[2]) then retry = math.max(retry, redis.call('TTL', KEYS[2])) end
            if a >= tonumber(ARGV[1]) or b >= tonumber(ARGV[2]) then return math.max(1, retry) end
            for i = 1, 2 do
              if redis.call('INCR', KEYS[i]) == 1 then redis.call('EXPIRE', KEYS[i], ARGV[3]) end
            end
            return 0
            """, Long.class);
    private final StringRedisTemplate redis;
    private final AuthProperties properties;

    public LoginRateLimiter(StringRedisTemplate redis, AuthProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public void check(String account, String remoteAddress) {
        try {
            Long retry = redis.execute(LIMIT, List.of(key("account", account), key("ip", remoteAddress)),
                    Integer.toString(properties.loginAccountLimit()), Integer.toString(properties.loginIpLimit()),
                    Long.toString(properties.loginWindow().toSeconds()));
            if (retry == null) { throw LoginService.unavailable(); }
            if (retry > 0) { throw new LoginRateLimitException(retry); }
        } catch (DataAccessException exception) {
            // Fail closed: a missing limiter must not silently enable unlimited password hashing.
            throw LoginService.unavailable();
        }
    }

    public String key(String dimension, String value) {
        try {
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
            return properties.redisNamespace() + ":{rate-limit}:" + dimension + ":" + digest;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }
}
