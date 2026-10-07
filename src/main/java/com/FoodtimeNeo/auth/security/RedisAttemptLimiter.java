package com.FoodtimeNeo.auth.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

/** Shared atomic counting; callers supply independent namespaces, quotas and business errors. */
public final class RedisAttemptLimiter {
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
    private RedisAttemptLimiter() { }
    public static Long check(StringRedisTemplate redis, String namespace, String dimension, String identity,
                             String ip, int identityLimit, int ipLimit, Duration window) {
        return redis.execute(LIMIT, List.of(key(namespace, dimension, identity), key(namespace, "ip", ip)),
                Integer.toString(identityLimit), Integer.toString(ipLimit), Long.toString(window.toSeconds()));
    }
    public static String key(String namespace, String dimension, String value) {
        try {
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
            return namespace + ":{rate-limit}:" + dimension + ":" + digest;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }
}
