package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DishSearchFiltersIT extends IntegrationApiSupport {
    private UUID addDish(String name, BigDecimal price) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.dishes(id,stall_id,name,price,status) VALUES (?, ?, ?, ?, 'active')",
                id, stall, name, price);
        return id;
    }

    private void rate(UUID dishId, int rating) {
        jdbc.update("INSERT INTO foodtime.dish_reviews(id,dish_id,user_id,rating,content) VALUES (?, ?, ?, ?, '测试评分')",
                UUID.randomUUID(), dishId, owner.id(), rating);
    }

    private String path(String q, String extra) {
        return "/api/v1/dishes/search?q=" + URLEncoder.encode(q, StandardCharsets.UTF_8) + extra;
    }

    @Test
    void retainsUnknownValuesAndOrdersThemAfterCompleteRows() throws Exception {
        String key = "筛" + UUID.randomUUID().toString().substring(0, 6);
        UUID complete = addDish(key + "完整", new BigDecimal("20.00"));
        rate(complete, 4);
        UUID unknownPrice = addDish(key + "未知价格", null);
        rate(unknownPrice, 4);
        UUID unknownRating = addDish(key + "未知评分", new BigDecimal("20.00"));
        UUID lowRating = addDish(key + "低评分", new BigDecimal("20.00"));
        rate(lowRating, 2);

        Browser browser = login(owner);
        var data = body(browser.call("GET", path(key, "&sort=price_asc"), null)).path("data");
        assertThat(data.path("total").asInt()).isEqualTo(4);
        var items = data.path("items");
        assertThat(items.get(0).path("id").asString()).isIn(complete.toString(), lowRating.toString());
        assertThat(items.get(1).path("id").asString()).isIn(complete.toString(), lowRating.toString());
        // Missing-layer rows follow; within the layer a known price precedes an unknown price.
        assertThat(items.get(2).path("id").asString()).isEqualTo(unknownRating.toString());
        assertThat(items.get(3).path("id").asString()).isEqualTo(unknownPrice.toString());
        assertThat(items.get(3).path("price").isNull()).isTrue();
        assertThat(items.get(2).path("averageScore").isNull()).isTrue();
        assertThat(items.get(2).path("reviewCount").asInt()).isZero();

        var completeRow = items.get(0).path("id").asString().equals(complete.toString())
                ? items.get(0) : items.get(1);
        assertThat(completeRow.path("averageScore").asDouble()).isEqualTo(4.0);
        assertThat(completeRow.path("reviewCount").asInt()).isEqualTo(1);
    }

    @Test
    void filtersDoNotTreatUnknownPriceOrRatingAsZero() throws Exception {
        String key = "筛" + UUID.randomUUID().toString().substring(0, 6);
        UUID complete = addDish(key + "完整", new BigDecimal("20.00"));
        rate(complete, 4);
        UUID unknownPrice = addDish(key + "未知价格", null);
        rate(unknownPrice, 4);
        UUID unknownRating = addDish(key + "未知评分", new BigDecimal("20.00"));
        UUID lowRating = addDish(key + "低评分", new BigDecimal("20.00"));
        rate(lowRating, 2);

        Browser browser = login(owner);
        var rated = body(browser.call("GET", path(key, "&minRating=4.0"), null)).path("data");
        assertThat(rated.path("total").asInt()).isEqualTo(3);
        assertThat(rated.path("items").toString()).doesNotContain(lowRating.toString());
        assertThat(rated.path("items").toString()).contains(unknownRating.toString());

        // Unknown price survives a price filter; known prices outside the range are excluded.
        var priceFiltered = body(browser.call("GET", path(key, "&minPrice=21"), null)).path("data");
        assertThat(priceFiltered.path("total").asInt()).isEqualTo(1);
        assertThat(priceFiltered.path("items").get(0).path("id").asString()).isEqualTo(unknownPrice.toString());

        var zero = body(browser.call("GET", path(key, "&minPrice=0&maxPrice=0"), null)).path("data");
        assertThat(zero.path("total").asInt()).isEqualTo(1);
        assertThat(zero.path("items").get(0).path("id").asString()).isEqualTo(unknownPrice.toString());
    }

    @Test
    void filtersByHallAndRejectsInvalidBoundsSorts() throws Exception {
        String key = "筛" + UUID.randomUUID().toString().substring(0, 6);
        addDish(key + "本食堂", new BigDecimal("10.00"));
        Browser browser = login(owner);
        assertThat(body(browser.call("GET", path(key,
                "&diningHallIds=" + hall), null)).path("data").path("total").asInt()).isEqualTo(1);
        assertThat(body(browser.call("GET", path(key,
                "&diningHallIds=" + UUID.randomUUID()), null)).path("data").path("total").asInt()).isZero();
        assertThat(browser.call("GET", path(key, "&minPrice=20&maxPrice=10"), null).statusCode()).isEqualTo(400);
        assertThat(browser.call("GET", path(key, "&minRating=5.1"), null).statusCode()).isEqualTo(400);
        assertThat(browser.call("GET", path(key, "&sort=name"), null).statusCode()).isEqualTo(400);
    }
}
