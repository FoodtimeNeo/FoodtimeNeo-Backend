package com.FoodtimeNeo.invitation.mapper;

import com.FoodtimeNeo.invitation.dto.InvitationCodeResponse;
import com.FoodtimeNeo.invitation.entity.InvitationCode;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
public class InvitationCodeStore {
    private final JdbcTemplate jdbc;
    private static final RowMapper<InvitationCodeResponse> ROW = (r,n) -> new InvitationCodeResponse(
            r.getObject("id",UUID.class),r.getString("label"),r.getString("status"),
            r.getObject("max_uses",Integer.class),r.getInt("used_count"),
            r.getTimestamp("expires_at") == null ? null : r.getTimestamp("expires_at").toInstant(),
            r.getTimestamp("created_at").toInstant());
    public InvitationCodeStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Returns 1 on insert, 0 on hash conflict (caller regenerates the code). */
    public int insert(UUID id,String hash,String label,Integer maxUses,Instant expiresAt,UUID createdBy) {
        return jdbc.update("""
                INSERT INTO foodtime.invitation_codes(id,code_hash,label,max_uses,expires_at,created_by_user_id)
                VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (code_hash) DO NOTHING
                """,id,hash,label,maxUses,
                expiresAt == null ? null : Timestamp.from(expiresAt),createdBy);
    }

    public InvitationCode lockByHash(String hash) {
        List<InvitationCode> locked = jdbc.query("""
                SELECT id,status,max_uses,used_count,expires_at FROM foodtime.invitation_codes
                WHERE code_hash = ? FOR UPDATE
                """,(r,n) -> new InvitationCode(r.getObject("id",UUID.class),r.getString("status"),
                        r.getObject("max_uses",Integer.class),r.getInt("used_count"),
                        r.getTimestamp("expires_at") == null ? null : r.getTimestamp("expires_at").toInstant()),hash);
        return locked.isEmpty() ? null : locked.get(0);
    }

    public void incrementUsed(UUID id) {
        jdbc.update("UPDATE foodtime.invitation_codes SET used_count = used_count + 1 WHERE id = ?",id);
    }

    public List<InvitationCodeResponse> page(String status,int size,long offset) {
        String filter = status == null ? "" : " WHERE status = ?";
        Object[] args = status == null ? new Object[]{size,offset} : new Object[]{status,size,offset};
        return jdbc.query("SELECT id,label,status,max_uses,used_count,expires_at,created_at" +
                " FROM foodtime.invitation_codes" + filter +
                " ORDER BY created_at DESC,id ASC LIMIT ? OFFSET ?",ROW,args);
    }

    public long count(String status) {
        String filter = status == null ? "" : " WHERE status = ?";
        Object[] args = status == null ? new Object[]{} : new Object[]{status};
        return jdbc.queryForObject("SELECT count(*) FROM foodtime.invitation_codes" + filter,Long.class,args);
    }

    /** Returns 0 when the code does not exist. */
    public int setStatus(UUID id,String status) {
        return jdbc.update("UPDATE foodtime.invitation_codes SET status = ? WHERE id = ?",status,id);
    }
}
