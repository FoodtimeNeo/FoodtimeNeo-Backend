package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CapabilityIT extends IntegrationApiSupport {
    private static List<String> names(tools.jackson.databind.JsonNode body) {
        List<String> result = new ArrayList<>();
        for (var name : body.path("data").path("capabilities")) result.add(name.asString());
        return result;
    }

    @Test
    void adminAndSuperadminReceiveTheirCurrentDefaultCapabilities() throws Exception {
        var admin = login(account("admin"));
        var adminBody = body(admin.call("GET", "/api/v1/users/me/capabilities", null));
        assertThat(names(adminBody)).containsExactly("catalog.edit", "moderation.review");
        assertThat(names(adminBody)).doesNotContain("correction.review", "report.review", "report.reply", "appeal.review");
        assertThat(adminBody.path("data").path("revision").asString()).isEqualTo("default-v1");

        var superadmin = login(account("superadmin"));
        var superBody = body(superadmin.call("GET", "/api/v1/users/me/capabilities", null));
        assertThat(names(superBody)).contains("correction.review", "report.review", "report.reply", "appeal.review");
    }

    @Test
    void anonymousCallersCannotReadCapabilities() throws Exception {
        var response = new Browser().call("GET", "/api/v1/users/me/capabilities", null);
        assertThat(response.statusCode()).isEqualTo(401);
    }
}
