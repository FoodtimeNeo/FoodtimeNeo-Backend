package com.FoodtimeNeo.auth.verification;

import com.FoodtimeNeo.auth.dto.EmailCodeRequest;
import com.FoodtimeNeo.auth.dto.EmailCodeResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.EmailVerificationProperties;
import com.FoodtimeNeo.user.mapper.UserMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Locale;
import java.util.UUID;

@Service
@Validated
public class EmailVerificationService {
    private static final Logger LOG = LoggerFactory.getLogger(EmailVerificationService.class);
    private final EmailVerificationStore store;
    private final EmailVerificationProperties properties;
    private final ObjectProvider<JavaMailSender> mail;
    private final UserMapper users;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(EmailVerificationStore store, EmailVerificationProperties properties,
                                     ObjectProvider<JavaMailSender> mail, UserMapper users, Clock clock) {
        this.store = store;
        this.properties = properties;
        this.mail = mail;
        this.users = users;
        this.clock = clock;
    }

    public EmailCodeResponse send(@NotNull @Valid EmailCodeRequest request, String remoteAddress) {
        requireEnabled();
        JavaMailSender sender = mail.getIfAvailable();
        if (sender == null) { throw unavailable(); }
        String token = UUID.randomUUID().toString();
        boolean reserved = false;
        boolean published = false;
        try {
            if (users.existsByEmail(request.email())) {
                throw new BusinessException("EMAIL_ALREADY_REGISTERED", "该邮箱已注册", HttpStatus.CONFLICT);
            }
            long retry = store.reserveSend(request.email(), remoteAddress, token);
            if (retry > 0) { throw new EmailRateLimitException(retry); }
            reserved = true;
            String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
            var message = new SimpleMailMessage();
            message.setFrom(properties.from());
            message.setTo(request.email());
            message.setSubject("FoodtimeNeo 注册验证码");
            message.setText("您的注册验证码：" + code + "\n验证码15分钟内有效，请勿向他人透露。\n如非本人操作，请忽略此邮件。");
            sender.send(message);
            if (!store.publish(request.email(), token, code)) { throw unavailable(); }
            published = true;
            return new EmailCodeResponse(clock.instant().plus(EmailVerificationProperties.CODE_TTL),
                    properties.resendInterval().toSeconds());
        } catch (DataAccessException | MailException exception) {
            LOG.error("Email verification delivery failed: {}", exception.getClass().getSimpleName());
            throw unavailable();
        } finally {
            if (reserved && !published) {
                try { store.cancelSend(request.email(), token); }
                catch (DataAccessException exception) { LOG.error("Email send cleanup failed: {}", exception.getClass().getSimpleName()); }
            }
        }
    }

    public VerificationClaim claim(String email, String code) {
        requireEnabled();
        String token = UUID.randomUUID().toString();
        try {
            long result = store.claim(email, code, token);
            if (result == 2) {
                throw new BusinessException("REGISTRATION_IN_PROGRESS", "该邮箱正在注册，请稍后重试", HttpStatus.CONFLICT);
            }
            if (result != 1) {
                throw new BusinessException("EMAIL_CODE_INVALID", "邮箱验证码错误或已失效，请重新获取或检查后重试", HttpStatus.BAD_REQUEST);
            }
            return new VerificationClaim(email, token);
        } catch (DataAccessException exception) {
            throw unavailable();
        }
    }

    public void finish(VerificationClaim claim, boolean registered) {
        try { store.finish(claim, registered); }
        catch (DataAccessException exception) {
            // The database unique index prevents replay after commit. Never report a committed account as a failure.
            // An uncommitted claim will release itself after its short lease, without extending the code's TTL.
            LOG.error("Email verification finalization failed: {}", exception.getClass().getSimpleName());
        }
    }

    private void requireEnabled() {
        if (!properties.enabled()) { throw unavailable(); }
    }

    public static BusinessException unavailable() {
        return new BusinessException("EMAIL_VERIFICATION_UNAVAILABLE", "邮箱验证服务暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
