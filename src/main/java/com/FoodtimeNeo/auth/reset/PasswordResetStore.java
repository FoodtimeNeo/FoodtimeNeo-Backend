package com.FoodtimeNeo.auth.reset;

import com.FoodtimeNeo.config.properties.EmailVerificationProperties;
import com.FoodtimeNeo.config.properties.PasswordResetProperties;
import com.FoodtimeNeo.user.entity.LoginAccount;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

@Component
public class PasswordResetStore {
    private static final DefaultRedisScript<Long> RESERVE=new DefaultRedisScript<>();
    static { RESERVE.setLocation(new ClassPathResource("redis/email/reserve-send.lua")); RESERVE.setResultType(Long.class); }
    private static final DefaultRedisScript<Long> PUBLISH=new DefaultRedisScript<>("""
        if redis.call('GET',KEYS[2])~=ARGV[1] then return 0 end
        redis.call('DEL',KEYS[1])
        redis.call('HSET',KEYS[1],'proof',ARGV[2],'userId',ARGV[3],'version',ARGV[4],'attempts','0')
        redis.call('EXPIRE',KEYS[1],ARGV[5])
        return 1
        """,Long.class);
    private static final DefaultRedisScript<Long> VERIFY=new DefaultRedisScript<>("""
        if redis.call('EXISTS',KEYS[1])==0 then return 0 end
        if redis.call('HGET',KEYS[1],'proof')~=ARGV[1] or redis.call('HGET',KEYS[1],'userId')~=ARGV[2]
          or redis.call('HGET',KEYS[1],'version')~=ARGV[3] then
          if redis.call('HINCRBY',KEYS[1],'attempts',1)>=5 then redis.call('DEL',KEYS[1]) end
          return 0
        end
        if redis.call('EXISTS',KEYS[2])~=0 then return 0 end
        redis.call('HSET',KEYS[2],'userId',ARGV[2],'version',ARGV[3],'purpose','password_reset')
        redis.call('EXPIRE',KEYS[2],ARGV[4])
        redis.call('DEL',KEYS[1])
        return 1
        """,Long.class);
    private static final DefaultRedisScript<List> CONSUME=new DefaultRedisScript<>("""
        if redis.call('HGET',KEYS[1],'purpose')~='password_reset' then return {} end
        local id=redis.call('HGET',KEYS[1],'userId')
        local version=redis.call('HGET',KEYS[1],'version')
        if not id or not version then return {} end
        redis.call('DEL',KEYS[1])
        return {id,version}
        """,List.class);
    private final StringRedisTemplate redis;
    private final PasswordResetProperties properties;
    private final EmailVerificationProperties email;
    public PasswordResetStore(StringRedisTemplate redis,PasswordResetProperties properties,EmailVerificationProperties email) {
        this.redis=redis;this.properties=properties;this.email=email;
    }
    public long reserve(String address,String ip,String device,String reservation) {
        Long value=redis.execute(RESERVE,List.of(key("code",address),key("cooldown",address),key("account",address),
            key("device",device==null||device.isBlank()?"unidentified":device),key("ip",ip)),reservation,
            Long.toString(properties.resendInterval().toSeconds()),"3600",Integer.toString(properties.sendAccountLimit()),
            Integer.toString(properties.sendDeviceLimit()),Integer.toString(properties.sendIpLimit()));
        if(value==null)throw PasswordResetService.unavailable();return value;
    }
    public boolean publish(LoginAccount account,String reservation,String code) {
        return Long.valueOf(1).equals(redis.execute(PUBLISH,List.of(key("code",account.email()),key("cooldown",account.email())),
            reservation,proof(account.email(),code),account.id().toString(),account.passwordChangedAt().toString(),Long.toString(properties.codeTtl().toSeconds())));
    }
    public boolean verify(LoginAccount account,String code,String token) {
        return Long.valueOf(1).equals(redis.execute(VERIFY,List.of(key("code",account.email()),key("grant",token)),
            proof(account.email(),code),account.id().toString(),account.passwordChangedAt().toString(),Long.toString(properties.tokenTtl().toSeconds())));
    }
    public Grant consume(String token) {
        List<?> value=redis.execute(CONSUME,List.of(key("grant",token)));
        if(value==null)throw PasswordResetService.unavailable();
        if(value.size()!=2)throw PasswordResetService.invalidGrant();
        return new Grant(UUID.fromString(value.get(0).toString()),value.get(1).toString());
    }
    public String key(String kind,String value) {
        try { return properties.redisNamespace()+":{password-reset}:"+kind+":"+HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(GeneralSecurityException e){throw new IllegalStateException("SHA-256 required",e);}
    }
    private String proof(String address,String code) {
        try { Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(email.secret().getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(("password_reset:"+address+":"+code).getBytes(StandardCharsets.UTF_8))); }
        catch(GeneralSecurityException e){throw new IllegalStateException("HMAC required",e);}
    }
    public record Grant(UUID userId,String version) { }
}
