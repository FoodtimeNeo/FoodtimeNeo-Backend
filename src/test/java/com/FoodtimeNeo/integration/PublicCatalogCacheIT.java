package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import com.FoodtimeNeo.dish.mapper.DishDetailMapper;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PublicCatalogCacheIT extends IntegrationApiSupport {
    @Autowired PlatformTransactionManager manager;
    @MockitoSpyBean DishDetailMapper details;

    @Test void revalidatesPublicDataButNeverBypassesAuthenticationOrVisibility() throws Exception {
        Browser browser = login(owner);
        String path = dishPath("");
        clearInvocations(details);
        var first = browser.call("GET", path, null);
        assertThat(first.statusCode()).isEqualTo(200);
        String etag = first.headers().firstValue("ETag").orElseThrow();
        assertThat(first.headers().firstValue("X-Foodtime-Public-Catalog")).contains("1");
        var unchanged = browser.imageIfNoneMatch(path, etag);
        assertThat(unchanged.statusCode()).isEqualTo(304);
        assertThat(unchanged.body()).isEmpty();
        verify(details, times(1)).findVisible(dish);
        assertThat(new Browser().imageIfNoneMatch(path, etag).statusCode()).isEqualTo(401);

        jdbc.update("UPDATE foodtime.dishes SET price=18 WHERE id=?", dish);
        var changed = browser.imageIfNoneMatch(path, etag);
        assertThat(changed.statusCode()).isEqualTo(200);
        assertThat(json.readTree(changed.body()).path("data").path("price").asInt()).isEqualTo(18);
        String current = changed.headers().firstValue("ETag").orElseThrow();
        jdbc.update("UPDATE foodtime.dining_halls SET status='inactive' WHERE id=?", hall);
        assertThat(browser.imageIfNoneMatch(path, current).statusCode()).isEqualTo(404);
    }

    @Test void revisionAndDataRollbackTogetherAndAggregatesInvalidateTheirPublicProjections() throws Exception {
        Browser browser = login(owner);
        String path = "/api/v1/stalls/" + stall + "/dishes";
        var first = browser.call("GET", path, null);
        String etag = first.headers().firstValue("ETag").orElseThrow();
        long revision = jdbc.queryForObject("SELECT revision FROM foodtime.public_catalog_revision", Long.class);
        new TransactionTemplate(manager).execute(status -> {
            jdbc.update("UPDATE foodtime.dishes SET name='rolled back' WHERE id=?", dish);
            status.setRollbackOnly();
            return null;
        });
        assertThat(jdbc.queryForObject("SELECT revision FROM foodtime.public_catalog_revision", Long.class)).isEqualTo(revision);
        assertThat(browser.imageIfNoneMatch(path, etag).statusCode()).isEqualTo(304);
        formal(owner, 5);
        var updated = browser.imageIfNoneMatch(path, etag);
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(json.readTree(updated.body()).path("data").get(0).path("reviewCount").asInt()).isEqualTo(1);
        assertThat(browser.call("GET", "/api/v1/users/me/profile", null).headers()
                .firstValue("X-Foodtime-Public-Catalog")).isEmpty();
        assertThat(browser.call("GET", "/api/v1/dishes/search?page=0", null).statusCode()).isEqualTo(400);
    }
}
