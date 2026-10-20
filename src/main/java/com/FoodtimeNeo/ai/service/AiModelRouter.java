package com.FoodtimeNeo.ai.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.AiTriageProperties;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Resolves logical model slots (primary/escalation/fallback/photo) to concrete model ids.
 *
 * <p>Precedence per call: Redis override first, env-configured default second, env default
 * on Redis failure (fail-open: a Redis outage must never stop triage). The monitor process
 * and superadmins pin new ids into the Redis hash to hot-swap models without restarting.
 */
@Service
public class AiModelRouter {
    static final Set<String> SLOTS = Set.of("primary-model", "escalation-model", "fallback-model", "photo-model");

    private static final Logger LOG = LoggerFactory.getLogger(AiModelRouter.class);

    private final AiTriageProperties properties;
    private final StringRedisTemplate redis;
    private final AtomicBoolean redisDownLogged = new AtomicBoolean(false);

    public AiModelRouter(AiTriageProperties properties, StringRedisTemplate redis) {
        this.properties = properties;
        this.redis = redis;
    }

    public String primary() {
        return resolve("primary-model", properties.primaryModel());
    }

    public String escalation() {
        return resolve("escalation-model", properties.escalationModel());
    }

    public String fallback() {
        return resolve("fallback-model", properties.fallbackModel());
    }

    public String photo() {
        return resolve("photo-model", properties.photoModel());
    }

    /** Effective mapping for observability: model id plus where it came from. */
    public Map<String, Map<String, String>> snapshot() {
        Map<String, Map<String, String>> out = new LinkedHashMap<>();
        out.put("primary-model", entry("primary-model", properties.primaryModel()));
        out.put("escalation-model", entry("escalation-model", properties.escalationModel()));
        out.put("fallback-model", entry("fallback-model", properties.fallbackModel()));
        out.put("photo-model", entry("photo-model", properties.photoModel()));
        return out;
    }

    /**
     * Pins a slot to a new model id. Superadmin only; recorded with actor and timestamp
     * so a bad promotion can be traced and reverted with the same call.
     */
    public void pin(UserProfile actor, String slot, String model, String reason) {
        if (!actor.status().equals("active") || !"superadmin".equals(actor.role())) {
            throw new BusinessException("ACCESS_DENIED", "需要平台管理员权限", HttpStatus.FORBIDDEN);
        }
        if (slot == null || !SLOTS.contains(slot)) {
            throw new BusinessException("VALIDATION_ERROR", "模型槽位不合法", HttpStatus.BAD_REQUEST);
        }
        String id = model == null ? "" : model.strip();
        if (id.isEmpty() || id.length() > 200 || !id.matches("[A-Za-z0-9][A-Za-z0-9._/:-]*")) {
            throw new BusinessException("VALIDATION_ERROR", "模型 ID 不合法", HttpStatus.BAD_REQUEST);
        }
        String note = reason == null ? "" : reason.strip();
        if (note.length() > 200) {
            throw new BusinessException("VALIDATION_ERROR", "备注过长", HttpStatus.BAD_REQUEST);
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(slot, id);
        fields.put("pinned-at", Instant.now().toString());
        fields.put("pinned-by", actor.id().toString());
        fields.put("reason", note);
        redis.opsForHash().putAll(properties.redisNamespace(), fields);
        LOG.info("AI model pinned slot={} model={} actor={} reason={}", slot, id, actor.id(), note);
    }

    /** Promotion metadata (pinned-at/pinned-by/reason); null when never pinned. */
    public String meta(String field) {
        try {
            Object value = redis.opsForHash().get(properties.redisNamespace(), field);
            return value instanceof String text && !text.isBlank() ? text.strip() : null;
        } catch (RuntimeException failure) {
            return null;
        }
    }

    private Map<String, String> entry(String slot, String envDefault) {
        String override = readOverride(slot);
        Map<String, String> row = new LinkedHashMap<>();
        if (override != null) {
            row.put("model", override);
            row.put("source", "redis");
        } else {
            row.put("model", envDefault);
            row.put("source", "env");
        }
        return row;
    }

    private String resolve(String slot, String envDefault) {
        String override = readOverride(slot);
        return override != null ? override : envDefault;
    }

    private String readOverride(String slot) {
        try {
            Object value = redis.opsForHash().get(properties.redisNamespace(), slot);
            redisDownLogged.set(false);
            if (value instanceof String text && !text.isBlank()) {
                return text.strip();
            }
            return null;
        } catch (RuntimeException failure) {
            if (redisDownLogged.compareAndSet(false, true)) {
                LOG.warn("AI model map unreadable, falling back to env defaults: {}",
                        failure.getClass().getSimpleName());
            }
            return null;
        }
    }
}
