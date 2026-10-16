package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.rating-summary.refresh-enabled=false")
class CatalogAdminIT extends IntegrationApiSupport {
    @Test
    void ordinaryUsersCannotReadOrModifyAdminCatalog() throws Exception {
        Browser student = login(owner);
        assertThat(student.call("GET","/api/v1/admin/catalog/dining-halls",null).statusCode()).isEqualTo(403);
        assertThat(student.call("PUT","/api/v1/admin/catalog/dishes/" + dish,Map.of("name","tampered","price",1,"role","admin")).statusCode()).isEqualTo(403);
        assertThat(jdbc.queryForObject("SELECT name FROM foodtime.dishes WHERE id = ?",String.class,dish)).isEqualTo("integration dish");
    }

    @Test
    void administratorCreatesCatalogWithRealParentKeysAndNoFloor() throws Exception {
        Browser admin = login(account("admin"));
        var hallResponse = admin.call("POST","/api/v1/admin/catalog/dining-halls",Map.of("name","created hall"));
        assertThat(hallResponse.statusCode()).isEqualTo(201);
        UUID newHall = UUID.fromString(body(hallResponse).path("data").path("id").asString()); additionalHalls.add(newHall);
        var stallResponse = admin.call("POST","/api/v1/admin/catalog/stalls",Map.of("name","created stall","parentId",newHall,"floor","fake"));
        assertThat(stallResponse.statusCode()).isEqualTo(201);
        UUID newStall = UUID.fromString(body(stallResponse).path("data").path("id").asString());
        assertThat(stallResponse.body()).doesNotContain("floor");
        var dishResponse = admin.call("POST","/api/v1/admin/catalog/dishes",Map.of("name","created dish","parentId",newStall,"price",new BigDecimal("10.20")));
        assertThat(dishResponse.statusCode()).isEqualTo(201);
        UUID newDish = UUID.fromString(body(dishResponse).path("data").path("id").asString());
        assertThat(jdbc.queryForObject("SELECT stall_id FROM foodtime.dishes WHERE id = ?",UUID.class,newDish)).isEqualTo(newStall);
        assertThat(jdbc.queryForObject("SELECT price FROM foodtime.dishes WHERE id = ?",BigDecimal.class,newDish)).isEqualByComparingTo("10.20");
    }

    @Test
    void disablingCatalogChangesPublicVisibilityAndKeepsAdminAccess() throws Exception {
        Browser admin = login(account("admin")), student = login(owner);
        assertThat(admin.call("PUT","/api/v1/admin/catalog/dining-halls/" + hall,Map.of("name","integration hall","status","inactive")).statusCode()).isEqualTo(200);
        assertThat(student.call("GET","/api/v1/dining-halls/" + hall + "/stalls",null).statusCode()).isEqualTo(404);
        var managed = admin.call("GET","/api/v1/admin/catalog/dining-halls/" + hall,null);
        assertThat(managed.statusCode()).isEqualTo(200);
        assertThat(body(managed).path("data").path("status").asString()).isEqualTo("inactive");
        assertThat(admin.call("PUT","/api/v1/admin/catalog/dishes/" + dish,Map.of("name","updated dish","price",new BigDecimal("9.80"))).statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT price FROM foodtime.dishes WHERE id = ?",BigDecimal.class,dish)).isEqualByComparingTo("9.80");
    }

    @Test
    void conflictsBadParentsAndMissingDishPriceDoNotWriteData() throws Exception {
        Browser admin = login(account("admin"));
        assertThat(admin.call("POST","/api/v1/admin/catalog/stalls",Map.of("name","integration stall","parentId",hall)).statusCode()).isEqualTo(409);
        assertThat(admin.call("POST","/api/v1/admin/catalog/stalls",Map.of("name","orphan","parentId",UUID.randomUUID())).statusCode()).isEqualTo(400);
        assertThat(admin.call("POST","/api/v1/admin/catalog/dishes",Map.of("name","no price","parentId",stall)).statusCode()).isEqualTo(400);
        assertThat(admin.call("PUT","/api/v1/admin/catalog/dishes/" + dish,Map.of("name","moving","parentId",UUID.randomUUID(),"price",1)).statusCode()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.stalls WHERE dining_hall_id = ?",Long.class,hall)).isEqualTo(1);
    }

    @Test
    void administratorsCanRemoveInappropriateReviewsWithoutChangingAuditHistory() throws Exception {
        UUID review = formal(owner,3);
        Browser admin = login(account("admin"));
        assertThat(admin.call("GET","/api/v1/admin/reviews",null).statusCode()).isEqualTo(200);
        assertThat(admin.call("DELETE","/api/v1/admin/reviews/" + review,Map.of("reason"," ")).statusCode()).isEqualTo(400);
        assertThat(admin.call("DELETE","/api/v1/admin/reviews/" + review,Map.of("reason","unrelated content")).statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.dish_reviews WHERE id = ?",Long.class,review)).isZero();
        assertThat(admin.call("DELETE","/api/v1/admin/reviews/" + review,Map.of("reason","unrelated content")).statusCode()).isEqualTo(404);
    }

    @Test
    void referencedParentsCannotBeDeletedButUnusedRecordsCan() throws Exception {
        Browser admin = login(account("admin"));
        assertThat(admin.call("DELETE","/api/v1/admin/catalog/dining-halls/" + hall,null).statusCode()).isEqualTo(409);
        assertThat(admin.call("DELETE","/api/v1/admin/catalog/stalls/" + stall,null).statusCode()).isEqualTo(409);
        assertThat(admin.call("DELETE","/api/v1/admin/catalog/dishes/" + dish,null).statusCode()).isEqualTo(200);
        assertThat(admin.call("DELETE","/api/v1/admin/catalog/stalls/" + stall,null).statusCode()).isEqualTo(200);
        assertThat(admin.call("DELETE","/api/v1/admin/catalog/dining-halls/" + hall,null).statusCode()).isEqualTo(200);
    }
}
