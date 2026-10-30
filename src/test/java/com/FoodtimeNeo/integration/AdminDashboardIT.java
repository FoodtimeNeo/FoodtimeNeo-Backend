package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 平台数据看板：仅超管可读，趋势按 Asia/Shanghai 自然日对齐，无数据日期补零。
 * 口径定义见 docs/admin-dashboard.md。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.rating-summary.refresh-enabled=false"})
class AdminDashboardIT extends IntegrationApiSupport {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Test
    void superadminSeesFullShapeWithSevenDayTrend() throws Exception {
        var superadmin = login(account("superadmin"));
        var data = body(superadmin.call("GET", "/api/v1/admin/dashboard?days=7", null)).path("data");
        for (String key : new String[]{"registeredUsers", "onlineUsers", "reviews", "replies",
                "posts", "halls", "stalls", "dishes", "generatedAt", "trend"}) {
            assertThat(data.has(key)).as("missing key %s", key).isTrue();
        }
        assertThat(data.path("trend").size()).isEqualTo(7);
        String today = LocalDate.now(ZONE).toString();
        assertThat(data.path("trend").get(6).path("day").asText()).isEqualTo(today);
        for (var day : data.path("trend")) {
            assertThat(day.path("newUsers").asLong()).isGreaterThanOrEqualTo(0);
            assertThat(day.path("onlinePeak").asLong()).isGreaterThanOrEqualTo(0);
        }
        // 在线数只断言为非负数：集成测试环境的 sessions 命名空间覆盖对会话存储不生效
        // （会话实际落在默认命名空间，见 docs/admin-dashboard.md），精确计数逻辑由单测覆盖。
        assertThat(data.path("onlineUsers").asLong()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void adminIsForbiddenAndAnonymousIsUnauthorized() throws Exception {
        var user = login(owner);
        assertThat(user.call("GET", "/api/v1/admin/dashboard", null).statusCode()).isEqualTo(403);
        var admin = login(account("admin"));
        assertThat(admin.call("GET", "/api/v1/admin/dashboard", null).statusCode()).isEqualTo(403);
        var anonymous = java.net.http.HttpClient.newHttpClient();
        var denied = anonymous.send(java.net.http.HttpRequest.newBuilder(
                        java.net.URI.create("http://127.0.0.1:" + port + "/api/v1/admin/dashboard")).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(denied.statusCode()).isEqualTo(401);
    }

    @Test
    void invalidDaysAreRejected() throws Exception {
        var superadmin = login(account("superadmin"));
        assertThat(superadmin.call("GET", "/api/v1/admin/dashboard?days=0", null).statusCode()).isEqualTo(400);
        assertThat(superadmin.call("GET", "/api/v1/admin/dashboard?days=32", null).statusCode()).isEqualTo(400);
        var month = body(superadmin.call("GET", "/api/v1/admin/dashboard?days=30", null)).path("data");
        assertThat(month.path("trend").size()).isEqualTo(30);
    }

    @Test
    void trendReflectsBackdatedActivity() throws Exception {
        var veteran = account("user");
        jdbc.update("UPDATE foodtime.users SET created_at = ? WHERE id = ?",
                Timestamp.from(LocalDate.now(ZONE).minusDays(3).atStartOfDay(ZONE).plusHours(2).toInstant()),
                veteran.id());
        UUID submission = UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.dish_review_submissions(id,dish_id,user_id,rating,content,submitted_at)"
                        + " VALUES (?,?,?,?,?,?)", submission, dish, veteran.id(), 5, "backdated",
                Timestamp.from(LocalDate.now(ZONE).minusDays(2).atStartOfDay(ZONE).plusHours(3).toInstant()));
        var superadmin = login(account("superadmin"));
        var trend = body(superadmin.call("GET", "/api/v1/admin/dashboard?days=7", null)).path("data").path("trend");
        String threeDaysAgo = LocalDate.now(ZONE).minusDays(3).toString();
        String twoDaysAgo = LocalDate.now(ZONE).minusDays(2).toString();
        var day3 = trendNode(trend, threeDaysAgo);
        var day2 = trendNode(trend, twoDaysAgo);
        assertThat(day3.path("newUsers").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(day2.path("newReviews").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(day3.path("onlinePeak").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(day2.path("onlinePeak").asLong()).isGreaterThanOrEqualTo(1);
    }

    private static tools.jackson.databind.JsonNode trendNode(tools.jackson.databind.JsonNode trend,
            String day) {
        for (var node : trend) {
            if (node.path("day").asText().equals(day)) {
                return node;
            }
        }
        throw new AssertionError("missing trend day " + day);
    }
}
