package com.FoodtimeNeo.feedback;

import com.FoodtimeNeo.common.api.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.time.Instant;

@Repository
public class FeedbackStore {
    private final JdbcTemplate jdbc;
    public FeedbackStore(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    private static final String SELECT="""
            SELECT f.*, u.display_name,
              (SELECT r.content FROM foodtime.feedback_replies r WHERE r.feedback_id=f.id
               ORDER BY r.created_at DESC,r.id DESC LIMIT 1) AS latest_reply
            FROM foodtime.user_feedback f JOIN foodtime.users u ON u.id=f.user_id
            """;
    private FeedbackDtos.Row row(ResultSet r,int ignored)throws SQLException {
        return new FeedbackDtos.Row(r.getObject("id",UUID.class),r.getString("type"),r.getString("content"),
                r.getString("status"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant(),
                r.getString("display_name"),r.getString("latest_reply"));
    }
    public void lockSubmit(UUID owner,String device) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",r->{},"feedback-device:"+device);
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",r->{},"feedback-owner:"+owner);
    }
    public FeedbackDtos.Row requested(UUID owner,UUID request) {
        var rows=jdbc.query(SELECT+" WHERE f.user_id=? AND f.client_request_id=?",this::row,owner,request);
        return rows.isEmpty()?null:rows.getFirst();
    }
    public long ownerCount(UUID owner,Instant start,Instant end) {
        return jdbc.queryForObject("SELECT count(*) FROM foodtime.user_feedback WHERE user_id=? AND created_at>=? AND created_at<?",
                Long.class,owner,java.sql.Timestamp.from(start),java.sql.Timestamp.from(end));
    }
    public long deviceCount(String device,Instant start,Instant end) {
        return jdbc.queryForObject("SELECT count(*) FROM foodtime.user_feedback WHERE device_key=? AND created_at>=? AND created_at<?",
                Long.class,device,java.sql.Timestamp.from(start),java.sql.Timestamp.from(end));
    }
    public FeedbackDtos.Row create(UUID owner,FeedbackDtos.Create input,String content,String device,boolean visitor,Instant now) {
        UUID id=UUID.randomUUID();
        jdbc.update("""
                INSERT INTO foodtime.user_feedback(id,user_id,client_request_id,type,content,device_key,submitted_as_visitor,created_at,updated_at)
                VALUES (?,?,?,?,?,?,?,?,?) ON CONFLICT (user_id,client_request_id) DO NOTHING
                """,id,owner,input.clientRequestId(),input.type(),content,device,visitor,java.sql.Timestamp.from(now),java.sql.Timestamp.from(now));
        return jdbc.queryForObject(SELECT+" WHERE f.user_id=? AND f.client_request_id=?",this::row,owner,input.clientRequestId());
    }
    public FeedbackDtos.Row find(UUID id,UUID owner) {
        var rows=jdbc.query(SELECT+" WHERE f.id=?"+(owner==null?"":" AND f.user_id=?"),this::row,
                owner==null?new Object[]{id}:new Object[]{id,owner});
        return rows.isEmpty()?null:rows.getFirst();
    }
    public List<FeedbackDtos.Row> page(UUID owner,boolean archived,PageRequest page) {
        String condition=(archived?"f.status='completed'":"f.status<>'completed'")+(owner==null?"":" AND f.user_id=?");
        return jdbc.query(SELECT+" WHERE "+condition+" ORDER BY f.created_at DESC,f.id LIMIT ? OFFSET ?",this::row,
                owner==null?new Object[]{page.size(),page.offset()}:new Object[]{owner,page.size(),page.offset()});
    }
    public long count(UUID owner,boolean archived) {
        return jdbc.queryForObject("SELECT count(*) FROM foodtime.user_feedback WHERE "+
                (archived?"status='completed'":"status<>'completed'")+(owner==null?"":" AND user_id=?"),Long.class,
                owner==null?new Object[]{}:new Object[]{owner});
    }
    public List<FeedbackDtos.Reply> replies(UUID id,PageRequest page) {
        return jdbc.query("""
                SELECT id,content,created_at FROM foodtime.feedback_replies WHERE feedback_id=?
                ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?
                """,(r,i)->new FeedbackDtos.Reply(r.getObject("id",UUID.class),r.getString("content"),
                r.getTimestamp("created_at").toInstant()),id,page.size(),page.offset());
    }
    public long replyCount(UUID id) {
        return jdbc.queryForObject("SELECT count(*) FROM foodtime.feedback_replies WHERE feedback_id=?",Long.class,id);
    }
    public boolean lock(UUID id) {
        return !jdbc.query("SELECT id FROM foodtime.user_feedback WHERE id=? FOR UPDATE",(r,i)->r.getObject(1),id).isEmpty();
    }
    public boolean action(UUID id,String key) {
        return jdbc.update("INSERT INTO foodtime.feedback_actions(feedback_id,action_key) VALUES (?,?) ON CONFLICT DO NOTHING",id,key)==1;
    }
    public boolean hasAction(UUID id,String key) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM foodtime.feedback_actions WHERE feedback_id=? AND action_key=?)",
                Boolean.class,id,key));
    }
    public void update(UUID id,UUID actor,String reply,String status) {
        if(!reply.isBlank())jdbc.update("INSERT INTO foodtime.feedback_replies(id,feedback_id,replied_by,content) VALUES (?,?,?,?)",
                UUID.randomUUID(),id,actor,reply);
        jdbc.update("UPDATE foodtime.user_feedback SET status=?,updated_at=now() WHERE id=?",status,id);
    }
    public List<FeedbackDtos.Row> exportRows() {
        // Serialize export and import on the same rows; a completed feedback cannot re-enter this batch.
        return jdbc.query(SELECT+" WHERE f.status<>'completed' ORDER BY f.id FOR UPDATE OF f",this::row);
    }
    public void markExported(UUID id) {
        jdbc.update("UPDATE foodtime.user_feedback SET status='processing',exported_at=coalesce(exported_at,now()),updated_at=now() WHERE id=? AND status<>'completed'",id);
    }
}
