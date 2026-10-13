package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class MyReviewSubmissionIT extends ReadApiITSupport {
    private static final String PATH = "/api/v1/users/me/review-submissions";
    private final List<UUID> submissionIds = new ArrayList<>();
    @Override protected void cleanContentFixtures() {
        for (UUID id : submissionIds) { jdbc.update("delete from foodtime.dish_review_submissions where id=?", id); }
    }
    private UUID submission(UUID dish, UUID owner, UUID reviewer, String status, int rating, String time) {
        UUID id = UUID.randomUUID();
        submissionIds.add(id);
        boolean reviewed = !status.equals("pending");
        jdbc.update("""
                insert into foodtime.dish_review_submissions
                (id,dish_id,user_id,rating,content,status,submitted_at,reviewed_by_user_id,reviewed_at,review_reason)
                values (?,?,?,?,?,?,?,?,?,?)
                """, id, dish, owner, rating, "评论提交-" + id, status, Timestamp.from(Instant.parse(time)),
                reviewed ? reviewer : null, reviewed ? Timestamp.from(Instant.parse(time).plusSeconds(60)) : null,
                reviewed ? "审核说明" : null);
        return id;
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "admin", "superadmin"})
    void onlyOwnAllStatesRemainVisibleWithInactiveAncestorsAndStablePaging(String role) throws Exception {
        UUID hall = hall("inactive");
        UUID stall = stall(hall, "inactive");
        UUID dish = dish(stall, "inactive", "1.00");
        UUID owner = user(role);
        UUID other = user("user");
        UUID reviewer = user("admin");
        UUID pending = submission(dish, owner, reviewer, "pending", 1, "2026-01-02T00:00:00Z");
        UUID approved = submission(dish, owner, reviewer, "approved", 5, "2026-01-02T00:00:00Z");
        UUID rejected = submission(dish, owner, reviewer, "rejected", 3, "2026-01-01T00:00:00Z");
        UUID foreign = submission(dish, other, reviewer, "pending", 2, "2026-01-03T00:00:00Z");
        var browser = login(owner);
        var response = browser.get(PATH + "?size=2&userId=" + other + "&status=rejected");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).doesNotContain(foreign.toString(), reviewer.toString(), other.toString());
        var data = json.readTree(response.body()).path("data");
        assertThat(data.path("total").asLong()).isEqualTo(3);
        var ties = new ArrayList<>(List.of(pending, approved));
        ties.sort(Comparator.comparing(UUID::toString));
        for (int i = 0; i < 2; i++) {
            var item = data.path("items").get(i);
            assertThat(item.path("id").asString()).isEqualTo(ties.get(i).toString());
            assertThat(item.path("dishId").asString()).isEqualTo(dish.toString());
            assertThat(item.path("stallId").asString()).isEqualTo(stall.toString());
            assertThat(item.path("diningHallId").asString()).isEqualTo(hall.toString());
            assertThat(item.path("dishName").asString()).isEqualTo("查询测试菜品-" + dish);
            assertThat(item.path("submittedAt").asString()).isEqualTo("2026-01-02T00:00:00Z");
            if (ties.get(i).equals(pending)) {
                assertThat(item.path("rating").asInt()).isEqualTo(1);
                assertThat(item.has("reviewedAt")).isTrue();
                assertThat(item.path("reviewedAt").isNull()).isTrue();
                assertThat(item.path("reviewReason").isNull()).isTrue();
            } else {
                assertThat(item.path("rating").asInt()).isEqualTo(5);
                assertThat(item.path("reviewReason").asString()).isEqualTo("审核说明");
            }
        }
        var second = json.readTree(browser.get(PATH + "?page=2&size=2").body()).path("data");
        assertThat(second.path("items").size()).isEqualTo(1);
        assertThat(second.path("items").get(0).path("id").asString()).isEqualTo(rejected.toString());
        var beyond = json.readTree(browser.get(PATH + "?page=2147483647&size=100").body()).path("data");
        assertThat(beyond.path("items").size()).isZero();
        assertThat(beyond.path("total").asLong()).isEqualTo(3);
    }

    @Test
    void foreignRecordsDoNotAffectEmptyOwnerPageOrCount() throws Exception {
        UUID dish = dish(stall(hall("active"), "active"), "active", "1.00");
        UUID other = user("user");
        submission(dish, other, null, "pending", 4, "2026-01-01T00:00:00Z");
        var response = login("user").get(PATH);
        assertThat(response.statusCode()).isEqualTo(200);
        var data = json.readTree(response.body()).path("data");
        assertThat(data.path("items").isArray()).isTrue();
        assertThat(data.path("items").size()).isZero();
        assertThat(data.path("total").asLong()).isZero();
        assertThat(data.path("page").asInt()).isEqualTo(1);
        assertThat(data.path("size").asInt()).isEqualTo(20);
    }

    @Test
    void subsequentRequestsReflectReviewOutcomeWithoutWritingOnRead() throws Exception {
        UUID dish = dish(stall(hall("active"), "active"), "active", "1.00");
        UUID owner = user("user");
        UUID reviewer = user("admin");
        UUID id = submission(dish, owner, reviewer, "pending", 4, "2026-01-01T00:00:00Z");
        var browser = login(owner);
        assertThat(json.readTree(browser.get(PATH).body()).path("data").path("items").get(0).path("status").asString()).isEqualTo("pending");
        jdbc.update("update foodtime.dish_review_submissions set status='rejected',reviewed_by_user_id=?,reviewed_at=now(),review_reason='不符合规范' where id=?", reviewer, id);
        var item = json.readTree(browser.get(PATH).body()).path("data").path("items").get(0);
        assertThat(item.path("status").asString()).isEqualTo("rejected");
        assertThat(item.path("reviewReason").asString()).isEqualTo("不符合规范");
        assertThat(jdbc.queryForObject("select count(*) from foodtime.dish_review_submissions where user_id=?", Integer.class, owner)).isEqualTo(1);
    }

    @Test
    void validatesLoginPagingAndDisabledAccount() throws Exception {
        assertThat(new Browser().get(PATH).statusCode()).isEqualTo(401);
        var browser = login("user");
        assertThat(browser.get(PATH + "?page=0").statusCode()).isEqualTo(400);
        jdbc.update("update foodtime.users set status='disabled' where id=?", userIds.get(0));
        assertThat(browser.get(PATH).statusCode()).isEqualTo(401);
    }

    @Test
    void openApiExposesOnlyPagingParametersAndNullableReviewMetadata() throws Exception {
        var document = json.readTree(new Browser().get("/v3/api-docs").body());
        var get = document.path("paths").path(PATH).path("get");
        assertThat(get.path("security").get(0).has("sessionCookie")).isTrue();
        assertThat(get.path("parameters").size()).isEqualTo(2);
        var properties = document.path("components").path("schemas").path("ReviewSubmissionResponse").path("properties");
        assertThat(properties.size()).isEqualTo(14);
        assertThat(properties.has("reviewedByUserId")).isFalse();
        for (String field : List.of("reviewedAt", "reviewReason")) {
            var property = properties.path(field);
            assertThat(property.path("nullable").asBoolean() || property.path("type").toString().contains("\"null\"")).isTrue();
        }
    }
}
