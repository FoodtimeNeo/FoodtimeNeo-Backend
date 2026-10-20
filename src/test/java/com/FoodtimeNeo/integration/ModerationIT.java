package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ModerationIT extends IntegrationApiSupport {
    private static final Path STORAGE = Path.of(System.getProperty("java.io.tmpdir"), "moderation-test-" + UUID.randomUUID());
    @DynamicPropertySource static void directory(DynamicPropertyRegistry properties) { properties.add("app.storage.directory", STORAGE::toString); }

    @Test
    void ordinaryUsersCannotModerateOrReadLogsEvenWithSpoofedRole() throws Exception {
        Browser student = login(owner);
        UUID id = review(student, "private review");
        assertThat(student.call("GET", "/api/v1/admin/moderation", null).statusCode()).isEqualTo(403);
        assertThat(student.call("GET", "/api/v1/admin/moderation/logs", null).statusCode()).isEqualTo(403);
        assertThat(student.call("POST", moderate("review",id), Map.of("action","approve","role","superadmin")).statusCode()).isEqualTo(403);
        assertThat(jdbc.queryForObject("SELECT status FROM foodtime.dish_review_submissions WHERE id = ?", String.class,id)).isEqualTo("pending");
    }

    @Test
    void queueItemsCarryTheSubmitterDisplayName() throws Exception {
        UUID id = review(login(owner), "named review");
        Browser admin = login(account("admin"));
        var items = body(admin.call("GET", "/api/v1/admin/moderation?status=pending&size=100", null))
                .path("data").path("items");
        var mine = java.util.stream.StreamSupport.stream(items.spliterator(), false)
                .filter(item -> item.path("id").asString().equals(id.toString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("created submission missing from queue"));
        assertThat(mine.path("userDisplayName").asString()).isEqualTo("integration user");
    }

    @Test
    void approvedReviewCopiesSubmissionTimeAndUpdatesTheUniqueFormalReview() throws Exception {
        Browser student = login(owner), admin = login(account("admin"));
        UUID first = review(student,"first");
        Timestamp submitted = jdbc.queryForObject("SELECT submitted_at FROM foodtime.dish_review_submissions WHERE id = ?",Timestamp.class,first);
        var approved = admin.call("POST",moderate("review",first),Map.of("action","approve"));
        assertThat(approved.statusCode()).isEqualTo(200);
        UUID formal = UUID.fromString(body(approved).path("data").path("approvedId").asString());
        assertThat(jdbc.queryForObject("SELECT created_at FROM foodtime.dish_reviews WHERE id = ?",Timestamp.class,formal)).isEqualTo(submitted);
        UUID second = review(student,"replacement");
        var replaced = admin.call("POST",moderate("review",second),Map.of("action","approve"));
        assertThat(body(replaced).path("data").path("approvedId").asString()).isEqualTo(formal.toString());
        assertThat(jdbc.queryForObject("SELECT content FROM foodtime.dish_reviews WHERE id = ?",String.class,formal)).isEqualTo("replacement");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.dish_reviews WHERE dish_id = ? AND user_id = ?",Long.class,dish,owner.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.content_moderation_logs WHERE moderator_user_id IN (SELECT id FROM foodtime.users WHERE role = 'admin') AND review_submission_id IN (?, ?)",Long.class,first,second)).isEqualTo(2);
    }

    @Test
    void concurrentModeratorsProduceOneApprovalAndOneLog() throws Exception {
        UUID id = review(login(owner),"concurrent");
        Account moderator = account("admin"); Browser first = login(moderator), second = login(moderator);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            Future<Integer> a = pool.submit(() -> { start.await(); return first.call("POST",moderate("review",id),Map.of("action","approve")).statusCode(); });
            Future<Integer> b = pool.submit(() -> { start.await(); return second.call("POST",moderate("review",id),Map.of("action","approve")).statusCode(); });
            start.countDown();
            assertThat(java.util.List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.content_moderation_logs WHERE review_submission_id = ?",Long.class,id)).isEqualTo(1);
    }

    @Test
    void rejectionRequiresReasonAndPreservesPrivateContent() throws Exception {
        Browser student = login(owner), admin = login(account("admin"));
        UUID id = review(student,"reject me");
        assertThat(admin.call("POST",moderate("review",id),Map.of("action","reject","reason"," ")).statusCode()).isEqualTo(400);
        assertThat(admin.call("POST",moderate("review",id),Map.of("action","reject","reason","unrelated")).statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT review_reason FROM foodtime.dish_review_submissions WHERE id = ?",String.class,id)).isEqualTo("unrelated");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.dish_reviews WHERE dish_id = ?",Long.class,dish)).isZero();
        assertThat(body(student.call("GET","/api/v1/users/me/review-submissions",null)).path("data").path("items").get(0).path("reviewReason").asString()).isEqualTo("unrelated");
    }

    @Test
    void approvingDishRequiresPriceAndCreatesLinkedApprovedImageAtomically() throws Exception {
        Browser student = login(owner), admin = login(account("admin"));
        String image = image(student);
        var created = student.call("POST","/api/v1/stalls/" + stall + "/submissions",Map.of("name","posted dish","imageUrl",image));
        UUID id = UUID.fromString(body(created).path("data").path("id").asString());
        assertThat(admin.call("POST",moderate("dish",id),Map.of("action","approve")).statusCode()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.dishes WHERE stall_id = ?",Long.class,stall)).isEqualTo(1);
        var approved = admin.call("POST",moderate("dish",id),Map.of("action","approve","price",new java.math.BigDecimal("8.20")));
        assertThat(approved.statusCode()).isEqualTo(200);
        UUID formal = UUID.fromString(body(approved).path("data").path("approvedId").asString());
        assertThat(jdbc.queryForObject("SELECT approved_dish_id FROM foodtime.dish_submissions WHERE id = ?",UUID.class,id)).isEqualTo(formal);
        assertThat(jdbc.queryForObject("SELECT price FROM foodtime.dishes WHERE id = ?",java.math.BigDecimal.class,formal)).isEqualByComparingTo("8.20");
        assertThat(jdbc.queryForObject("SELECT image_url FROM foodtime.dish_images WHERE dish_id = ? AND status = 'approved'",String.class,formal)).isEqualTo(image);
        assertThat(login(other).image(image).statusCode()).isEqualTo(200);
    }

    @Test
    void conflictingStallNamesRollBackWithoutApprovalOrLog() throws Exception {
        Browser student = login(owner), admin = login(account("admin"));
        var created = student.call("POST","/api/v1/dining-halls/" + hall + "/submissions",Map.of("name","integration stall","coverImageUrl",image(student)));
        UUID id = UUID.fromString(body(created).path("data").path("id").asString());
        assertThat(admin.call("POST",moderate("stall",id),Map.of("action","approve")).statusCode()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT status FROM foodtime.stall_submissions WHERE id = ?",String.class,id)).isEqualTo("pending");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.content_moderation_logs WHERE stall_submission_id = ?",Long.class,id)).isZero();
        jdbc.update("UPDATE foodtime.stall_submissions SET name = 'unique submitted stall' WHERE id = ?",id);
        assertThat(admin.call("POST",moderate("stall",id),Map.of("action","approve")).statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.stalls WHERE dining_hall_id = ?",Long.class,hall)).isEqualTo(2);
    }

    private UUID review(Browser browser,String content) throws Exception {
        var response = browser.call("POST",dishPath("/reviews"),Map.of("rating",4,"content",content));
        assertThat(response.statusCode()).isEqualTo(202);
        return UUID.fromString(body(response).path("data").path("id").asString());
    }
    private String moderate(String type,UUID id) { return "/api/v1/admin/moderation/" + type + "/" + id; }
    private String image(Browser browser) throws Exception {
        var out = new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(10,10,BufferedImage.TYPE_INT_RGB),"png",out);
        var response = browser.upload(out.toByteArray()); assertThat(response.statusCode()).isEqualTo(201);
        return body(response).path("data").path("imageUrl").asString();
    }
}
