package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class MyDishSubmissionIT extends ReadApiITSupport {
    private static final String PATH = "/api/v1/users/me/dish-submissions";
    private final List<UUID> submissionIds = new ArrayList<>();
    @Override protected void cleanContentFixtures() {
        for (UUID id : submissionIds) { jdbc.update("delete from foodtime.dish_submissions where id=?", id); }
    }
    private UUID submission(UUID stall, UUID owner, UUID reviewer, String status, String price, UUID approvedDish, String time) {
        UUID id = UUID.randomUUID();
        submissionIds.add(id);
        boolean reviewed = !status.equals("pending");
        jdbc.update("""
                insert into foodtime.dish_submissions
                (id,stall_id,name,price,image_url,submitted_by_user_id,status,reviewed_by_user_id,reviewed_at,
                 review_reason,approved_dish_id,created_at)
                values (?,?,?,?,?,?,?,?,?,?,?,?)
                """, id, stall, "投稿菜品-" + id, price == null ? null : new BigDecimal(price), "https://example.test/" + id + ".jpg",
                owner, status, reviewed ? reviewer : null,
                reviewed ? Timestamp.from(Instant.parse(time).plusSeconds(60)) : null,
                reviewed ? "审核说明" : null, approvedDish, Timestamp.from(Instant.parse(time)));
        return id;
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "admin", "superadmin"})
    void ownSubmissionContentAllStatesAndNullPricesRemainVisibleWhenAncestorsInactive(String role) throws Exception {
        UUID hall = hall("inactive");
        UUID stall = stall(hall, "inactive");
        UUID formalDish = dish(stall, "inactive", "99999999.99");
        UUID owner = user(role);
        UUID other = user("user");
        UUID reviewer = user("admin");
        UUID pending = submission(stall, owner, reviewer, "pending", null, null, "2026-01-02T00:00:00Z");
        UUID approved = submission(stall, owner, reviewer, "approved", "99999999.99", formalDish, "2026-01-02T00:00:00Z");
        UUID rejected = submission(stall, owner, reviewer, "rejected", null, null, "2026-01-01T00:00:00Z");
        UUID foreign = submission(stall, other, null, "pending", "0.00", null, "2026-01-03T00:00:00Z");
        var browser = login(owner);
        var response = browser.get(PATH + "?size=2&submittedByUserId=" + other + "&userId=" + other + "&status=rejected");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).doesNotContain(foreign.toString(), other.toString(), reviewer.toString());
        var data = json.readTree(response.body()).path("data");
        assertThat(data.path("total").asLong()).isEqualTo(3);
        var ties = new ArrayList<>(List.of(pending, approved));
        ties.sort(Comparator.comparing(UUID::toString));
        for (int i=0; i<2; i++) {
            var item = data.path("items").get(i);
            assertThat(item.path("id").asString()).isEqualTo(ties.get(i).toString());
            assertThat(item.size()).isEqualTo(15);
            assertThat(item.path("stallId").asString()).isEqualTo(stall.toString());
            assertThat(item.path("diningHallId").asString()).isEqualTo(hall.toString());
            assertThat(item.path("name").asString()).isEqualTo("投稿菜品-" + ties.get(i));
            assertThat(item.path("description").isNull()).isTrue();
            if (ties.get(i).equals(pending)) {
                assertThat(item.has("price")).isTrue();
                assertThat(item.path("price").isNull()).isTrue();
                assertThat(item.path("reviewedAt").isNull()).isTrue();
                assertThat(item.path("reviewReason").isNull()).isTrue();
                assertThat(item.path("approvedDishId").isNull()).isTrue();
            } else {
                assertThat(item.path("price").isNumber()).isTrue();
                assertThat(new BigDecimal(item.path("price").asString())).isEqualByComparingTo("99999999.99");
                assertThat(item.path("approvedDishId").asString()).isEqualTo(formalDish.toString());
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
    void foreignSubmissionsDoNotAffectEmptyOwnerResult() throws Exception {
        UUID stall = stall(hall("active"), "active");
        submission(stall, user("user"), null, "pending", null, null, "2026-01-01T00:00:00Z");
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
    void preservesSubmittedContentAfterFormalDishChangesAndReflectsReviewUpdates() throws Exception {
        UUID stall = stall(hall("active"), "active");
        UUID owner = user("user");
        UUID reviewer = user("admin");
        UUID formal = dish(stall, "active", "12.30");
        UUID id = submission(stall, owner, reviewer, "approved", "12.30", formal, "2026-01-01T00:00:00Z");
        jdbc.update("update foodtime.dishes set name='正式菜品已改名',price=99.00 where id=?", formal);
        var item = json.readTree(login(owner).get(PATH).body()).path("data").path("items").get(0);
        assertThat(item.path("name").asString()).isEqualTo("投稿菜品-" + id);
        assertThat(new BigDecimal(item.path("price").asString())).isEqualByComparingTo("12.30");
        jdbc.update("update foodtime.dish_submissions set review_reason='补充审核说明' where id=?", id);
        assertThat(login(owner).get(PATH).body()).contains("补充审核说明");
        assertThat(jdbc.queryForObject("select count(*) from foodtime.dish_submissions where submitted_by_user_id=?", Integer.class, owner)).isEqualTo(1);
    }

    @Test
    void validatesLoginPagingAndDisabledAccount() throws Exception {
        assertThat(new Browser().get(PATH).statusCode()).isEqualTo(401);
        var browser = login("user");
        assertThat(browser.get(PATH + "?size=101").statusCode()).isEqualTo(400);
        jdbc.update("update foodtime.users set status='disabled' where id=?", userIds.get(0));
        assertThat(browser.get(PATH).statusCode()).isEqualTo(401);
    }

    @Test
    void openApiDefinesNumericNullablePriceAndNullableApprovalReference() throws Exception {
        var document = json.readTree(new Browser().get("/v3/api-docs").body());
        var get = document.path("paths").path(PATH).path("get");
        assertThat(get.path("security").get(0).has("sessionCookie")).isTrue();
        assertThat(get.path("parameters").size()).isEqualTo(2);
        var properties = document.path("components").path("schemas").path("DishSubmissionResponse").path("properties");
        assertThat(properties.size()).isEqualTo(15);
        assertThat(properties.path("price").path("type").toString()).contains("number");
        for (String field : List.of("price", "description", "reviewedAt", "reviewReason", "approvedDishId")) {
            var property = properties.path(field);
            assertThat(property.path("nullable").asBoolean() || property.path("type").toString().contains("\"null\""))
                    .as("nullable %s", field).isTrue();
        }
    }
}
