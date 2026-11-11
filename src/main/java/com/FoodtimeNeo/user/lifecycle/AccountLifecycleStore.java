package com.FoodtimeNeo.user.lifecycle;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@Repository
public class AccountLifecycleStore {
    private final JdbcTemplate jdbc;
    public AccountLifecycleStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public record Identity(UUID id,String email,String schoolEmail,String role,String status,Instant version){String target(){return schoolEmail==null?email:schoolEmail;}}
    public record Challenge(String email,Instant version,UUID nonce,String hash,String state,LocalDate day,Instant reservedAt,Instant expiresAt,int attempts){}
    public void lockActor(UUID id){jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",r->{},"account-lifecycle:"+id);}
    public Identity identity(UUID id,boolean lock){var rows=jdbc.query("SELECT id,email,school_email,role,status,password_changed_at FROM foodtime.users WHERE id=?"+(lock?" FOR UPDATE":""),(r,i)->new Identity(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getTimestamp(6).toInstant()),id);return rows.isEmpty()?null:rows.getFirst();}
    public Challenge challenge(UUID id,String purpose){var rows=jdbc.query("SELECT * FROM foodtime.account_email_actions WHERE user_id=? AND purpose=?",(r,i)->new Challenge(r.getString("email"),r.getTimestamp("password_version").toInstant(),r.getObject("nonce",UUID.class),r.getString("code_hash"),r.getString("state"),r.getDate("send_day").toLocalDate(),r.getTimestamp("reserved_at").toInstant(),r.getTimestamp("expires_at").toInstant(),r.getInt("attempts")),id,purpose);return rows.isEmpty()?null:rows.getFirst();}
    public void reserve(Identity user,String purpose,UUID nonce,String hash,Instant now,Instant expiry,LocalDate day){jdbc.update("""
        INSERT INTO foodtime.account_email_actions(user_id,purpose,email,password_version,nonce,code_hash,state,send_day,reserved_at,expires_at)
        VALUES (?,?,?,?,?,?,'reserved',?,?,?) ON CONFLICT(user_id,purpose) DO UPDATE SET email=EXCLUDED.email,
        password_version=EXCLUDED.password_version,nonce=EXCLUDED.nonce,code_hash=EXCLUDED.code_hash,state='reserved',
        send_day=EXCLUDED.send_day,reserved_at=EXCLUDED.reserved_at,expires_at=EXCLUDED.expires_at,attempts=0
        """,user.id(),purpose,user.target(),Timestamp.from(user.version()),nonce,hash,day,Timestamp.from(now),Timestamp.from(expiry));}
    public boolean publish(UUID id,String purpose,UUID nonce,LocalDate sentDay){return jdbc.update("UPDATE foodtime.account_email_actions SET state='sent',send_day=? WHERE user_id=? AND purpose=? AND nonce=? AND state='reserved'",sentDay,id,purpose,nonce)==1;}
    public void cancel(UUID id,String purpose,UUID nonce){jdbc.update("DELETE FROM foodtime.account_email_actions WHERE user_id=? AND purpose=? AND nonce=? AND state='reserved'",id,purpose,nonce);}
    public void failedAttempt(UUID id,String purpose){jdbc.update("UPDATE foodtime.account_email_actions SET attempts=LEAST(5,attempts+1) WHERE user_id=? AND purpose=?",id,purpose);}
    public void used(UUID id,String purpose){jdbc.update("UPDATE foodtime.account_email_actions SET state='used',email='',code_hash=repeat('0',64) WHERE user_id=? AND purpose=?",id,purpose);}
    public void unbind(UUID id){jdbc.update("UPDATE foodtime.users SET school_email=NULL,school_email_verified_at=NULL,role='visitor' WHERE id=? AND role IN ('visitor','user') AND status='active'",id);}
    public void delete(Identity user,String password,Instant now){
        jdbc.update("""
            UPDATE foodtime.users SET email=?,school_email=NULL,school_email_verified_at=NULL,email_verified_at=NULL,
            display_name='已注销用户',avatar_url=NULL,role='visitor',status='deleted',deleted_at=?,password_hash=?,
            password_changed_at=GREATEST(?,password_changed_at+interval '1 microsecond'),last_login_at=NULL,invited_by_code_id=NULL
            WHERE id=? AND status='active' AND role IN ('visitor','user')
            ""","deleted-"+user.id()+"@deleted.invalid",Timestamp.from(now),password,Timestamp.from(now),user.id());
        // Public contributions and the other participant's messages retain the anonymous identity.
        jdbc.update("DELETE FROM foodtime.user_follows WHERE follower_id=? OR following_id=?",user.id(),user.id());
        jdbc.update("DELETE FROM foodtime.user_blocks WHERE blocker_id=? OR blocked_id=?",user.id(),user.id());
        jdbc.update("DELETE FROM foodtime.community_favorites WHERE user_id=?",user.id());
        jdbc.update("DELETE FROM foodtime.dish_favorites WHERE user_id=?",user.id());
        jdbc.update("UPDATE foodtime.account_email_actions SET state='used',email='',code_hash=repeat('0',64) WHERE user_id=?",user.id());
    }
}
