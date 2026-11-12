package com.FoodtimeNeo.auth.captcha;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.CaptchaProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class CaptchaService {
    private final ChallengeProvider provider;
    private final ChallengeStore store;
    private final CaptchaProperties properties;
    private final Clock clock;

    public CaptchaService(ChallengeProvider provider, ChallengeStore store,
                          CaptchaProperties properties, Clock clock) {
        this.provider = provider;
        this.store = store;
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedCaptcha issue(String deviceId) {
        if (!properties.enabled()) {
            throw new BusinessException("CAPTCHA_DISABLED", "登录验证码未启用", HttpStatus.NOT_FOUND);
        }
        GeneratedChallenge generated;
        try {
            generated = provider.generate();
        } catch (RuntimeException failure) {
            throw new BusinessException("CAPTCHA_UNAVAILABLE", "验证码暂时不可用，请稍后重试",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        String challengeId = UUID.randomUUID().toString();
        store.save(challengeId, generated.answer(), deviceId, properties.challengeTtl());
        return new IssuedCaptcha(challengeId, generated.image(),
                clock.instant().plus(properties.challengeTtl()));
    }

    public boolean verify(String challengeId, String answer, String deviceId) {
        return properties.enabled() && store.consume(challengeId, answer, deviceId);
    }

    public record IssuedCaptcha(String challengeId, byte[] image, Instant expiresAt) {
        public IssuedCaptcha {
            image = image.clone();
        }

        @Override
        public byte[] image() {
            return image.clone();
        }
    }
}
