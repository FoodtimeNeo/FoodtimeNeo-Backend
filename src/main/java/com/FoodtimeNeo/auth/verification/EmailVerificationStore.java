package com.FoodtimeNeo.auth.verification;

import com.FoodtimeNeo.config.properties.EmailVerificationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@Component
public class EmailVerificationStore {
    private static final DefaultRedisScript<Long> RESERVE = script("reserve-send");
    private static final DefaultRedisScript<Long> PUBLISH = script("publish");
    private static final DefaultRedisScript<Long> CLAIM = script("claim");
    private static final DefaultRedisScript<Long> FINISH = script("finish");
    private static final DefaultRedisScript<Long> CANCEL = script("cancel-send");
    private final StringRedisTemplate redis;
    private final EmailVerificationProperties properties;

    public EmailVerificationStore(StringRedisTemplate redis, EmailVerificationProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public long reserveSend(String email, String ip, String token) {
        return result(redis.execute(RESERVE, List.of(key("code", email), key("cooldown", email),
                key("account", email), key("ip", ip)), token, Long.toString(properties.resendInterval().toSeconds()),
                Long.toString(properties.sendWindow().toSeconds()), Integer.toString(properties.sendAccountLimit()),
                Integer.toString(properties.sendIpLimit())));
    }

    public boolean publish(String email, String token, String code) {
        return result(redis.execute(PUBLISH, List.of(key("code", email), key("cooldown", email)), token,
                proof(email, code), Long.toString(EmailVerificationProperties.CODE_TTL.toSeconds()))) == 1;
    }

    public long claim(String email, String code, String token) {
        return result(redis.execute(CLAIM, List.of(key("code", email)), proof(email, code), token,
                Integer.toString(EmailVerificationProperties.CLAIM_SECONDS),
                Integer.toString(EmailVerificationProperties.MAX_ATTEMPTS)));
    }

    public void finish(VerificationClaim claim, boolean consume) {
        redis.execute(FINISH, List.of(key("code", claim.email())), claim.token(), consume ? "consume" : "release");
    }

    public void cancelSend(String email, String token) {
        redis.execute(CANCEL, List.of(key("cooldown", email)), token);
    }

    public String key(String kind, String value) {
        try {
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
            // Shared hash tag also allows atomic operations on Redis Cluster.
            return properties.redisNamespace() + ":{verification}:" + kind + ":" + digest;
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }

    private String proof(String email, String code) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(hmac.doFinal((email + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 is required", exception);
        }
    }

    private static long result(Long value) {
        if (value == null) { throw EmailVerificationService.unavailable(); }
        return value;
    }

    private static DefaultRedisScript<Long> script(String name) {
        var script = new DefaultRedisScript<Long>();
        script.setLocation(new ClassPathResource("redis/email/" + name + ".lua"));
        script.setResultType(Long.class);
        return script;
    }
}
