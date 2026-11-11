package com.FoodtimeNeo.community.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public class CommunityViewStore {
    private final JdbcTemplate jdbc;
    public CommunityViewStore(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public record Target(UUID author,String status,long count) { }
    public Target lock(UUID id) {
        var rows=jdbc.query("SELECT user_id,status,view_count FROM foodtime.community_posts WHERE id=? AND deleted_at IS NULL FOR UPDATE",
                (r,n) -> new Target(r.getObject("user_id",UUID.class),r.getString("status"),r.getLong("view_count")),id);
        return rows.isEmpty()?null:rows.getFirst();
    }
    public long record(UUID post,UUID viewer,UUID requestId,long currentCount) {
        int inserted=jdbc.update("INSERT INTO foodtime.community_post_views(post_id,viewer_id,client_request_id) VALUES (?,?,?) ON CONFLICT DO NOTHING",post,viewer,requestId);
        if(inserted==0) return currentCount;
        return jdbc.queryForObject("UPDATE foodtime.community_posts SET view_count=view_count+1 WHERE id=? RETURNING view_count",Long.class,post);
    }
}
