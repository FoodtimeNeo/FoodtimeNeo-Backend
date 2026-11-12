package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogCorrectionIT extends IntegrationApiSupport {
    private static final String BASE = "/api/v1/users/me/catalog-corrections";

    private long revision(Browser browser) throws Exception {
        return body(browser.call("GET", dishPath(""), null)).path("data").path("revision").asLong();
    }

    private Map<String, Object> proposal(String name, String key, long baseRevision, List<String> evidence) {
        Map<String, Object> body = new HashMap<>();
        body.put("targetType", "dish");
        body.put("targetId", dish.toString());
        body.put("baseRevision", baseRevision);
        body.put("changedFields", List.of("name", "price"));
        body.put("name", name);
        body.put("price", new BigDecimal("15.50"));
        body.put("reason", "菜单已经更新，实际价格为15.5元");
        body.put("evidenceUrls", evidence);
        body.put("idempotencyKey", key);
        return body;
    }

    @Test
    void submitApproveAndKeepEvidencePrivate() throws Exception {
        var author = login(owner);
        byte[] image = png();
        var upload = author.upload(image);
        assertThat(upload.statusCode()).isEqualTo(201);
        String evidenceUrl = body(upload).path("data").path("imageUrl").asString();
        long base = revision(author);

        String key = UUID.randomUUID().toString();
        var submitted = author.call("POST", BASE, proposal("新菜名", key, base, List.of(evidenceUrl)));
        assertThat(submitted.statusCode()).isEqualTo(201);
        var created = body(submitted).path("data");
        String id = created.path("id").asString();
        assertThat(created.path("status").asString()).isEqualTo("pending");
        assertThat(created.path("baseRevision").asLong()).isEqualTo(base);
        assertThat(created.path("originalPrice").decimalValue()).isEqualByComparingTo("12.30");
        assertThat(created.path("evidenceUrls").size()).isEqualTo(1);

        var replay = author.call("POST", BASE, proposal("新菜名", key, base, List.of(evidenceUrl)));
        assertThat(replay.statusCode()).isEqualTo(201);
        assertThat(body(replay).path("data").path("id").asString()).isEqualTo(id);
        var reused = author.call("POST", BASE, proposal("另一个名字", key, base, List.of(evidenceUrl)));
        assertThat(reused.statusCode()).isEqualTo(409);
        assertThat(body(reused).path("code").asString()).isEqualTo("IDEMPOTENCY_KEY_REUSED");

        var otherUser = login(other);
        assertThat(otherUser.call("GET", BASE + "/" + id, null).statusCode()).isEqualTo(404);
        var admin = login(account("admin"));
        assertThat(admin.call("GET", "/api/v1/admin/catalog-corrections", null).statusCode()).isEqualTo(403);
        assertThat(admin.image(evidenceUrl).statusCode()).isEqualTo(404);
        var superadmin = login(account("superadmin"));
        assertThat(superadmin.call("GET", "/api/v1/admin/catalog-corrections", null).statusCode()).isEqualTo(200);
        assertThat(body(superadmin.call("GET", "/api/v1/admin/catalog-corrections/" + id, null))
                .path("data").path("correction").path("evidenceUrls").size()).isEqualTo(1);
        assertThat(superadmin.image(evidenceUrl).statusCode()).isEqualTo(200);

        var decided = superadmin.call("POST", "/api/v1/admin/catalog-corrections/" + id + "/decision",
                Map.of("decision", "approved", "reviewNote", "已核对菜单", "expectedRevision", 1));
        assertThat(decided.statusCode()).isEqualTo(200);
        assertThat(body(decided).path("data").path("correction").path("status").asString()).isEqualTo("approved");
        var updated = body(author.call("GET", dishPath(""), null)).path("data");
        assertThat(updated.path("name").asString()).isEqualTo("新菜名");
        assertThat(updated.path("price").decimalValue()).isEqualByComparingTo("15.50");
        assertThat(updated.path("revision").asLong()).isEqualTo(base + 1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM foodtime.catalog_correction_events WHERE correction_id = ?
                """, Integer.class, UUID.fromString(id))).isEqualTo(2);
    }

    @Test
    void directEditMakesApprovalConflictAndRejectionStillWorks() throws Exception {
        var author = login(owner);
        long base = revision(author);
        var submitted = author.call("POST", BASE,
                proposal("旧提案", UUID.randomUUID().toString(), base, List.of()));
        assertThat(submitted.statusCode()).isEqualTo(201);
        String id = body(submitted).path("data").path("id").asString();

        var admin = login(account("admin"));
        assertThat(admin.call("PUT", "/api/v1/admin/catalog/dishes/" + dish,
                Map.of("name", "管理员直接改名", "description", "直接编辑", "price", new BigDecimal("12.30"),
                        "status", "active")).statusCode()).isEqualTo(200);
        var stale = author.call("POST", BASE, proposal("基于旧版本", UUID.randomUUID().toString(), base, List.of()));
        assertThat(stale.statusCode()).isEqualTo(409);
        assertThat(body(stale).path("code").asString()).isEqualTo("CATALOG_REVISION_CONFLICT");

        var superadmin = login(account("superadmin"));
        var conflict = superadmin.call("POST", "/api/v1/admin/catalog-corrections/" + id + "/decision",
                Map.of("decision", "approved", "expectedRevision", 1));
        assertThat(conflict.statusCode()).isEqualTo(409);
        assertThat(body(conflict).path("code").asString()).isEqualTo("CATALOG_REVISION_CONFLICT");
        assertThat(jdbc.queryForObject("SELECT status FROM foodtime.catalog_corrections WHERE id = ?",
                String.class, UUID.fromString(id))).isEqualTo("pending");

        var rejected = superadmin.call("POST", "/api/v1/admin/catalog-corrections/" + id + "/decision",
                Map.of("decision", "rejected", "reviewNote", "与当前菜单不一致", "expectedRevision", 1));
        assertThat(rejected.statusCode()).isEqualTo(200);
        assertThat(body(rejected).path("data").path("correction").path("status").asString()).isEqualTo("rejected");
        assertThat(body(author.call("GET", dishPath(""), null)).path("data").path("name").asString())
                .isEqualTo("管理员直接改名");
    }

    @Test
    void fifthSubmissionSucceedsAndSixthIsLimited() throws Exception {
        var author = login(owner);
        long base = revision(author);
        for (int index = 0; index < 5; index++) {
            var response = author.call("POST", BASE,
                    proposal("名称" + index, UUID.randomUUID().toString(), base, List.of()));
            assertThat(response.statusCode()).isEqualTo(201);
        }
        var over = author.call("POST", BASE, proposal("第六个", UUID.randomUUID().toString(), base, List.of()));
        assertThat(over.statusCode()).isEqualTo(429);
        assertThat(body(over).path("code").asString()).isEqualTo("CORRECTION_DAILY_LIMIT");
    }

    @Test
    void visitorCannotSubmit() throws Exception {
        var visitor = login(account("visitor"));
        long base = revision(visitor);
        var response = visitor.call("POST", BASE, proposal("访客提案", UUID.randomUUID().toString(), base, List.of()));
        assertThat(response.statusCode()).isEqualTo(403);
    }

    private static byte[] png() throws Exception {
        var image = new java.awt.image.BufferedImage(3, 5, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", bytes);
        return bytes.toByteArray();
    }
}
