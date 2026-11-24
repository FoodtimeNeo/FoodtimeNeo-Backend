package com.FoodtimeNeo.search;

import com.FoodtimeNeo.common.exception.BusinessException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import java.util.*;

/** WGS84 points must be verified independently of the illustrated campus map. */
@ConfigurationProperties(prefix = "app.search")
public record SearchLocations(Map<UUID, Point> halls) implements HallDistanceProvider {
    public SearchLocations {
        halls = halls == null ? Map.of() : Map.copyOf(halls);
        halls.values().forEach(point -> {
            if (point == null || point.latitude() == null || point.longitude() == null || !valid(point.latitude(), point.longitude())
                    || point.source() == null || point.source().isBlank())
                throw new IllegalArgumentException("Search hall coordinates require valid WGS84 and a verified source");
        });
    }
    public record Point(Double latitude, Double longitude, String source) { }
    @Override public Set<UUID> hallIds() { return halls.keySet(); }
    public Map<UUID, Double> distances(Double latitude, Double longitude) {
        if (latitude == null && longitude == null) return Map.of();
        if (latitude == null || longitude == null || !valid(latitude, longitude))
            throw new BusinessException("VALIDATION_ERROR", "定位坐标不合法", HttpStatus.BAD_REQUEST);
        Map<UUID, Double> distances = new LinkedHashMap<>();
        halls.forEach((id, point) -> distances.put(id, meters(latitude, longitude, point.latitude(), point.longitude())));
        return distances;
    }
    private static boolean valid(double lat, double lon) {
        return Double.isFinite(lat) && Double.isFinite(lon) && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }
    static double meters(double lat1, double lon1, double lat2, double lon2) {
        double a = Math.pow(Math.sin(Math.toRadians(lat2 - lat1) / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(Math.toRadians(lon2 - lon1) / 2), 2);
        return 6371008.8 * 2 * Math.asin(Math.sqrt(Math.min(1, Math.max(0, a))));
    }
}
