package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserSearchIT extends IntegrationApiSupport {
    private String key() { return "搜" + UUID.randomUUID().toString().substring(0, 8); }
    private Account named(String name) {
        Account account = account("user");
        jdbc.update("UPDATE foodtime.users SET display_name=? WHERE id=?", name, account.id());
        return account;
    }
    private String path(String query) { return "/api/v1/users/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8); }

    @Test void searchesPublicNicknamesAndReturnsViewerFollowState() throws Exception {
        String q = key();
        Account exact = named(q), prefix = named(q + "同学");
        named("同学" + q);
        Account disabled = named(q + "停用");
        jdbc.update("UPDATE foodtime.users SET status='disabled' WHERE id=?", disabled.id());
        jdbc.update("UPDATE foodtime.users SET display_name=? WHERE id=?", q, owner.id());
        jdbc.update("INSERT INTO foodtime.user_follows(follower_id,following_id) VALUES (?,?)", owner.id(), exact.id());
        Browser browser = login(owner);
        var response = browser.call("GET", path("  " + q + "  "), null);
        assertThat(response.statusCode()).isEqualTo(200);
        var data = body(response).path("data");
        assertThat(data.path("total").asLong()).isEqualTo(3);
        assertThat(data.path("items").get(0).path("id").asString()).isEqualTo(exact.id().toString());
        assertThat(data.path("items").get(1).path("id").asString()).isEqualTo(prefix.id().toString());
        assertThat(data.path("items").get(0).path("followedByMe").asBoolean()).isTrue();
        List<String> fields = new ArrayList<>();
        data.path("items").get(0).propertyNames().forEach(fields::add);
        assertThat(fields).containsExactlyInAnyOrder("id", "displayName", "avatarUrl", "followedByMe");
        assertThat(body(browser.call("GET", path(q) + "&page=2&size=1", null)).path("data").path("items").get(0).path("id").asString())
                .isEqualTo(prefix.id().toString());
    }

    @Test void caseIsIgnoredAndWildcardCharactersAreLiteral() throws Exception {
        String q = key();
        Account match = named(q + "MiXeD%_!");
        named(q + "mixed-other");
        var data = body(login(owner).call("GET", path(q + "mixed%_!"), null)).path("data");
        assertThat(data.path("total").asLong()).isEqualTo(1);
        assertThat(data.path("items").get(0).path("id").asString()).isEqualTo(match.id().toString());
    }

    @Test void rejectsAnonymousAndInvalidRequestsButDoesNotEnumerateOnBlank() throws Exception {
        assertThat(new Browser().call("GET", path("同学"), null).statusCode()).isEqualTo(401);
        Browser browser = login(owner);
        assertThat(body(browser.call("GET", path("  "), null)).path("data").path("items").size()).isZero();
        assertThat(browser.call("GET", path("字".repeat(51)), null).statusCode()).isEqualTo(400);
        assertThat(browser.call("GET", path("同学") + "&page=0", null).statusCode()).isEqualTo(400);
        assertThat(browser.call("GET", path("同学") + "&size=101", null).statusCode()).isEqualTo(400);
    }

    @Test void resultsUseExistingFollowAndPrivateMessageInterfaces() throws Exception {
        String q = key();
        Account target = named(q);
        Browser browser = login(owner);
        String id = body(browser.call("GET", path(q), null)).path("data").path("items").get(0).path("id").asString();
        assertThat(browser.call("PUT", "/api/v1/users/" + id + "/follow", null).statusCode()).isEqualTo(200);
        assertThat(body(browser.call("GET", path(q), null)).path("data").path("items").get(0).path("followedByMe").asBoolean()).isTrue();
        assertThat(body(browser.call("POST", "/api/v1/users/me/conversations", Map.of("userId", id))).path("data").path("other").path("id").asString())
                .isEqualTo(target.id().toString());
    }

    @Test void visitorsCanSearchButExistingWriteRestrictionsStillApply() throws Exception {
        String q = key();
        Account target = named(q);
        Browser visitor = login(account("visitor"));
        assertThat(body(visitor.call("GET", path(q), null)).path("data").path("total").asLong()).isEqualTo(1);
        assertThat(visitor.call("PUT", "/api/v1/users/" + target.id() + "/follow", null).statusCode()).isEqualTo(403);
        assertThat(visitor.call("POST", "/api/v1/users/me/conversations", Map.of("userId", target.id().toString())).statusCode()).isEqualTo(403);
    }
}
