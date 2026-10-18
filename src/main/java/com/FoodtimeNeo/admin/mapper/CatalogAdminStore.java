package com.FoodtimeNeo.admin.mapper;

import com.FoodtimeNeo.admin.dto.*;
import com.FoodtimeNeo.review.dto.PublishedReviewResponse;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class CatalogAdminStore {
    private final JdbcTemplate jdbc;
    private static final String REVIEW_FILTER = " WHERE strpos(lower(u.email),lower(?)) > 0 OR strpos(lower(u.display_name),lower(?)) > 0 OR strpos(r.user_id::text,lower(?)) > 0";
    private static final RowMapper<AdminCatalogResponse> ROW = (r,n) -> new AdminCatalogResponse(
            r.getObject("id",UUID.class),r.getObject("parent_id",UUID.class),r.getString("name"),r.getString("description"),
            r.getBigDecimal("price"),r.getString("cover_image_url"),r.getString("status"),r.getObject("sort_order",Integer.class));
    public CatalogAdminStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<AdminCatalogResponse> page(String type, UUID parent, int size, long offset) {
        String filter = parent == null || type.equals("dining-halls") ? "" : " WHERE " + parentColumn(type) + " = ?";
        var args = new ArrayList<Object>(); if (!filter.isEmpty()) args.add(parent);
        args.add(size); args.add(offset);
        return jdbc.query(select(type) + filter + " ORDER BY t.name,t.id LIMIT ? OFFSET ?",ROW,args.toArray());
    }
    public long count(String type, UUID parent) {
        if (parent == null || type.equals("dining-halls")) return jdbc.queryForObject("SELECT COUNT(*) FROM " + table(type),Long.class);
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table(type) + " WHERE " + parentColumn(type) + " = ?",Long.class,parent);
    }
    public AdminCatalogResponse get(String type, UUID id, boolean lock) {
        var items = jdbc.query(select(type) + " WHERE t.id = ?" + (lock ? " FOR UPDATE OF t" : ""),ROW,id);
        return items.isEmpty() ? null : items.get(0);
    }
    public boolean parentExists(String type, UUID id) {
        String parentTable = type.equals("stalls") ? "foodtime.dining_halls" : "foodtime.stalls";
        return !jdbc.query("SELECT id FROM " + parentTable + " WHERE id = ? FOR SHARE",(r,n) -> r.getObject(1,UUID.class),id).isEmpty();
    }
    public void save(String type, UUID id, UUID parent, CatalogEditRequest body, String status, int sort, boolean create) {
        if (create) {
            if (type.equals("dining-halls")) jdbc.update("INSERT INTO foodtime.dining_halls(id,name,description,cover_image_url,status,sort_order) VALUES (?, ?, ?, ?, ?, ?)",id,body.name().strip(),body.description(),body.coverImageUrl(),status,sort);
            else if (type.equals("stalls")) jdbc.update("INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url,status,sort_order) VALUES (?, ?, ?, ?, ?, ?, ?)",id,parent,body.name().strip(),body.description(),body.coverImageUrl(),status,sort);
            else jdbc.update("INSERT INTO foodtime.dishes(id,stall_id,name,description,price,status) VALUES (?, ?, ?, ?, ?, ?)",id,parent,body.name().strip(),body.description(),body.price(),status);
        } else {
            if (type.equals("dishes")) jdbc.update("UPDATE foodtime.dishes SET name = ?,description = ?,price = ?,status = ? WHERE id = ?",body.name().strip(),body.description(),body.price(),status,id);
            else jdbc.update("UPDATE " + table(type) + " SET name = ?,description = ?,cover_image_url = ?,status = ?,sort_order = ? WHERE id = ?",body.name().strip(),body.description(),body.coverImageUrl(),status,sort,id);
        }
    }
    public List<PublishedReviewResponse> reviews(String q,int size,long offset) {
        return jdbc.query("""
                SELECT r.*,d.name AS dish_name,s.id AS stall_id,s.name AS stall_name,h.id AS hall_id,h.name AS hall_name,
                u.display_name AS user_display_name,
                a.risk_level AS ai_risk_level,a.confidence AS ai_confidence,a.labels AS ai_labels,
                a.reason AS ai_reason,a.checked_at AS ai_checked_at
                FROM foodtime.dish_reviews r JOIN foodtime.dishes d ON d.id = r.dish_id
                JOIN foodtime.stalls s ON s.id = d.stall_id JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id
                JOIN foodtime.users u ON u.id = r.user_id
                LEFT JOIN foodtime.ai_moderation_results a ON a.submission_type = 'review' AND a.submission_id = r.submission_id
                """ + REVIEW_FILTER + " ORDER BY r.created_at DESC,r.id ASC LIMIT ? OFFSET ?",
                (r,n) -> new PublishedReviewResponse(r.getObject("id",UUID.class),r.getObject("dish_id",UUID.class),r.getObject("user_id",UUID.class),r.getString("user_display_name"),r.getInt("rating"),r.getString("content"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant(),r.getString("dish_name"),r.getObject("stall_id",UUID.class),r.getString("stall_name"),r.getObject("hall_id",UUID.class),r.getString("hall_name"),r.getString("ai_risk_level"),r.getBigDecimal("ai_confidence"),readLabels(r),r.getString("ai_reason"),readInstant(r)),q,q,q,size,offset);
    }

    private static java.util.List<String> readLabels(java.sql.ResultSet result) throws java.sql.SQLException {
        java.sql.Array column = result.getArray("ai_labels");
        if (column == null) return null;
        return java.util.List.of((String[]) column.getArray());
    }

    private static java.time.Instant readInstant(java.sql.ResultSet result) throws java.sql.SQLException {
        java.sql.Timestamp value = result.getTimestamp("ai_checked_at");
        return value == null ? null : value.toInstant();
    }
    public long reviewCount(String q) { return jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.dish_reviews r JOIN foodtime.users u ON u.id = r.user_id" + REVIEW_FILTER,Long.class,q,q,q); }
    public int removeReview(UUID id) { return jdbc.update("DELETE FROM foodtime.dish_reviews WHERE id = ?",id); }
    public int remove(String type,UUID id) { return jdbc.update("DELETE FROM " + table(type) + " WHERE id = ?",id); }

    private String select(String type) {
        String parent = type.equals("dining-halls") ? "NULL::uuid" : "t." + parentColumn(type);
        String price = type.equals("dishes") ? "t.price" : "NULL::numeric";
        String cover = type.equals("dishes") ? "(SELECT image_url FROM foodtime.dish_images WHERE dish_id = t.id AND status = 'approved' ORDER BY created_at DESC,id ASC LIMIT 1)" : "t.cover_image_url";
        String sort = type.equals("dishes") ? "NULL::integer" : "t.sort_order";
        return "SELECT t.id," + parent + " AS parent_id,t.name,t.description," + price + " AS price," + cover + " AS cover_image_url,t.status," + sort + " AS sort_order FROM " + table(type) + " t";
    }
    private String parentColumn(String type) { return type.equals("stalls") ? "dining_hall_id" : "stall_id"; }
    private String table(String type) {
        return switch(type) { case "dining-halls" -> "foodtime.dining_halls"; case "stalls" -> "foodtime.stalls"; case "dishes" -> "foodtime.dishes"; default -> throw new IllegalArgumentException("Unknown catalog type"); };
    }
}
