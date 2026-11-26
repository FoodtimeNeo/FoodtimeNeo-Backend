package com.FoodtimeNeo.search;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.dining.entity.DiningHall;
import com.FoodtimeNeo.dining.mapper.DiningHallMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

/** Reads verified WGS84 hall coordinates from PostgreSQL (cached by catalog revision) and allows config overrides. */
@Service
@Primary
public class DbHallDistanceProvider implements HallDistanceProvider {
    private record CachedCoordinates(long revision, Map<UUID, double[]> points) { }
    private final DiningHallMapper diningHalls;
    private final SearchLocations configured;
    private final JdbcTemplate jdbc;
    private volatile CachedCoordinates cache;

    public DbHallDistanceProvider(DiningHallMapper diningHalls, SearchLocations configured) {
        this(diningHalls, configured, null);
    }

    @Autowired
    public DbHallDistanceProvider(DiningHallMapper diningHalls, SearchLocations configured, JdbcTemplate jdbc) {
        this.diningHalls = diningHalls;
        this.configured = configured == null ? new SearchLocations(Map.of()) : configured;
        this.jdbc = jdbc;
    }

    @Override
    public Set<UUID> hallIds() {
        return Collections.unmodifiableSet(resolvedCoordinates().keySet());
    }

    @Override
    public Map<UUID, Double> distances(Double latitude, Double longitude) {
        if (latitude == null && longitude == null) return Map.of();
        if (latitude == null || longitude == null || !valid(latitude, longitude)) {
            throw new BusinessException("VALIDATION_ERROR", "定位坐标不合法", HttpStatus.BAD_REQUEST);
        }
        Map<UUID, Double> result = new LinkedHashMap<>();
        resolvedCoordinates().forEach((id, point) ->
                result.put(id, SearchLocations.meters(latitude, longitude, point[0], point[1])));
        return result;
    }

    private Map<UUID, double[]> resolvedCoordinates() {
        Long revision = jdbc == null ? null : jdbc.queryForObject(
                "SELECT revision FROM foodtime.public_catalog_revision WHERE id = 1", Long.class);
        CachedCoordinates current = cache;
        if (revision != null && current != null && current.revision() == revision) {
            return current.points();
        }
        Map<UUID, double[]> points = new LinkedHashMap<>();
        for (DiningHall hall : diningHalls.findActive()) {
            if (hall.latitude() != null && hall.longitude() != null) {
                double lat = hall.latitude().doubleValue();
                double lon = hall.longitude().doubleValue();
                if (valid(lat, lon)) {
                    points.put(hall.id(), new double[]{lat, lon});
                }
            }
        }
        configured.halls().forEach((id, point) ->
                points.put(id, new double[]{point.latitude(), point.longitude()}));
        Map<UUID, double[]> snapshot = Collections.unmodifiableMap(points);
        if (revision != null) {
            cache = new CachedCoordinates(revision, snapshot);
        }
        return snapshot;
    }

    private static boolean valid(double lat, double lon) {
        return Double.isFinite(lat) && Double.isFinite(lon) && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }
}
