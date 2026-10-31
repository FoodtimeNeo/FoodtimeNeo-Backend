package com.FoodtimeNeo.dashboard;

import com.FoodtimeNeo.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * 在线用户 presence：每次已认证请求为当前用户写一个带 TTL 的 Redis 键。
 * 与登录会话解耦，避免把保留数天的会话都算成“在线”。
 * 同一用户多端登录只算一个；TTL 内没有请求即自动下线。
 */
@Service
public class OnlinePresence {
    private static final Logger LOG = LoggerFactory.getLogger(OnlinePresence.class);
    private final StringRedisTemplate redis;
    private final String namespace;
    private final Duration ttl;

    public OnlinePresence(StringRedisTemplate redis,
            @Value("${app.dashboard.presence-namespace:foodtime:presence}") String namespace,
            @Value("${app.dashboard.presence-ttl:PT5M}") Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Dashboard presence TTL must be positive");
        }
        this.redis = redis;
        this.namespace = namespace;
        this.ttl = ttl;
    }

    /** 尽力而为：presence 写失败不能影响正常请求。 */
    public void touch(UUID userId) {
        if (userId == null) {
            return;
        }
        try {
            redis.opsForValue().set(key(userId), "1", ttl);
        } catch (DataAccessException exception) {
            LOG.warn("Online presence write failed: {}", exception.getClass().getSimpleName());
        }
    }

    /** TTL 内仍有请求的去重用户数。Redis 不可用时返回 503，与看板其他聚合一致。 */
    public long count() {
        String match = namespace + ":user:*";
        try {
            Long count = redis.execute((RedisCallback<Long>) connection -> {
                long found = 0;
                try (Cursor<byte[]> cursor = connection.scan(
                        ScanOptions.scanOptions().match(match).count(1000).build())) {
                    while (cursor.hasNext()) {
                        cursor.next();
                        found++;
                    }
                }
                return found;
            });
            return count == null ? 0 : count;
        } catch (DataAccessException exception) {
            LOG.error("Online presence scan failed: {}", exception.getClass().getSimpleName());
            throw new BusinessException("DASHBOARD_UNAVAILABLE", "数据看板服务暂时不可用",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private String key(UUID userId) {
        return namespace + ":user:" + userId;
    }
}
