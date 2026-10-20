package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserAdminIT extends IntegrationApiSupport {
    @Test
    void onlyPlatformAdministratorsCanReadAndWriteUserAccess() throws Exception {
        for (var actor : new Account[]{owner,account("admin")}) {
            Browser browser = login(actor);
            assertThat(browser.call("GET","/api/v1/admin/users",null).statusCode()).isEqualTo(403);
            assertThat(browser.call("PUT","/api/v1/admin/users/"+other.id(),Map.of("role","admin","status","disabled")).statusCode()).isEqualTo(403);
        }
        assertThat(jdbc.queryForObject("SELECT role FROM foodtime.users WHERE id=?",String.class,other.id())).isEqualTo("user");
    }

    @Test
    void listUsesRealIdentityCreationTimeAndFormalReviewCountWithoutSecrets() throws Exception {
        formal(owner,5);
        Browser admin = login(account("superadmin"));
        var response = admin.call("GET","/api/v1/admin/users?q="+owner.email()+"&size=1",null);
        assertThat(response.statusCode()).isEqualTo(200);
        var data = body(response).path("data");
        assertThat(data.path("total").asLong()).isEqualTo(1);
        assertThat(data.path("items").get(0).path("id").asString()).isEqualTo(owner.id().toString());
        assertThat(data.path("items").get(0).path("reviewCount").asLong()).isEqualTo(1);
        assertThat(data.path("items").get(0).path("createdAt").asString()).isNotEmpty();
        assertThat(response.body()).doesNotContain("password","hash","emailVerified","passwordChanged");
        assertThat(body(admin.call("GET","/api/v1/admin/users?q=%25",null)).path("data").path("total").asLong()).isZero();
    }

    @Test
    void rolesAreRecheckedInExistingSessionsAndPasswordsCannotBeChanged() throws Exception {
        Browser platform = login(account("superadmin")), student = login(owner);
        String hash = jdbc.queryForObject("SELECT password_hash FROM foodtime.users WHERE id=?",String.class,owner.id());
        assertThat(platform.call("PUT","/api/v1/admin/users/"+owner.id(),Map.of("role","admin","status","active","password","tampered","email","changed@example.com")).statusCode()).isEqualTo(200);
        assertThat(student.call("GET","/api/v1/admin/catalog/dining-halls",null).statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT password_hash FROM foodtime.users WHERE id=?",String.class,owner.id())).isEqualTo(hash);
        assertThat(jdbc.queryForObject("SELECT email FROM foodtime.users WHERE id=?",String.class,owner.id())).isEqualTo(owner.email());
        assertThat(platform.call("PUT","/api/v1/admin/users/"+owner.id(),Map.of("role","user","status","active")).statusCode()).isEqualTo(200);
        assertThat(student.call("GET","/api/v1/admin/catalog/dining-halls",null).statusCode()).isEqualTo(403);
    }

    @Test
    void disabledAccountsLoseSessionsAndCannotLoginUntilReenabled() throws Exception {
        Browser platform = login(account("superadmin")), student = login(owner);
        assertThat(platform.call("PUT","/api/v1/admin/users/"+owner.id(),Map.of("role","user","status","disabled")).statusCode()).isEqualTo(200);
        assertThat(student.call("GET","/api/v1/users/me/profile",null).statusCode()).isEqualTo(401);
        Browser fresh = new Browser(); fresh.csrf();
        assertThat(fresh.call("POST","/api/v1/auth/login",Map.of("account",owner.email(),"password",PASSWORD)).statusCode()).isEqualTo(401);
        assertThat(platform.call("PUT","/api/v1/admin/users/"+owner.id(),Map.of("role","user","status","active")).statusCode()).isEqualTo(200);
        assertThat(student.call("GET","/api/v1/users/me/profile",null).statusCode()).isEqualTo(401);
        login(owner);
    }

    @Test
    void selfChangesUnknownRolesAndUnknownUsersAreRejected() throws Exception {
        Account actor = account("superadmin"); Browser platform = login(actor);
        assertThat(platform.call("PUT","/api/v1/admin/users/"+actor.id(),Map.of("role","user","status","disabled")).statusCode()).isEqualTo(409);
        assertThat(platform.call("PUT","/api/v1/admin/users/"+owner.id(),Map.of("role","operator","status","active")).statusCode()).isEqualTo(400);
        assertThat(platform.call("PUT","/api/v1/admin/users/"+UUID.randomUUID(),Map.of("role","user","status","active")).statusCode()).isEqualTo(404);
        assertThat(jdbc.queryForObject("SELECT role FROM foodtime.users WHERE id=?",String.class,actor.id())).isEqualTo("superadmin");
    }
}
