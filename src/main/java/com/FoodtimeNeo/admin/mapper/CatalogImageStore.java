package com.FoodtimeNeo.admin.mapper;

import com.FoodtimeNeo.admin.dto.CatalogImageResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class CatalogImageStore {
    private final JdbcTemplate jdbc;
    public CatalogImageStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private String table(String type) {
        return switch (type) {
            case "dining-halls" -> "foodtime.dining_halls";
            case "stalls" -> "foodtime.stalls";
            case "dishes" -> "foodtime.dishes";
            default -> throw new IllegalArgumentException("Invalid catalog type");
        };
    }

    public boolean lock(String type, UUID parent) {
        return !jdbc.query("SELECT id FROM " + table(type) + " WHERE id=? FOR UPDATE",
                (r,n) -> r.getObject(1, UUID.class), parent).isEmpty();
    }

    private String select(String type) {
        return switch (type) {
            case "dishes" -> "SELECT id,dish_id AS parent_id,image_url,0 AS position,created_at FROM foodtime.dish_images WHERE status='approved'";
            case "dining-halls" -> "SELECT id,id AS parent_id,cover_image_url AS image_url,0 AS position,created_at FROM foodtime.dining_halls WHERE cover_image_url IS NOT NULL AND cover_image_url<>''";
            case "stalls" -> """
                    SELECT id,stall_id AS parent_id,image_url,sort_order AS position,created_at FROM foodtime.stall_images
                    UNION ALL
                    SELECT s.id,s.id,s.cover_image_url,-2147483648,s.created_at FROM foodtime.stalls s
                    WHERE s.cover_image_url IS NOT NULL AND s.cover_image_url<>''
                    AND NOT EXISTS(SELECT 1 FROM foodtime.stall_images i WHERE i.stall_id=s.id AND i.image_url=s.cover_image_url)
                    """;
            default -> throw new IllegalArgumentException("Invalid catalog type");
        };
    }

    public List<CatalogImageResponse> page(String type, UUID parent, int size, long offset) {
        return jdbc.query("SELECT id,image_url FROM (" + select(type) + ") images WHERE parent_id=? ORDER BY position,created_at DESC,id LIMIT ? OFFSET ?",
                (r,n) -> new CatalogImageResponse(r.getObject("id",UUID.class),r.getString("image_url")), parent,size,offset);
    }
    public long count(String type, UUID parent) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM (" + select(type) + ") images WHERE parent_id=?",Long.class,parent);
    }
    public CatalogImageResponse find(String type, UUID parent, UUID imageId) {
        var rows = jdbc.query("SELECT id,image_url FROM (" + select(type) + ") images WHERE parent_id=? AND id=?",
                (r,n) -> new CatalogImageResponse(r.getObject("id",UUID.class),r.getString("image_url")),parent,imageId);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public CatalogImageResponse add(String type, UUID parent, UUID actor, String url) {
        var previous = jdbc.query("SELECT id FROM (" + select(type) + ") images WHERE parent_id=? AND image_url=?",
                (r,n) -> r.getObject(1,UUID.class),parent,url);
        if (!previous.isEmpty()) return new CatalogImageResponse(previous.getFirst(),url);
        UUID id = UUID.randomUUID();
        if (type.equals("dining-halls")) {
            jdbc.update("UPDATE foodtime.dining_halls SET cover_image_url=? WHERE id=?",url,parent);
            id = parent;
        } else if (type.equals("stalls")) {
            jdbc.update("INSERT INTO foodtime.stall_images(id,stall_id,image_url,sort_order) VALUES (?,?,?,COALESCE((SELECT MAX(sort_order)::bigint+1 FROM foodtime.stall_images WHERE stall_id=?),0))",id,parent,url,parent);
            jdbc.update("UPDATE foodtime.stalls SET cover_image_url=? WHERE id=? AND (cover_image_url IS NULL OR cover_image_url='')",url,parent);
        } else {
            jdbc.update("INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status,reviewed_by_user_id,reviewed_at,review_reason) VALUES (?,?,?,?,'approved',?,now(),'平台管理员直接添加')",id,parent,actor,url,actor);
        }
        return new CatalogImageResponse(id,url);
    }
    public void remove(String type, UUID parent, UUID actor, CatalogImageResponse image) {
        if (type.equals("dishes")) {
            jdbc.update("UPDATE foodtime.dish_images SET status='rejected',reviewed_by_user_id=?,reviewed_at=now(),review_reason='平台管理员移除图片' WHERE dish_id=? AND image_url=? AND status='approved'",actor,parent,image.imageUrl());
        } else if (type.equals("dining-halls")) {
            jdbc.update("UPDATE foodtime.dining_halls SET cover_image_url=NULL WHERE id=? AND cover_image_url=?",parent,image.imageUrl());
        } else {
            jdbc.update("DELETE FROM foodtime.stall_images WHERE stall_id=? AND image_url=?",parent,image.imageUrl());
            jdbc.update("UPDATE foodtime.stalls SET cover_image_url=(SELECT image_url FROM foodtime.stall_images WHERE stall_id=? ORDER BY sort_order,created_at,id LIMIT 1) WHERE id=? AND cover_image_url=?",parent,parent,image.imageUrl());
        }
    }
}
