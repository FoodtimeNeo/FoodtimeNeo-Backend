package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

/**
 * AI model mapping admin API: snapshot shows effective source,
 * only superadmin can pin, overrides land in Redis.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.rating-summary.refresh-enabled=false",
                "app.ai.triage.redis-namespace=foodtime:ai:triage-test"})
class AiModelMapIT extends IntegrationApiSupport {
    @Autowired
    private StringRedisTemplate redis;

    @AfterEach
    void clearOverrides() {
        redis.delete("foodtime:ai:triage-test");
    }

    @Test
    void snapshotShowsEnvDefaultsAndPinRequiresSuperadmin() throws Exception {
        var user = login(owner);
        assertThat(user.call("GET", "/api/v1/admin/ai/models", null).statusCode()).isEqualTo(403);

        var admin = login(account("admin"));
        var snapshot = body(admin.call("GET", "/api/v1/admin/ai/models", null));
        assertThat(snapshot.path("data").path("slots").toString()).contains("env");
        assertThat(admin.call("POST", "/api/v1/admin/ai/models",
                Map.of("slot", "primary-model", "model", "x")).statusCode()).isEqualTo(403);

        var superadmin = login(account("superadmin"));
        assertThat(superadmin.call("POST", "/api/v1/admin/ai/models",
                Map.of("slot", "primary-model", "model", "pinned-primary")).statusCode()).isEqualTo(200);
        var after = body(superadmin.call("GET", "/api/v1/admin/ai/models", null)).path("data");
        boolean pinned = false;
        for (var slot : after.path("slots")) {
            if (slot.path("slot").asString().equals("primary-model")) {
                pinned = slot.path("model").asString().equals("pinned-primary")
                        && slot.path("source").asString().equals("redis");
            }
        }
        assertThat(pinned).isTrue();
        assertThat(after.path("pinnedBy").asString()).isNotBlank();
    }
}
