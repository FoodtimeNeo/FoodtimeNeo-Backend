package com.FoodtimeNeo.dashboard;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 看板读模型：全部一次性聚合查询，不写库。
 * 趋势口径见 docs/admin-dashboard.md；日期一律按 Asia/Shanghai 业务时区切分。
 */
@Repository
public class DashboardStore {
    /** 趋势聚合的事件源：表、时间列、新增计数列名（计数列仅 new 系列使用）。 */
    private record EventSource(String table, String tsColumn, String countColumn) {
    }

    private static final List<EventSource> NEW_EVENTS = List.of(
            new EventSource("foodtime.users", "created_at", "newUsers"),
            new EventSource("foodtime.dish_review_submissions", "submitted_at", "newReviews"),
            new EventSource("foodtime.dish_review_replies", "created_at", "newReplies"),
            new EventSource("foodtime.community_posts", "created_at", "newPosts"));

    private static final List<String> ACTIVITY_TABLES = List.of(
            // 表名:时间列:用户列（users 主键叫 id，其余叫 user_id）
            "foodtime.users:created_at:id",
            "foodtime.dish_review_submissions:submitted_at:user_id",
            "foodtime.dish_review_replies:created_at:user_id",
            "foodtime.community_posts:created_at:user_id",
            "foodtime.community_comments:created_at:user_id",
            "foodtime.community_post_likes:created_at:user_id",
            "foodtime.community_comment_likes:created_at:user_id",
            "foodtime.dish_review_likes:created_at:user_id",
            "foodtime.dish_review_reply_likes:created_at:user_id");

    private final JdbcTemplate jdbc;

    public DashboardStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Totals(long users, long reviews, long replies, long posts, long halls, long stalls, long dishes) {
    }

    public Totals totals() {
        return jdbc.queryForObject("""
                SELECT (SELECT COUNT(*) FROM foodtime.users WHERE status = 'active') AS users,
                       (SELECT COUNT(*) FROM foodtime.dish_reviews) AS reviews,
                       (SELECT COUNT(*) FROM foodtime.dish_review_replies) AS replies,
                       (SELECT COUNT(*) FROM foodtime.community_posts WHERE status = 'approved' AND deleted_at IS NULL) AS posts,
                       (SELECT COUNT(*) FROM foodtime.dining_halls WHERE status = 'active') AS halls,
                       (SELECT COUNT(*) FROM foodtime.stalls WHERE status = 'active') AS stalls,
                       (SELECT COUNT(*) FROM foodtime.dishes WHERE status = 'active') AS dishes
                """, (r, i) -> new Totals(r.getLong("users"), r.getLong("reviews"), r.getLong("replies"),
                        r.getLong("posts"), r.getLong("halls"), r.getLong("stalls"), r.getLong("dishes")));
    }

    /**
     * 返回 [fromInclusive, toExclusive) 区间内每天的新增计数。
     * key 为 ISO 日期字符串；无行的日期不出现在结果中，由调用方补零。
     */
    public Map<String, Map<String, Long>> dailyNewCounts(Instant fromInclusive, Instant toExclusive) {
        StringBuilder sql = new StringBuilder("SELECT day, ");
        for (int i = 0; i < NEW_EVENTS.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("SUM(").append(NEW_EVENTS.get(i).countColumn()).append(") AS ")
                    .append(NEW_EVENTS.get(i).countColumn());
        }
        sql.append(" FROM (");
        for (int i = 0; i < NEW_EVENTS.size(); i++) {
            EventSource e = NEW_EVENTS.get(i);
            if (i > 0) {
                sql.append(" UNION ALL ");
            }
            sql.append("SELECT (").append(e.tsColumn()).append(" AT TIME ZONE 'Asia/Shanghai')::date AS day");
            for (int j = 0; j < NEW_EVENTS.size(); j++) {
                sql.append(j == i ? ", 1 AS " : ", 0 AS ").append(NEW_EVENTS.get(j).countColumn());
            }
            sql.append(" FROM ").append(e.table()).append(" WHERE ").append(e.tsColumn()).append(" >= ? AND ")
                    .append(e.tsColumn()).append(" < ?");
        }
        sql.append(") a GROUP BY day");
        Object[] args = new Object[NEW_EVENTS.size() * 2];
        for (int i = 0; i < NEW_EVENTS.size(); i++) {
            args[i * 2] = Timestamp.from(fromInclusive);
            args[i * 2 + 1] = Timestamp.from(toExclusive);
        }
        return jdbc.query(sql.toString(), args, rs -> {
            Map<String, Map<String, Long>> out = new LinkedHashMap<>();
            while (rs.next()) {
                String day = rs.getDate("day").toLocalDate().toString();
                Map<String, Long> row = new LinkedHashMap<>();
                for (EventSource e : NEW_EVENTS) {
                    row.put(e.countColumn(), rs.getLong(e.countColumn()));
                }
                out.put(day, row);
            }
            return out;
        });
    }

    /**
     * 返回 [fromInclusive, toExclusive) 区间内每天的去重活跃用户数。
     * 活跃 = 当天在任一事件源中出现过 user_id（注册/投稿/回复/动态/评论/点赞）。
     * 无行的日期不在结果中，由调用方补零。
     */
    public Map<String, Long> dailyActiveUsers(Instant fromInclusive, Instant toExclusive) {
        StringBuilder sql = new StringBuilder("SELECT day, COUNT(DISTINCT user_id) AS peak FROM (");
        for (int i = 0; i < ACTIVITY_TABLES.size(); i++) {
            String[] parts = ACTIVITY_TABLES.get(i).split(":");
            if (i > 0) {
                sql.append(" UNION ALL ");
            }
            sql.append("SELECT (").append(parts[1]).append(" AT TIME ZONE 'Asia/Shanghai')::date AS day, ")
                    .append(parts[2]).append(" AS user_id FROM ").append(parts[0])
                    .append(" WHERE ").append(parts[1]).append(" >= ? AND ").append(parts[1]).append(" < ?");
        }
        sql.append(") a GROUP BY day");
        Object[] args = new Object[ACTIVITY_TABLES.size() * 2];
        for (int i = 0; i < ACTIVITY_TABLES.size(); i++) {
            args[i * 2] = Timestamp.from(fromInclusive);
            args[i * 2 + 1] = Timestamp.from(toExclusive);
        }
        return jdbc.query(sql.toString(), args, (ResultSet rs) -> {
            Map<String, Long> out = new LinkedHashMap<>();
            while (rs.next()) {
                out.put(rs.getDate("day").toLocalDate().toString(), rs.getLong("peak"));
            }
            return out;
        });
    }
}
