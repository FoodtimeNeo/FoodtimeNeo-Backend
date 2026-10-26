package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 公告 / 信箱集成测试：发布 → feed 可见 → 已读幂等 → unread 归零，
 * since_seq 增量、分页校验、messages 读写与隔离、删除后 feed 消失。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NoticeInboxIT extends IntegrationApiSupport {

    @AfterEach
    void cleanNoticesAndMessages() {
        // 子类 AfterEach 先于基类执行，提前删掉引用 users 的行，避免基类删用户时触碰外键。
        for (UUID user : users) {
            jdbc.update("DELETE FROM foodtime.notice_reads WHERE user_id = ?", user);
            jdbc.update("DELETE FROM foodtime.messages WHERE user_id = ?", user);
            jdbc.update("DELETE FROM foodtime.notices WHERE created_by = ?", user);
        }
    }

    @Test
    void anonymousRequestsRequireLogin() throws Exception {
        HttpClient anonymous = HttpClient.newHttpClient();
        for (String path : List.of(
                "/api/v1/users/me/notices/feed",
                "/api/v1/users/me/notices",
                "/api/v1/users/me/messages",
                "/api/v1/users/me/messages/unread-count")) {
            var response = anonymous.send(HttpRequest.newBuilder(
                            URI.create("http://127.0.0.1:" + port + path)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(path).isEqualTo(401);
            assertThat(body(response).path("code").asString()).as(path).isEqualTo("UNAUTHENTICATED");
        }
        // 管理端路径对匿名请求同样先被会话校验拦下（401），与登录态无关
        var adminDenied = anonymous.send(HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + port + "/api/v1/admin/notices")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(adminDenied.statusCode()).isEqualTo(401);
        assertThat(body(adminDenied).path("code").asString()).isEqualTo("UNAUTHENTICATED");
        // 无 CSRF 令牌的写请求在鉴权前被拒绝（403）
        var noCsrf = anonymous.send(HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + port + "/api/v1/users/me/notices/" + UUID.randomUUID() + "/read"))
                        .POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(noCsrf.statusCode()).isEqualTo(403);
        assertThat(body(noCsrf).path("code").asString()).isEqualTo("CSRF_INVALID");
    }

    @Test
    void ordinaryUserCannotUseAdminNoticeEndpoints() throws Exception {
        Browser user = login(owner);
        assertThat(user.call("GET", "/api/v1/admin/notices", null).statusCode()).isEqualTo(403);
        var denied = user.call("POST", "/api/v1/admin/notices", Map.of("title", "x", "body", "y"));
        assertThat(denied.statusCode()).isEqualTo(403);
        assertThat(body(denied).path("code").asString()).isEqualTo("ACCESS_DENIED");
    }

    @Test
    void publishFeedReadCycleEndsWithZeroUnread() throws Exception {
        Browser admin = login(account("admin"));
        Browser user = login(owner);
        UUID id = publish(admin, "开学通知", "欢迎返校");

        var feed = body(user.call("GET", "/api/v1/users/me/notices/feed?since_seq=0&limit=20", null));
        assertThat(feed.path("data").path("unreadCount").asLong()).isEqualTo(1);
        assertThat(ids(feed.path("data").path("items"))).contains(id.toString());

        var list = body(user.call("GET", "/api/v1/users/me/notices?page=1&size=20", null));
        assertThat(list.path("data").path("total").asLong()).isEqualTo(1);
        assertThat(ids(list.path("data").path("items"))).contains(id.toString());

        assertThat(user.call("POST", "/api/v1/users/me/notices/" + id + "/read", Map.of()).statusCode()).isEqualTo(200);
        // 已读幂等：重复调用同样成功
        assertThat(user.call("POST", "/api/v1/users/me/notices/" + id + "/read", Map.of()).statusCode()).isEqualTo(200);

        var after = body(user.call("GET", "/api/v1/users/me/notices/feed?since_seq=0&limit=20", null));
        assertThat(after.path("data").path("unreadCount").asLong()).isZero();
        assertThat(ids(after.path("data").path("items"))).contains(id.toString());

        // 不存在的公告返回 404
        var missing = user.call("POST", "/api/v1/users/me/notices/" + UUID.randomUUID() + "/read", Map.of());
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(body(missing).path("code").asString()).isEqualTo("NOTICE_NOT_FOUND");
    }

    @Test
    void sinceSeqOnlyReturnsNewerNoticesInAscendingOrder() throws Exception {
        Browser admin = login(account("admin"));
        Browser user = login(owner);
        UUID first = publish(admin, "第一条", "one");
        UUID second = publish(admin, "第二条", "two");
        UUID third = publish(admin, "第三条", "three");

        var all = body(user.call("GET", "/api/v1/users/me/notices/feed?since_seq=0&limit=100", null));
        assertThat(ids(all.path("data").path("items")))
                .containsExactly(first.toString(), second.toString(), third.toString());

        long seqOfFirst = jdbc.queryForObject("SELECT seq FROM foodtime.notices WHERE id = ?", Long.class, first);
        var incremental = body(user.call(
                "GET", "/api/v1/users/me/notices/feed?since_seq=" + seqOfFirst + "&limit=100", null));
        assertThat(ids(incremental.path("data").path("items")))
                .containsExactly(second.toString(), third.toString());

        long maxSeq = jdbc.queryForObject("SELECT MAX(seq) FROM foodtime.notices", Long.class);
        var empty = body(user.call(
                "GET", "/api/v1/users/me/notices/feed?since_seq=" + maxSeq + "&limit=100", null));
        assertThat(empty.path("data").path("items").size()).isZero();
    }

    @Test
    void invalidPagingAndPayloadAreRejected() throws Exception {
        Browser user = login(owner);
        for (String path : List.of(
                "/api/v1/users/me/notices?page=0",
                "/api/v1/users/me/notices?size=101",
                "/api/v1/users/me/notices/feed?limit=0",
                "/api/v1/users/me/notices/feed?limit=101",
                "/api/v1/users/me/notices/feed?since_seq=-1",
                "/api/v1/users/me/messages?page=0",
                "/api/v1/users/me/messages?size=101")) {
            var response = user.call("GET", path, null);
            assertThat(response.statusCode()).as(path).isEqualTo(400);
            assertThat(body(response).path("code").asString()).as(path).isEqualTo("VALIDATION_ERROR");
        }
        Browser admin = login(account("admin"));
        assertThat(admin.call("POST", "/api/v1/admin/notices", Map.of("title", " ", "body", "x")).statusCode())
                .isEqualTo(400);
        assertThat(admin.call("POST", "/api/v1/admin/notices", Map.of("title", "t")).statusCode()).isEqualTo(400);
        assertThat(admin.call("GET", "/api/v1/admin/notices?page=0", null).statusCode()).isEqualTo(400);
    }

    @Test
    void messagesAreReadPerUserAndIsolatedAcrossUsers() throws Exception {
        Browser user = login(owner);
        UUID mine = insertMessage(owner.id(), "欢迎", "欢迎来到食时");
        UUID theirs = insertMessage(other.id(), "他人", "与你无关");

        var list = body(user.call("GET", "/api/v1/users/me/messages?page=1&size=20", null));
        assertThat(list.path("data").path("total").asLong()).isEqualTo(1);
        assertThat(list.path("data").path("items").get(0).path("id").asString()).isEqualTo(mine.toString());
        assertThat(list.path("data").path("items").get(0).path("isRead").asBoolean()).isFalse();
        assertThat(unreadCount(user)).isEqualTo(1);

        // 非本人消息按 404 处理，不泄露归属
        var cross = user.call("POST", "/api/v1/users/me/messages/" + theirs + "/read", Map.of());
        assertThat(cross.statusCode()).isEqualTo(404);
        assertThat(body(cross).path("code").asString()).isEqualTo("MESSAGE_NOT_FOUND");
        var unknown = user.call("POST", "/api/v1/users/me/messages/" + UUID.randomUUID() + "/read", Map.of());
        assertThat(unknown.statusCode()).isEqualTo(404);

        // 单条已读幂等，已读消息仍保留在列表中
        assertThat(user.call("POST", "/api/v1/users/me/messages/" + mine + "/read", Map.of()).statusCode())
                .isEqualTo(200);
        assertThat(user.call("POST", "/api/v1/users/me/messages/" + mine + "/read", Map.of()).statusCode())
                .isEqualTo(200);
        assertThat(unreadCount(user)).isZero();
        var kept = body(user.call("GET", "/api/v1/users/me/messages?page=1&size=20", null));
        assertThat(kept.path("data").path("total").asLong()).isEqualTo(1);
        assertThat(kept.path("data").path("items").get(0).path("isRead").asBoolean()).isTrue();

        // read-all 覆盖多条，且只影响自己
        insertMessage(owner.id(), "第二封", "two");
        insertMessage(owner.id(), "第三封", "three");
        assertThat(unreadCount(user)).isEqualTo(2);
        assertThat(user.call("POST", "/api/v1/users/me/messages/read-all", Map.of()).statusCode()).isEqualTo(200);
        assertThat(unreadCount(user)).isZero();
        Browser stranger = login(other);
        assertThat(body(stranger.call("GET", "/api/v1/users/me/messages?page=1&size=20", null))
                .path("data").path("total").asLong()).isEqualTo(1);
        assertThat(unreadCount(stranger)).isEqualTo(1);
    }

    @Test
    void deletedNoticeDisappearsFromFeed() throws Exception {
        Browser admin = login(account("admin"));
        Browser user = login(owner);
        UUID id = publish(admin, "临时通知", "稍后删除");
        assertThat(ids(body(user.call("GET", "/api/v1/users/me/notices/feed?since_seq=0&limit=20", null))
                .path("data").path("items"))).contains(id.toString());

        assertThat(admin.call("DELETE", "/api/v1/admin/notices/" + id, null).statusCode()).isEqualTo(200);

        assertThat(ids(body(user.call("GET", "/api/v1/users/me/notices/feed?since_seq=0&limit=20", null))
                .path("data").path("items"))).doesNotContain(id.toString());
        assertThat(admin.call("GET", "/api/v1/admin/notices/" + id, null).statusCode()).isEqualTo(404);
        assertThat(admin.call("DELETE", "/api/v1/admin/notices/" + id, null).statusCode()).isEqualTo(404);
    }

    @Test
    void editingNoticeKeepsSeqAndReadState() throws Exception {
        Browser admin = login(account("admin"));
        Browser user = login(owner);
        UUID id = publish(admin, "旧标题", "旧正文");
        long seqBefore = jdbc.queryForObject("SELECT seq FROM foodtime.notices WHERE id = ?", Long.class, id);
        assertThat(user.call("POST", "/api/v1/users/me/notices/" + id + "/read", Map.of()).statusCode())
                .isEqualTo(200);

        var updated = admin.call("PUT", "/api/v1/admin/notices/" + id, Map.of("title", "新标题", "body", "新正文"));
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(body(updated).path("data").path("title").asString()).isEqualTo("新标题");

        assertThat(jdbc.queryForObject("SELECT seq FROM foodtime.notices WHERE id = ?", Long.class, id))
                .isEqualTo(seqBefore);
        var feed = body(user.call("GET", "/api/v1/users/me/notices/feed?since_seq=0&limit=20", null));
        assertThat(feed.path("data").path("unreadCount").asLong()).isZero();
        assertThat(feed.path("data").path("items").get(0).path("title").asString()).isEqualTo("新标题");
    }

    private UUID publish(Browser admin, String title, String bodyText) throws Exception {
        var created = admin.call("POST", "/api/v1/admin/notices", Map.of("title", title, "body", bodyText));
        assertThat(created.statusCode()).isEqualTo(201);
        return UUID.fromString(body(created).path("data").path("id").asString());
    }

    private UUID insertMessage(UUID userId, String title, String bodyText) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.messages(id, user_id, title, body) VALUES (?, ?, ?, ?)",
                id, userId, title, bodyText);
        return id;
    }

    private long unreadCount(Browser user) throws Exception {
        return body(user.call("GET", "/api/v1/users/me/messages/unread-count", null))
                .path("data").path("unreadCount").asLong();
    }

    private List<String> ids(tools.jackson.databind.JsonNode items) {
        var result = new ArrayList<String>();
        for (var item : items) {
            result.add(item.path("id").asString());
        }
        return result;
    }
}
