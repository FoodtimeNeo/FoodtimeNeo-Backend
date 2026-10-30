package com.FoodtimeNeo.dashboard;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 平台数据看板：仅 superadmin 可读，读多写少，全走一次性聚合。
 * 在线口径（与前后端契约一致，见 docs/admin-dashboard.md）：
 * onlineUsers = Redis 中未过期的会话数；onlinePeak(day) = 当日去重活跃用户数。
 */
@Service
public class DashboardService {
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int MAX_DAYS = 31;

    private static final Logger LOG = LoggerFactory.getLogger(DashboardService.class);
    private final DashboardStore store;
    private final StringRedisTemplate redis;
    private final String sessionNamespace;

    public DashboardService(DashboardStore store, StringRedisTemplate redis,
            @Value("${spring.session.redis.namespace:spring:session}") String sessionNamespace) {
        this.store = store;
        this.redis = redis;
        this.sessionNamespace = sessionNamespace;
    }

    @Transactional(readOnly = true)
    public DashboardDtos.DashboardResponse overview(UserProfile actor, int days) {
        guard(actor);
        if (days < 1 || days > MAX_DAYS) {
            throw new BusinessException("VALIDATION_ERROR", "days 取值范围为 1 到 31",
                    HttpStatus.BAD_REQUEST);
        }
        LocalDate end = LocalDate.now(BUSINESS_ZONE);
        LocalDate start = end.minusDays(days - 1);
        Instant from = start.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant to = end.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
        DashboardStore.Totals totals;
        Map<String, Map<String, Long>> counts;
        Map<String, Long> peaks;
        try {
            totals = store.totals();
            counts = store.dailyNewCounts(from, to);
            peaks = store.dailyActiveUsers(from, to);
        } catch (DataAccessException exception) {
            LOG.error("Dashboard query failed", exception);
            throw new BusinessException("DASHBOARD_UNAVAILABLE", "数据看板服务暂时不可用",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        List<DashboardDtos.TrendDay> trend = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            String key = day.toString();
            Map<String, Long> row = counts.getOrDefault(key, Map.of());
            trend.add(new DashboardDtos.TrendDay(key,
                    row.getOrDefault("newUsers", 0L),
                    row.getOrDefault("newReviews", 0L),
                    row.getOrDefault("newReplies", 0L),
                    row.getOrDefault("newPosts", 0L),
                    peaks.getOrDefault(key, 0L)));
        }
        return new DashboardDtos.DashboardResponse(totals.users(), onlineUsers(), totals.reviews(),
                totals.replies(), totals.posts(), totals.halls(), totals.stalls(), totals.dishes(),
                Instant.now(), trend);
    }

    private void guard(UserProfile actor) {
        if (!actor.status().equals("active") || !"superadmin".equals(actor.role())) {
            throw new BusinessException("ACCESS_DENIED", "需要平台管理员权限", HttpStatus.FORBIDDEN);
        }
    }

    /** 有效会话数：会话命名空间下除过期标记外的会话键数（兼容自定义命名空间）。 */
    long onlineUsers() {
        try {
            String match = sessionNamespace + ":*";
            Long count = redis.execute((RedisCallback<Long>) connection -> {
                long found = 0;
                Cursor<byte[]> cursor = connection.scan(
                        ScanOptions.scanOptions().match(match).count(1000).build());
                while (cursor.hasNext()) {
                    String key = new String(cursor.next(), StandardCharsets.UTF_8);
                    if (key.contains(":sessions:") && !key.contains(":expires:")) {
                        found++;
                    }
                }
                return found;
            });
            return count == null ? 0 : count;
        } catch (DataAccessException exception) {
            LOG.error("Dashboard session scan failed: {}", exception.getClass().getSimpleName());
            throw new BusinessException("DASHBOARD_UNAVAILABLE", "数据看板服务暂时不可用",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

}
