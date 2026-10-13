package com.FoodtimeNeo.media.mapper;
import com.FoodtimeNeo.media.dto.ImageSubmissionResponse;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class ImageSubmissionStore {
    private final JdbcTemplate jdbc;
    private static final String SELECT = """
            SELECT i.*,d.name AS dish_name,s.id AS stall_id,s.name AS stall_name,h.id AS hall_id,h.name AS hall_name
            FROM foodtime.dish_images i JOIN foodtime.dishes d ON d.id=i.dish_id
            JOIN foodtime.stalls s ON s.id=d.stall_id JOIN foodtime.dining_halls h ON h.id=s.dining_hall_id
            """;
    private static final RowMapper<ImageSubmissionResponse> ROW = (r,n) -> new ImageSubmissionResponse(
            r.getObject("id",UUID.class),r.getObject("dish_id",UUID.class),r.getString("dish_name"),r.getObject("stall_id",UUID.class),
            r.getString("stall_name"),r.getObject("hall_id",UUID.class),r.getString("hall_name"),r.getString("image_url"),r.getString("status"),
            r.getTimestamp("reviewed_at") == null ? null : r.getTimestamp("reviewed_at").toInstant(),r.getString("review_reason"),r.getTimestamp("created_at").toInstant());
    public ImageSubmissionStore(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public ImageSubmissionResponse create(UUID dish,UUID owner,String image) {
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url) VALUES (?, ?, ?, ?)",id,dish,owner,image);
        return jdbc.queryForObject(SELECT+" WHERE i.id=?",ROW,id);
    }
    public List<ImageSubmissionResponse> page(UUID owner,int size,long offset) {
        return jdbc.query(SELECT+" WHERE i.uploaded_by_user_id=? ORDER BY i.created_at DESC,i.id ASC LIMIT ? OFFSET ?",ROW,owner,size,offset);
    }
    public long count(UUID owner) { return jdbc.queryForObject("SELECT count(*) FROM foodtime.dish_images WHERE uploaded_by_user_id=?",Long.class,owner); }
}
