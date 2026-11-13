package com.FoodtimeNeo.user.lifecycle;

import com.FoodtimeNeo.auth.security.RedisAttemptLimiter;
import com.FoodtimeNeo.auth.verification.EmailRateLimitException;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.AuthProperties;
import com.FoodtimeNeo.config.properties.EmailVerificationProperties;
import com.FoodtimeNeo.user.entity.UserProfile;
import com.FoodtimeNeo.user.service.ProfileService;
import org.slf4j.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import static com.FoodtimeNeo.user.lifecycle.AccountLifecycleDtos.*;

@Service
public class AccountLifecycleService {
    private static final Logger LOG=LoggerFactory.getLogger(AccountLifecycleService.class);
    private static final ZoneOffset ZONE=ZoneOffset.ofHours(8);
    private final AccountLifecycleStore store;private final EmailCooldowns cooldowns;private final ProfileService profiles;
    private final EmailVerificationProperties email;private final AuthProperties auth;private final ObjectProvider<JavaMailSender> mail;
    private final PasswordEncoder passwords;private final Clock clock;private final StringRedisTemplate redis;private final TransactionTemplate tx;
    private final SecureRandom random=new SecureRandom();
    public AccountLifecycleService(AccountLifecycleStore store,EmailCooldowns cooldowns,ProfileService profiles,
            EmailVerificationProperties email,AuthProperties auth,ObjectProvider<JavaMailSender> mail,PasswordEncoder passwords,
            Clock clock,StringRedisTemplate redis,PlatformTransactionManager manager){
        this.store=store;this.cooldowns=cooldowns;this.profiles=profiles;this.email=email;this.auth=auth;this.mail=mail;
        this.passwords=passwords;this.clock=clock;this.redis=redis;tx=new TransactionTemplate(manager);
    }
    private static Instant tomorrow(Instant now){return now.atOffset(ZONE).toLocalDate().plusDays(1).atStartOfDay().toInstant(ZONE);}
    private static LocalDate day(Instant now){return now.atOffset(ZONE).toLocalDate();}
    private AccountLifecycleStore.Identity active(UUID id,boolean lock){var value=store.identity(id,lock);if(value==null||!value.status().equals("active"))throw new BusinessException("UNAUTHENTICATED","请重新登录",HttpStatus.UNAUTHORIZED);return value;}
    private void allowed(AccountLifecycleStore.Identity user,String purpose){
        if(!Set.of("user","visitor").contains(user.role()))throw new BusinessException("ACCOUNT_ACTION_FORBIDDEN","管理员和超级管理员不能注销、绑定或解绑学校邮箱",HttpStatus.FORBIDDEN);
        if(purpose.equals("unbind")&&user.schoolEmail()==null)throw new BusinessException("SCHOOL_EMAIL_NOT_BOUND","当前没有绑定学校邮箱",HttpStatus.CONFLICT);
    }
    public Summary summary(UserProfile actor){
        var user=active(actor.id(),false);Instant now=clock.instant();var proof=store.challenge(actor.id(),"delete");
        boolean permitted=Set.of("user","visitor").contains(user.role());
        Instant next=proof!=null&&proof.day().equals(day(now))?tomorrow(now):null;
        return new Summary(user.target(),user.schoolEmail(),permitted,permitted&&user.schoolEmail()!=null,next);
    }
    private void enabled(){if(!email.enabled()||email.secret()==null||email.secret().length()<32)throw unavailable();}
    public Sent send(UserProfile actor,String purpose,String ip){
        allowed(active(actor.id(),false),purpose);enabled();var sender=mail.getIfAvailable();if(sender==null)throw unavailable();
        limit(actor.id(),purpose+":send",ip);
        String code=String.format(Locale.ROOT,"%06d",random.nextInt(1_000_000));UUID nonce=UUID.randomUUID();Instant now=clock.instant(),expires=now.plus(Duration.ofMinutes(15));
        var user=tx.execute(status->{
            store.lockActor(actor.id());var current=active(actor.id(),true);allowed(current,purpose);var old=store.challenge(actor.id(),purpose);
            if(old!=null&&old.state().equals("reserved")&&now.isBefore(old.expiresAt()))throw new BusinessException("ACCOUNT_ACTION_IN_PROGRESS","验证码正在发送，请稍后检查邮箱",HttpStatus.CONFLICT);
            if(old!=null&&purpose.equals("delete")&&old.day().equals(day(now)))throw new BusinessException("DELETION_CODE_DAILY_LIMIT","注销验证码每天只能成功发送一次，请明天再试（UTC+8 00:00 重置）",HttpStatus.TOO_MANY_REQUESTS);
            if(old!=null&&purpose.equals("unbind")&&now.isBefore(old.reservedAt().plusSeconds(60)))throw new EmailRateLimitException(Math.max(1,Duration.between(now,old.reservedAt().plusSeconds(60)).toSeconds()));
            store.reserve(current,purpose,nonce,hash(actor.id(),purpose,current.target(),nonce,code),now,expires,day(now));return current;
        });
        boolean delivered=false;
        try {
            var message=new SimpleMailMessage();message.setFrom(email.from());message.setTo(user.target());
            message.setSubject(purpose.equals("delete")?"FoodtimeNeo 账号注销验证码":"FoodtimeNeo 学校邮箱解绑验证码");
            message.setText((purpose.equals("delete")?"您的账号注销":"您的学校邮箱解绑")+"验证码："+code+"\n验证码15分钟有效，请勿向他人透露。\n"+
                (purpose.equals("delete")?"注销立即生效、不可撤销，新账号不继承旧数据；邮箱冷却1小时后可重新注册。注销验证码每天只能发送一次。":"解绑后账号保留，学校邮箱认证移除，可用功能按平台当前权限策略执行；学校邮箱冷却1小时后可重新绑定。")+"\n如非本人操作，请勿提交验证码。");
            sender.send(message);delivered=true;
            Instant sentAt=clock.instant();
            boolean published=Boolean.TRUE.equals(tx.execute(status->{store.lockActor(actor.id());return store.publish(actor.id(),purpose,nonce,day(sentAt));}));
            if(!published)throw unavailable();
            return new Sent(expires,purpose.equals("delete")?tomorrow(sentAt):now.plusSeconds(60),user.target());
        }catch(RuntimeException error){
            LOG.warn("Account lifecycle mail failed: {}",error.getClass().getSimpleName());
            if(!delivered)tx.executeWithoutResult(status->{store.lockActor(actor.id());store.cancel(actor.id(),purpose,nonce);});
            throw unavailable();
        }
    }
    public Object confirm(UserProfile actor,String purpose,Code request,String ip){
        allowed(active(actor.id(),false),purpose);enabled();limit(actor.id(),purpose+":verify",ip);
        // Expensive password hashing occurs before locks and only for deletion.
        String replacement=purpose.equals("delete")?passwords.encode(UUID.randomUUID().toString()):null;
        Object outcome=tx.execute(status->{
            store.lockActor(actor.id());var snapshot=active(actor.id(),false);allowed(snapshot,purpose);
            cooldowns.lock(Arrays.asList(snapshot.email(),snapshot.schoolEmail()));
            var user=active(actor.id(),true);allowed(user,purpose);
            if(!Objects.equals(user.email(),snapshot.email())||!Objects.equals(user.schoolEmail(),snapshot.schoolEmail()))throw invalid();
            var proof=store.challenge(actor.id(),purpose);Instant now=clock.instant();
            if(proof==null||!proof.state().equals("sent")||!now.isBefore(proof.expiresAt())||proof.attempts()>=5||!proof.email().equals(user.target())||!proof.version().equals(user.version()))return null;
            if(!MessageDigest.isEqual(proof.hash().getBytes(StandardCharsets.US_ASCII),hash(actor.id(),purpose,user.target(),proof.nonce(),request.verificationCode()).getBytes(StandardCharsets.US_ASCII))){store.failedAttempt(actor.id(),purpose);return null;}
            Instant reusable=now.plus(Duration.ofHours(1));store.used(actor.id(),purpose);
            if(purpose.equals("unbind")){cooldowns.hold(user.schoolEmail(),reusable);store.unbind(actor.id());return new Unbound(profiles.current(actor.id()),reusable);}
            cooldowns.hold(user.email(),reusable);if(user.schoolEmail()!=null)cooldowns.hold(user.schoolEmail(),reusable);
            store.delete(user,replacement,now);return new Deleted(true,reusable);
        });
        // Invalid attempts commit their counter; throwing inside the transaction would undo it.
        if(outcome==null)throw invalid();return outcome;
    }
    private void limit(UUID id,String scope,String ip){Long retry=RedisAttemptLimiter.check(redis,auth.redisNamespace()+":account-lifecycle:"+scope,"account",id.toString(),ip,30,300,Duration.ofMinutes(15));if(retry==null)throw unavailable();if(retry>0)throw new EmailRateLimitException(retry);}
    private String hash(UUID user,String purpose,String address,UUID nonce,String code){try{Mac hmac=Mac.getInstance("HmacSHA256");hmac.init(new SecretKeySpec(email.secret().getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(hmac.doFinal((user+":"+purpose+":"+address+":"+nonce+":"+code).getBytes(StandardCharsets.UTF_8)));}catch(GeneralSecurityException error){throw new IllegalStateException(error);}}
    private static BusinessException invalid(){return new BusinessException("ACCOUNT_ACTION_CODE_INVALID","验证码错误、已使用或已过期，请核对当前账号与邮箱后重试",HttpStatus.BAD_REQUEST);}
    private static BusinessException unavailable(){return new BusinessException("ACCOUNT_ACTION_UNAVAILABLE","邮箱验证暂时不可用；若已提交操作，请重新读取账号状态确认结果",HttpStatus.SERVICE_UNAVAILABLE);}
}
