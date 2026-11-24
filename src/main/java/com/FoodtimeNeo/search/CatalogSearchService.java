package com.FoodtimeNeo.search;

import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.dish.service.DishSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;
import java.util.*;

/** Groups and their bounded previews share one database snapshot; no per-card requests. */
@Service
public class CatalogSearchService {
    public record Dish(UUID id, String name, BigDecimal price, String priceUnit, String imageUrl,
            BigDecimal averageScore, long reviewCount) { }
    public record Stall(UUID id, String name, UUID diningHallId, String diningHallName, String imageUrl,
            BigDecimal averageScore, long reviewCount, Double distanceMeters, long matchCount, List<Dish> dishes) { }
    private final NamedParameterJdbcTemplate jdbc;
    private final HallDistanceProvider locations;
    public CatalogSearchService(NamedParameterJdbcTemplate jdbc, HallDistanceProvider locations) {
        this.jdbc = jdbc; this.locations = locations;
    }
    private record Spec(String cte, MapSqlParameterSource params, String order) { }
    private Spec spec(SearchRequest request) {
        String sort = request.sort() == null ? "relevance" : request.sort();
        var query = DishSearchService.normalize(request.q(), request.diningHallIds(), request.minPrice(),
                request.maxPrice(), request.minRating(), sort.equals("distance") ? "relevance" : sort);
        var distances = locations.distances(request.latitude(), request.longitude());
        if (sort.equals("distance") && request.latitude() == null)
            throw new BusinessException("LOCATION_REQUIRED", "请先获取当前位置", HttpStatus.BAD_REQUEST);
        var params = new MapSqlParameterSource().addValue("q", query.q())
                .addValue("min", query.minPrice()).addValue("max", query.maxPrice()).addValue("rating", query.minRating());
        StringBuilder points = new StringBuilder("SELECT NULL::uuid hall_id, NULL::double precision distance WHERE false");
        int index = 0;
        for (var entry : distances.entrySet()) {
            points.append(" UNION ALL SELECT :hall").append(index).append("::uuid, :distance").append(index).append("::double precision");
            params.addValue("hall" + index, entry.getKey()).addValue("distance" + index, entry.getValue()); index++;
        }
        String where = "";
        if (!query.diningHallIds().isEmpty()) {
            where += " AND h.id IN (:halls)"; params.addValue("halls", query.diningHallIds());
        }
        if (request.stallId() != null) { where += " AND s.id=:stall"; params.addValue("stall", request.stallId()); }
        if (query.minPrice() != null) where += " AND (d.price IS NULL OR d.price>=:min)";
        if (query.maxPrice() != null) where += " AND (d.price IS NULL OR d.price<=:max)";
        if (query.minRating() != null) where += " AND (r.average_score IS NULL OR r.average_score>=:rating)";
        String cte = "WITH positions AS (" + points + "), matches AS (" + """
            SELECT d.id,d.name,d.price,d.price_unit,s.id stall_id,s.name stall_name,s.cover_image_url stall_image,
              h.id hall_id,h.name hall_name,r.average_score,COALESCE(r.review_count,0) review_count,
              v.average_score stall_score,COALESCE(v.review_count,0) stall_reviews,pos.distance,
              CASE WHEN d.price IS NULL OR r.average_score IS NULL THEN 1 ELSE 0 END missing,
              CASE WHEN :q='' THEN 2 WHEN lower(d.name)=lower(:q) THEN 0
                   WHEN strpos(lower(d.name),lower(:q))=1 THEN 1 ELSE 2 END relevance
            FROM foodtime.dishes d JOIN foodtime.stalls s ON s.id=d.stall_id AND s.status='active'
            JOIN foodtime.dining_halls h ON h.id=s.dining_hall_id AND h.status='active'
            LEFT JOIN foodtime.dish_rating_summary r ON r.dish_id=d.id
            LEFT JOIN foodtime.venue_review_targets t ON t.stall_id=s.id
            LEFT JOIN foodtime.venue_rating_summary v ON v.target_id=t.id
            LEFT JOIN positions pos ON pos.hall_id=h.id
            WHERE d.status='active' AND (:q='' OR strpos(lower(d.name),lower(:q))>0)
            """ + where + ") ";
        // Only these fixed fragments may be interpolated into SQL.
        String order = switch (sort) {
            case "price_asc" -> "missing,price ASC NULLS LAST,id";
            case "price_desc" -> "missing,price DESC NULLS LAST,id";
            case "rating_desc" -> "missing,average_score DESC NULLS LAST,id";
            case "distance" -> "distance ASC NULLS LAST,relevance,missing,id";
            default -> "missing,relevance,lower(name),id";
        };
        return new Spec(cte, params, order);
    }
    private static Dish dish(java.sql.ResultSet row) throws java.sql.SQLException {
        return new Dish(row.getObject("id", UUID.class), row.getString("name"), row.getBigDecimal("price"),
                row.getString("price_unit"), row.getString("image_url"), row.getBigDecimal("average_score"), row.getLong("review_count"));
    }
    private static final String IMAGE = """
        LEFT JOIN LATERAL (SELECT image_url FROM foodtime.dish_images WHERE dish_id=m.id AND status='approved'
            ORDER BY created_at DESC,id LIMIT 1) image ON true
        """;
    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public PageResponse<Stall> search(SearchRequest request) {
        var spec = spec(request);
        var page = new PageRequest(request.page() == null ? 1 : request.page(), request.size() == null ? 10 : request.size());
        int preview = request.previewSize() == null ? 4 : request.previewSize();
        if (preview < 2 || preview > 8) throw new BusinessException("VALIDATION_ERROR", "预览数量须在2到8之间", HttpStatus.BAD_REQUEST);
        var params = spec.params().addValue("limit", page.size()).addValue("offset", page.offset()).addValue("preview", preview);
        long total = jdbc.queryForObject(spec.cte() + "SELECT count(DISTINCT stall_id) FROM matches", params, Long.class);
        String ranked = spec.cte() + ", ranked AS (SELECT *,row_number() OVER(ORDER BY " + spec.order()
                + ") ranking FROM matches), groups AS (SELECT stall_id,min(ranking) first_rank,count(*) match_count"
                + " FROM ranked GROUP BY stall_id ORDER BY first_rank,stall_id LIMIT :limit OFFSET :offset) ";
        List<Stall> groups = jdbc.query(ranked + """
            SELECT m.*,g.match_count FROM groups g JOIN ranked m ON m.stall_id=g.stall_id AND m.ranking=g.first_rank
            ORDER BY g.first_rank,g.stall_id
            """, params, (row, n) -> new Stall(row.getObject("stall_id", UUID.class), row.getString("stall_name"),
                row.getObject("hall_id", UUID.class), row.getString("hall_name"), row.getString("stall_image"),
                row.getBigDecimal("stall_score"), row.getLong("stall_reviews"), row.getObject("distance", Double.class),
                row.getLong("match_count"), new ArrayList<>()));
        if (!groups.isEmpty()) {
            var byId = new LinkedHashMap<UUID, Stall>(); groups.forEach(group -> byId.put(group.id(), group));
            params.addValue("ids", byId.keySet());
            jdbc.query(spec.cte() + ", preview AS (SELECT *,row_number() OVER(PARTITION BY stall_id ORDER BY "
                    + spec.order() + ") preview_rank FROM matches WHERE stall_id IN (:ids)) SELECT m.*,image.image_url FROM preview m "
                    + IMAGE + " WHERE m.preview_rank<=:preview ORDER BY m.stall_id,m.preview_rank", params,
                    (org.springframework.jdbc.core.RowCallbackHandler) row -> byId.get(row.getObject("stall_id", UUID.class)).dishes().add(dish(row)));
        }
        return PageResponse.of(groups, total, page);
    }
    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public PageResponse<Dish> dishes(SearchRequest request) {
        if (request.stallId() == null) throw new BusinessException("VALIDATION_ERROR", "请选择档口", HttpStatus.BAD_REQUEST);
        var spec = spec(request);
        var page = new PageRequest(request.page() == null ? 1 : request.page(), request.size() == null ? 20 : request.size());
        var params = spec.params().addValue("limit", page.size()).addValue("offset", page.offset());
        long total = jdbc.queryForObject(spec.cte() + "SELECT count(*) FROM matches", params, Long.class);
        var items = jdbc.query(spec.cte() + "SELECT m.*,image.image_url FROM matches m " + IMAGE
                + " ORDER BY " + spec.order().replace("id", "m.id") + " LIMIT :limit OFFSET :offset", params, (row, n) -> dish(row));
        return PageResponse.of(items, total, page);
    }
}
