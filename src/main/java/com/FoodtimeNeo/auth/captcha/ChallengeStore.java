package com.FoodtimeNeo.auth.captcha;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.CaptchaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/** One-time, device-bound challenge storage backed by Redis. */
@Component
public class ChallengeStore {
    private static final Logger LOG = LoggerFactory.getLogger(ChallengeStore.class);
    private static final DefaultRedisScript<String> CONSUME = new DefaultRedisScript<>("""
            local value = redis.call('GET', KEYS[1])
            redis.call('DEL', KEYS[1])
            return value
            """, String.class);
    private final StringRedisTemplate redis;
    private final CaptchaProperties properties;
    private final SecretKeySpec key;

    public ChallengeStore(StringRedisTemplate redis, CaptchaProperties properties) {
        this.redis = redis;
        this.properties = properties;
        this.key = new SecretKeySpec(secretBytes(properties.secret()), "HmacSHA256");
    }

    public void save(String challengeId, int answer, String deviceId, Duration ttl) {
        try {
            redis.opsForValue().set(key(challengeId), digest(Integer.toString(answer)) + "|" + device(deviceId), ttl);
        } catch (DataAccessException failure) {
            throw unavailable();
        }
    }

    /** Atomically removes the challenge before comparing, so a replay is always rejected. */
    public boolean consume(String challengeId, String answer, String deviceId) {
        if (challengeId == null || !challengeId.matches("[0-9a-fA-F-]{36}")) return false;
        String payload;
        try {
            payload = redis.execute(CONSUME, List.of(key(challengeId)));
        } catch (DataAccessException failure) {
            throw unavailable();
        }
        if (payload == null) return false;
        int separator = payload.indexOf('|');
        if (separator <= 0) return false;
        String storedHash = payload.substring(0, separator);
        String storedDevice = payload.substring(separator + 1);
        byte[] expected = digest(answer == null ? "" : answer).getBytes(StandardCharsets.UTF_8);
        byte[] actual = storedHash.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual)
                && MessageDigest.isEqual(device(deviceId).getBytes(StandardCharsets.UTF_8),
                storedDevice.getBytes(StandardCharsets.UTF_8));
    }

    private String key(String challengeId) {
        return properties.redisNamespace() + ":challenge:" + challengeId;
    }

    private String digest(String answer) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(("captcha-v1:" + answer).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("HmacSHA256 is required", failure);
        }
    }

    private static String device(String deviceId) {
        return deviceId == null || deviceId.isBlank() ? "unidentified" : deviceId;
    }

    private static byte[] secretBytes(String secret) {
        if (secret != null && secret.strip().length() >= 32) {
            return secret.strip().getBytes(StandardCharsets.UTF_8);
        }
        byte[] fallback = new byte[32];
        new SecureRandom().nextBytes(fallback);
        LOG.warn("CAPTCHA secret is not configured; using an ephemeral secret for this process");
        return fallback;
    }

    private static BusinessException unavailable() {
        return new BusinessException("CAPTCHA_UNAVAILABLE", "验证码服务暂时不可用，请稍后重试",
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
