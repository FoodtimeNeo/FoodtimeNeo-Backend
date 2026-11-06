package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 1:1 private messaging: membership, unread, idempotency, hide/revive and visitor read-only. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DirectMessageIT extends IntegrationApiSupport {

    @Test
    void sendListsUnreadAndMarksRead() throws Exception {
        Browser author = login(owner), friend = login(other);
        UUID conversation = start(author, other);
        var sent = author.call("POST", "/api/v1/users/me/conversations/" + conversation + "/messages",
                Map.of("content", "  今晚吃什么？  ", "clientRequestId", UUID.randomUUID()));
        assertThat(sent.statusCode()).isEqualTo(200);
        assertThat(body(sent).path("data").path("content").asString()).isEqualTo("今晚吃什么？");

        var list = friend.call("GET", "/api/v1/users/me/conversations", null);
        var row = body(list).path("data").path("items").get(0);
        assertThat(row.path("id").asString()).isEqualTo(conversation.toString());
        assertThat(row.path("other").path("id").asString()).isEqualTo(owner.id().toString());
        assertThat(row.path("lastMessage").asString()).isEqualTo("今晚吃什么？");
        assertThat(row.path("unreadCount").asLong()).isEqualTo(1);
        assertThat(body(friend.call("GET", "/api/v1/users/me/conversations/unread-count", null))
                .path("data").path("unreadCount").asLong()).isEqualTo(1);
        assertThat(body(author.call("GET", "/api/v1/users/me/conversations/unread-count", null))
                .path("data").path("unreadCount").asLong()).isZero();

        var messages = friend.call("GET", "/api/v1/users/me/conversations/" + conversation + "/messages", null);
        assertThat(body(messages).path("data").path("items")).hasSize(1);
        assertThat(friend.call("POST", "/api/v1/users/me/conversations/" + conversation + "/read", null)
                .statusCode()).isEqualTo(200);
        assertThat(body(friend.call("GET", "/api/v1/users/me/conversations/unread-count", null))
                .path("data").path("unreadCount").asLong()).isZero();
    }

    @Test
    void repeatedClientRequestIdReturnsTheSameMessage() throws Exception {
        Browser author = login(owner);
        UUID conversation = start(author, other);
        UUID request = UUID.randomUUID();
        var first = author.call("POST", "/api/v1/users/me/conversations/" + conversation + "/messages",
                Map.of("content", "重试消息", "clientRequestId", request));
        var second = author.call("POST", "/api/v1/users/me/conversations/" + conversation + "/messages",
                Map.of("content", "重试消息", "clientRequestId", request));
        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(second.statusCode()).isEqualTo(200);
        assertThat(body(second).path("data").path("id").asString())
                .isEqualTo(body(first).path("data").path("id").asString());
    }

    @Test
    void hiddenConversationReturnsWhenTheOtherSideSendsAgain() throws Exception {
        Browser author = login(owner), friend = login(other);
        UUID conversation = start(author, other);
        author.call("POST", "/api/v1/users/me/conversations/" + conversation + "/messages",
                Map.of("content", "先发一条", "clientRequestId", UUID.randomUUID()));
        assertThat(author.call("DELETE", "/api/v1/users/me/conversations/" + conversation, null)
                .statusCode()).isEqualTo(200);
        assertThat(body(author.call("GET", "/api/v1/users/me/conversations", null))
                .path("data").path("items")).isEmpty();

        friend.call("POST", "/api/v1/users/me/conversations/" + conversation + "/messages",
                Map.of("content", "我回来了", "clientRequestId", UUID.randomUUID()));
        var row = body(author.call("GET", "/api/v1/users/me/conversations", null))
                .path("data").path("items").get(0);
        assertThat(row.path("lastMessage").asString()).isEqualTo("我回来了");
        assertThat(row.path("unreadCount").asLong()).isEqualTo(1);
    }

    @Test
    void visitorsCanReadButNotSendMessages() throws Exception {
        Browser author = login(owner);
        Account guestAccount = account("visitor");
        Browser guest = login(guestAccount);
        UUID conversation = start(author, guestAccount);
        author.call("POST", "/api/v1/users/me/conversations/" + conversation + "/messages",
                Map.of("content", "欢迎", "clientRequestId", UUID.randomUUID()));

        assertThat(guest.call("GET", "/api/v1/users/me/conversations", null).statusCode()).isEqualTo(200);
        assertThat(guest.call("POST", "/api/v1/users/me/conversations/" + conversation + "/read", null)
                .statusCode()).isEqualTo(200);
        var denied = guest.call("POST", "/api/v1/users/me/conversations/" + conversation + "/messages",
                Map.of("content", "访客回复", "clientRequestId", UUID.randomUUID()));
        assertThat(denied.statusCode()).isEqualTo(403);
        assertThat(body(denied).path("code").asString()).isEqualTo("VISITOR_READ_ONLY");
    }

    @Test
    void outsidersAndSelfConversationsAreRejected() throws Exception {
        Browser author = login(owner);
        UUID conversation = start(author, other);
        Browser outsider = login(account("user"));
        assertThat(outsider.call("GET", "/api/v1/users/me/conversations/" + conversation + "/messages", null)
                .statusCode()).isEqualTo(404);
        assertThat(author.call("POST", "/api/v1/users/me/conversations",
                Map.of("userId", owner.id())).statusCode()).isEqualTo(400);
    }

    private UUID start(Browser browser, Account other) throws Exception {
        var response = browser.call("POST", "/api/v1/users/me/conversations", Map.of("userId", other.id()));
        assertThat(response.statusCode()).isEqualTo(200);
        return UUID.fromString(body(response).path("data").path("id").asString());
    }
}
