package com.FoodtimeNeo.user.lifecycle;

import com.FoodtimeNeo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

/** Address locks are shared by registration, binding, unbinding and deletion transactions. */
@Service
public class EmailCooldowns {
    private final JdbcTemplate jdbc;private final Clock clock;
    public EmailCooldowns(JdbcTemplate jdbc,Clock clock){this.jdbc=jdbc;this.clock=clock;}
    public static String key(String email){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(email.strip().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException error){throw new IllegalStateException(error);}}
    public void lock(Collection<String> emails){emails.stream().filter(Objects::nonNull).filter(s->!s.isBlank()).map(EmailCooldowns::key).distinct().sorted().forEach(key->jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",r->{},"email-lifecycle:"+key));}
    @Transactional
    public void requireAvailable(String email){
        lock(List.of(email));var now=Timestamp.from(clock.instant());
        jdbc.update("DELETE FROM foodtime.email_cooldowns WHERE email_hash=? AND available_at<=?",key(email),now);
        var rows=jdbc.query("SELECT available_at FROM foodtime.email_cooldowns WHERE email_hash=? AND available_at>?",(r,i)->r.getTimestamp(1).toInstant(),key(email),now);
        if(!rows.isEmpty())throw new BusinessException("EMAIL_COOLING_DOWN","该邮箱正在注销或解绑后的 1 小时冷却期，请稍后再注册或绑定",HttpStatus.CONFLICT);
    }
    public void hold(String email,Instant until){jdbc.update("INSERT INTO foodtime.email_cooldowns(email_hash,available_at) VALUES (?,?) ON CONFLICT(email_hash) DO UPDATE SET available_at=GREATEST(foodtime.email_cooldowns.available_at,EXCLUDED.available_at)",key(email),Timestamp.from(until));}
}
