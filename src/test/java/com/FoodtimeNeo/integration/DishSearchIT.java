package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DishSearchIT extends IntegrationApiSupport {
    private String path(String query) {
        return "/api/v1/dishes/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
    }

    private UUID newStall(String hallStatus, String stallStatus) {
        UUID hallId = UUID.randomUUID();
        additionalHalls.add(hallId);
        jdbc.update("INSERT INTO foodtime.dining_halls(id,name,status) VALUES (?, '另一食堂', ?)", hallId, hallStatus);
        UUID stallId = UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.stalls(id,dining_hall_id,name,status) VALUES (?, ?, '二号档口', ?)",
                stallId, hallId, stallStatus);
        return stallId;
    }

    private UUID addDish(UUID stallId, String name, String status) {
        UUID dishId = UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.dishes(id,stall_id,name,price,status) VALUES (?, ?, ?, 12.30, ?)",
                dishId, stallId, name, status);
        return dishId;
    }

    @Test void returnsOnlyActiveCatalogRowsAcrossHallsWithStableRanking() throws Exception {
        String q = "鸡腿饭" + UUID.randomUUID().toString().substring(0, 6);
        UUID otherStall = newStall("active", "active");
        UUID exact = addDish(otherStall, q, "active");
        UUID prefix = addDish(stall, q + "加蛋", "active");
        UUID contains = addDish(stall, "照烧" + q, "active");
        addDish(stall, q + "停售", "inactive");
        addDish(newStall("inactive", "active"), q, "active");
        addDish(newStall("active", "inactive"), q, "active");

        Browser browser = login(account("visitor"));
        var response = browser.call("GET", path(" " + q + " ") + "&size=2", null);
        assertThat(response.statusCode()).isEqualTo(200);
        var result = body(response).path("data");
        assertThat(result.path("total").asInt()).isEqualTo(3);
        var items = result.path("items");
        assertThat(items.size()).isEqualTo(2);
        assertThat(items.get(0).path("id").asString()).isEqualTo(exact.toString());
        assertThat(items.get(0).path("diningHallName").asString()).isEqualTo("另一食堂");
        assertThat(items.get(0).path("stallName").asString()).isEqualTo("二号档口");
        assertThat(items.get(0).path("stallId").asString()).isEqualTo(otherStall.toString());
        assertThat(items.get(0).path("price").asDouble()).isEqualTo(12.30);
        assertThat(items.get(1).path("id").asString()).isEqualTo(prefix.toString());
        assertThat(body(browser.call("GET", path(q) + "&page=2&size=2", null)).path("data")
                .path("items").get(0).path("id").asString()).isEqualTo(contains.toString());
        assertThat(browser.call("GET", "/api/v1/dishes/" + exact, null).statusCode()).isEqualTo(200);
    }

    @Test void treatsSearchCharactersLiterallyAndEnforcesAuthenticationAndBounds() throws Exception {
        String key = UUID.randomUUID().toString().substring(0, 6);
        UUID literal = addDish(stall, key + "Mix%_", "active");
        addDish(stall, key + "MixAB", "active");
        assertThat(new Browser().call("GET", path(key), null).statusCode()).isEqualTo(401);
        Browser browser = login(owner);
        var data = body(browser.call("GET", path(key + "mix%_"), null)).path("data");
        assertThat(data.path("total").asInt()).isEqualTo(1);
        assertThat(data.path("items").get(0).path("id").asString()).isEqualTo(literal.toString());
        assertThat(body(browser.call("GET", path(" "), null)).path("data").path("total").asInt()).isGreaterThan(0);
        assertThat(browser.call("GET", path("字".repeat(51)), null).statusCode()).isEqualTo(400);
        assertThat(browser.call("GET", path(key) + "&size=101", null).statusCode()).isEqualTo(400);
        assertThat(browser.call("GET", path(key) + "&page=0", null).statusCode()).isEqualTo(400);
    }
}
