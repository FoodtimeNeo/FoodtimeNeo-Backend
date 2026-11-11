package com.FoodtimeNeo.notice;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.sql.Timestamp;
import java.time.Clock;

@Repository
public class NoticeStore {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public NoticeStore(JdbcTemplate jdbc,Clock clock) { this.jdbc = jdbc;this.clock=clock; }

    // Compatibility for installed clients that still send separate title/body fields.
    private String markdown(String title, String body) { return "# " + title.strip() + "\n\n" + body; }
    private NoticeDtos.NoticeResponse row(ResultSet r, int ignored) throws SQLException {
        String content = r.getString("content"), title = "系统公告", body = content;
        if (content.startsWith("# ")) {
            int end = content.indexOf('\n');
            title = end < 0 ? content.substring(2) : content.substring(2, end);
            body = end < 0 ? "" : content.substring(end).stripLeading();
        }
        title = NoticeHeading.title(content);
        if(title.isBlank()) title="系统公告";
        var end=r.getTimestamp("display_ends_at");
        Instant ends=end==null?null:end.toInstant();
        return new NoticeDtos.NoticeResponse(r.getObject("notice_id", UUID.class), title, body,
                r.getTimestamp("published_at").toInstant(), content,r.getTimestamp("created_time").toInstant(),
                r.getTimestamp("display_starts_at").toInstant(),ends,
                ends!=null&&r.getBoolean("deadline_date_only")?ends.minusNanos(1).atOffset(NoticeSchedule.ZONE).toLocalDate():null,
                r.getBoolean("is_update"),r.getBoolean("published"),r.getBoolean("enabled"),r.getLong("seq"),
                NoticeSchedule.status(new NoticeSchedule.Resolved(r.getTimestamp("published_at").toInstant(),
                        r.getTimestamp("display_starts_at").toInstant(),ends,r.getBoolean("deadline_date_only"),
                        r.getBoolean("is_update"),r.getBoolean("published"),r.getBoolean("enabled")),clock.instant()));
    }
    public UUID createContent(UUID actorId, String content) {
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.notices(notice_id,content,created_by) VALUES (?,?,?)",id,content,actorId);return id;
    }
    public int updateContent(UUID id,String content) {
        return jdbc.update("UPDATE foodtime.notices SET content=? WHERE notice_id=?",content,id);
    }
    public UUID configured() { return jdbc.queryForObject("SELECT notice_id FROM foodtime.config WHERE id=1",UUID.class); }
    public void configure(UUID id) {
        jdbc.update("UPDATE foodtime.config SET notice_id=? WHERE id=1",id);
        if(id==null)jdbc.update("UPDATE foodtime.notices SET enabled=false");
        else jdbc.update("UPDATE foodtime.notices SET published=true,enabled=true WHERE notice_id=?",id);
    }
    public void schedule(UUID id,NoticeSchedule.Resolved value) {
        jdbc.update("""
                UPDATE foodtime.notices SET published_at=?,display_starts_at=?,display_ends_at=?,deadline_date_only=?,
                    is_update=?,published=?,enabled=? WHERE notice_id=?
                """,Timestamp.from(value.publishedAt()),Timestamp.from(value.startsAt()),
                value.endsAt()==null?null:Timestamp.from(value.endsAt()),value.dateOnly(),value.isUpdate(),value.published(),value.enabled(),id);
        if(!value.enabled())jdbc.update("UPDATE foodtime.config SET notice_id=NULL WHERE id=1 AND notice_id=?",id);
    }
    public void announceDue(Instant now) {
        // Creation sequence is provisional for future/draft rows. Allocate their public cursor when due,
        // under one transaction lock, so a later-created immediate notice cannot make them disappear.
        var at=Timestamp.from(now);
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM foodtime.notices WHERE published AND published_at<=? AND NOT publication_announced)",Boolean.class,at)))return;
        lockPublications();
        var ids=jdbc.query("SELECT notice_id FROM foodtime.notices WHERE published AND published_at<=? AND NOT publication_announced ORDER BY published_at,seq",
                (r,i)->r.getObject(1,UUID.class),at);
        for(UUID id:ids)jdbc.update("UPDATE foodtime.notices SET seq=nextval(pg_get_serial_sequence('foodtime.notices','seq')),publication_announced=true WHERE notice_id=? AND NOT publication_announced",id);
    }
    public void lockPublications(){jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended('notice-publication',0))",r->{});}
    public com.FoodtimeNeo.common.api.PageResponse<NoticeDtos.NoticeResponse> search(
            String query, com.FoodtimeNeo.common.api.PageRequest page) {
        String needle=query.toLowerCase(java.util.Locale.ROOT);
        // Titles are derived, never persisted. Stream rows so a large archive does not retain every body.
        return jdbc.query(connection -> {
            var statement=connection.prepareStatement("SELECT * FROM foodtime.notices ORDER BY seq DESC");
            statement.setFetchSize(128);return statement;
        }, (org.springframework.jdbc.core.ResultSetExtractor<com.FoodtimeNeo.common.api.PageResponse<NoticeDtos.NoticeResponse>>) result -> {
            var items=new java.util.ArrayList<NoticeDtos.NoticeResponse>();long total=0;
            while(result.next()) {
                var item=row(result,0);
                if(!item.title().toLowerCase(java.util.Locale.ROOT).contains(needle)) continue;
                if(total>=page.offset()&&items.size()<page.size()) items.add(item);
                total++;
            }
            return com.FoodtimeNeo.common.api.PageResponse.of(items,total,page);
        });
    }
    public UUID create(UUID actorId, String title, String body) {
        UUID id = UUID.randomUUID();
        var now=Timestamp.from(clock.instant());
        // Use the same clock as eligibility checks; database and application clocks may differ.
        jdbc.update("INSERT INTO foodtime.notices(notice_id,content,created_by,published_at,display_starts_at) VALUES (?,?,?,?,?)",
                id, markdown(title, body), actorId,now,now);
        return id;
    }
    public NoticeDtos.NoticeResponse find(UUID id) {
        var rows = jdbc.query("SELECT * FROM foodtime.notices WHERE notice_id = ?",
                this::row, id);
        return rows.isEmpty() ? null : rows.get(0);
    }
    public List<NoticeDtos.NoticeResponse> page(int size, long offset) {
        return jdbc.query("SELECT * FROM foodtime.notices ORDER BY seq DESC LIMIT ? OFFSET ?",
                this::row, size, offset);
    }
    public long count() { return jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.notices", Long.class); }
    /** Deprecated: kept for older installed clients. New clients never poll this endpoint. */
    public List<NoticeDtos.NoticeResponse> feed(long sinceSeq, int limit,Instant now) {
        return jdbc.query("SELECT * FROM foodtime.notices WHERE published AND publication_announced AND published_at<=? AND seq>? ORDER BY seq ASC LIMIT ?",
                this::row,Timestamp.from(now), sinceSeq, limit);
    }
    public List<NoticeDtos.NoticeResponse> visiblePage(int size,long offset,Instant now) {
        return jdbc.query("SELECT * FROM foodtime.notices WHERE published AND published_at<=? ORDER BY published_at DESC,seq DESC LIMIT ? OFFSET ?",
                this::row,Timestamp.from(now),size,offset);
    }
    public long visibleCount(Instant now) {
        return jdbc.queryForObject("SELECT count(*) FROM foodtime.notices WHERE published AND published_at<=?",Long.class,Timestamp.from(now));
    }
    public long unreadCount(UUID userId,Instant now) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.notices n WHERE published AND published_at<=? AND NOT EXISTS"
                + " (SELECT 1 FROM foodtime.notice_reads r WHERE r.notice_id = n.notice_id AND r.user_id = ?)",
                Long.class,Timestamp.from(now), userId);
    }
    public boolean visible(UUID id,Instant now) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM foodtime.notices WHERE notice_id=? AND published AND published_at<=?)",Boolean.class,id,Timestamp.from(now)));
    }
    public boolean exists(UUID id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM foodtime.notices WHERE notice_id = ?)", Boolean.class, id));
    }
    public void markRead(UUID noticeId, UUID userId) {
        jdbc.update("INSERT INTO foodtime.notice_reads(notice_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                noticeId, userId);
    }
    public int update(UUID id, String title, String body) {
        return jdbc.update("UPDATE foodtime.notices SET content = ? WHERE notice_id = ?", markdown(title, body), id);
    }
    public int delete(UUID id) {
        return jdbc.update("DELETE FROM foodtime.notices WHERE notice_id = ?", id);
    }
    public List<NoticeDtos.CurrentNoticeResponse> currentSet(UUID userId,Instant now) {
        var rows = jdbc.query("""
                WITH ranked AS (
                    SELECT n.*,row_number() OVER (PARTITION BY is_update ORDER BY published_at DESC,seq DESC) AS rank
                    FROM foodtime.notices n WHERE published AND published_at<=? AND display_starts_at<=?
                      AND (display_ends_at IS NULL OR display_ends_at>?)
                      AND (enabled OR notice_id=(SELECT notice_id FROM foodtime.config WHERE id=1))
                )
                SELECT n.*, r.user_id IS NOT NULL AS is_read,
                       COALESCE(r.dont_show_again, FALSE) AS dont_show_again
                FROM ranked n
                LEFT JOIN foodtime.notice_reads r ON r.notice_id = n.notice_id AND r.user_id = ?
                WHERE n.rank=1 AND NOT COALESCE(r.dont_show_again, FALSE) ORDER BY n.is_update DESC
                """, (r, i) -> new NoticeDtos.CurrentNoticeResponse(r.getObject("notice_id", UUID.class),
                r.getString("content"), r.getTimestamp("created_time").toInstant(),
                r.getBoolean("is_read"), r.getBoolean("dont_show_again"),r.getTimestamp("published_at").toInstant(),
                r.getTimestamp("display_ends_at")==null?null:r.getTimestamp("display_ends_at").toInstant(),r.getBoolean("is_update")),
                Timestamp.from(now),Timestamp.from(now),Timestamp.from(now),userId);
        return rows;
    }
    public boolean confirm(UUID noticeId, UUID userId, boolean dontShowAgain) {
        // A late unchecked confirmation from another client must not clear a saved preference.
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                INSERT INTO foodtime.notice_reads(notice_id, user_id, dont_show_again) VALUES (?, ?, ?)
                ON CONFLICT (notice_id, user_id) DO UPDATE
                SET dont_show_again = foodtime.notice_reads.dont_show_again OR EXCLUDED.dont_show_again
                RETURNING dont_show_again
                """, Boolean.class, noticeId, userId, dontShowAgain));
    }
}
