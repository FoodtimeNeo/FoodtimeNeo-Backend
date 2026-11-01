package com.FoodtimeNeo.auth.reset;

import com.FoodtimeNeo.auth.dto.*;
import com.FoodtimeNeo.auth.security.RedisAttemptLimiter;
import com.FoodtimeNeo.auth.verification.EmailRateLimitException;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.*;
import com.FoodtimeNeo.user.entity.LoginAccount;
import com.FoodtimeNeo.user.mapper.UserMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

@Service @Validated
public class PasswordResetService {
    private static final Logger LOG=LoggerFactory.getLogger(PasswordResetService.class);
    private final PasswordResetStore store; private final PasswordResetProperties properties;
    private final EmailVerificationProperties email; private final UserMapper users; private final PasswordEncoder passwords;
    private final StringRedisTemplate redis; private final ObjectProvider<JavaMailSender> mail; private final Executor executor;
    private final Clock clock; private final SecureRandom random=new SecureRandom();
    public PasswordResetService(PasswordResetStore store,PasswordResetProperties properties,EmailVerificationProperties email,
            UserMapper users,PasswordEncoder passwords,StringRedisTemplate redis,ObjectProvider<JavaMailSender> mail,
            @Qualifier("passwordResetMailExecutor") Executor executor,Clock clock) {
        this.store=store;this.properties=properties;this.email=email;this.users=users;this.passwords=passwords;
        this.redis=redis;this.mail=mail;this.executor=executor;this.clock=clock;
    }
    public EmailCodeResponse send(@NotNull @Valid PasswordResetEmailRequest request,String ip,String device) {
        requireEnabled();JavaMailSender sender=mail.getIfAvailable();if(sender==null)throw unavailable();
        try {
            String reservation=UUID.randomUUID().toString();long retry=store.reserve(request.email(),ip,device,reservation);
            if(retry>0)throw new EmailRateLimitException(retry);
            // Account lookup and SMTP both occur after queue admission, including unknown/disabled emails.
            executor.execute(()->deliver(request.email(),reservation,sender));
            return new EmailCodeResponse(clock.instant().plus(properties.codeTtl()),properties.resendInterval().toSeconds());
        } catch(DataAccessException|RejectedExecutionException e){throw unavailable();}
    }
    private void deliver(String address,String reservation,JavaMailSender sender) {
        try {
            LoginAccount account=users.findLoginAccount(address);if(!active(account))return;
            String code=String.format(Locale.ROOT,"%06d",random.nextInt(1_000_000));
            var message=message(account.email(),"FoodtimeNeo 重置密码验证码","您的重置密码验证码："+code+"\n"+
                "验证码"+properties.codeTtl().toMinutes()+"分钟内有效，请勿向任何人透露。\n如非本人操作，请忽略此邮件。");
            sender.send(message);
            if(!store.publish(account,reservation,code))LOG.warn("Password reset proof superseded before publication");
        } catch(RuntimeException e){LOG.error("Password reset delivery failed: {}",e.getClass().getSimpleName());}
        // Keep cooldown and quotas for every result: delivery/account differences must not change HTTP behavior.
    }
    public PasswordResetGrantResponse verify(@NotNull @Valid PasswordResetVerifyRequest request,String ip) {
        requireEnabled();
        try {
            limit("verify",request.email(),ip,properties.verifyAccountLimit(),properties.verifyIpLimit());
            LoginAccount account=users.findLoginAccount(request.email());
            byte[] bytes=new byte[32];random.nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            if(!active(account)||!store.verify(account,request.verificationCode(),token))throw invalidCode();
            return new PasswordResetGrantResponse(token,clock.instant().plus(properties.tokenTtl()));
        } catch(DataAccessException e){throw unavailable();}
    }
    public void reset(@NotNull @Valid PasswordResetRequest request,String ip) {
        requireEnabled();
        try {
            limit("reset",request.resetToken(),ip,5,properties.verifyIpLimit());
            // Consume BEFORE any write. Unknown database outcome never makes a used token reusable.
            // An infrastructure error requires a fresh email verification rather than replaying a write.
            var grant=store.consume(request.resetToken());
            LoginAccount account=users.findLoginAccountById(grant.userId());
            if(!active(account)||!account.passwordChangedAt().toString().equals(grant.version()))throw invalidGrant();
            String hash=passwords.encode(request.newPassword());
            if(users.changePassword(account.id(),account.passwordHash(),account.passwordChangedAt(),hash)!=1)throw invalidGrant();
            try { executor.execute(()->notifyChanged(account.email())); }
            catch(RejectedExecutionException e){LOG.warn("Password change notification queue is full");}
        } catch(DataAccessException e){throw unavailable();}
    }
    private void notifyChanged(String address) {
        try { var sender=mail.getIfAvailable();if(sender!=null)sender.send(message(address,"FoodtimeNeo 密码已重置",
            "您的账号密码已通过邮箱验证重置，旧设备需重新登录。如非本人操作，请立即保护邮箱并联系管理员。")); }
        catch(RuntimeException e){LOG.warn("Password change notification failed: {}",e.getClass().getSimpleName());}
    }
    private SimpleMailMessage message(String address,String subject,String body) {
        var message=new SimpleMailMessage();message.setFrom(email.from());message.setTo(address);message.setSubject(subject);message.setText(body);return message;
    }
    private void limit(String scope,String identity,String ip,int accounts,int ips) {
        Long retry=RedisAttemptLimiter.check(redis,properties.redisNamespace()+":"+scope,"account",identity,ip,accounts,ips,Duration.ofMinutes(15));
        if(retry==null)throw unavailable();if(retry>0)throw new EmailRateLimitException(retry);
    }
    private void requireEnabled(){if(!email.enabled()||email.secret()==null||email.secret().length()<32)throw unavailable();}
    private static boolean active(LoginAccount account){return account!=null&&"active".equals(account.status());}
    public static BusinessException invalidCode(){return new BusinessException("PASSWORD_RESET_CODE_INVALID","验证码错误或已过期，请重新验证邮箱",HttpStatus.BAD_REQUEST);}
    public static BusinessException invalidGrant(){return new BusinessException("PASSWORD_RESET_TOKEN_INVALID","验证已失效或已使用，请重新验证邮箱",HttpStatus.BAD_REQUEST);}
    public static BusinessException unavailable(){return new BusinessException("PASSWORD_RESET_UNAVAILABLE","找回密码暂时不可用；若已提交重置，请尝试登录确认结果或重新验证邮箱",HttpStatus.SERVICE_UNAVAILABLE);}
}
