package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Community likes aggregate per target; approved comments/replies notify their owners. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CommunityNotificationIT extends IntegrationApiSupport {

    @Test
    void postLikesAggregateIntoOneMessageAndExcludeTheAuthor() throws Exception {
        Browser author = login(owner);
        Browser fan1 = login(other);
        Account fan2Account = account("user"), fan3Account = account("user");
        Browser fan2 = login(fan2Account), fan3 = login(fan3Account);
        rename(other, "小明");
        rename(fan2Account, "小红");
        rename(fan3Account, "小刚");
        UUID post = approvedPost(author, "聚合点赞-" + UUID.randomUUID());

        assertThat(fan1.call("PUT", "/api/v1/community/posts/" + post + "/like", Map.of()).statusCode()).isEqualTo(200);
        assertThat(fan2.call("PUT", "/api/v1/community/posts/" + post + "/like", Map.of()).statusCode()).isEqualTo(200);
        assertThat(fan3.call("PUT", "/api/v1/community/posts/" + post + "/like", Map.of()).statusCode()).isEqualTo(200);
        // 作者自己点赞不进入通知人数。
        assertThat(author.call("PUT", "/api/v1/community/posts/" + post + "/like", Map.of()).statusCode()).isEqualTo(200);

        JsonNode message = only(messages(author), "LIKE_POST");
        assertThat(message.path("actorCount").asInt()).isEqualTo(3);
        assertThat(message.path("refType").asString()).isEqualTo("community-post");
        assertThat(message.path("refId").asString()).isEqualTo(post.toString());
        assertThat(message.path("refRootCommentId").isNull()).isTrue();
        assertThat(message.path("body").asString()).contains("总计 3 人赞了你的食记");
        assertThat(message.path("body").asString()).contains("小刚").contains("小红");

        assertThat(fan1.call("DELETE", "/api/v1/community/posts/" + post + "/like", null).statusCode()).isEqualTo(200);
        JsonNode afterUnlike = only(messages(author), "LIKE_POST");
        assertThat(afterUnlike.path("actorCount").asInt()).isEqualTo(2);
        assertThat(afterUnlike.path("body").asString()).contains("赞了你的食记");
    }

    @Test
    void selfLikeOnFreshPostCreatesNoMessage() throws Exception {
        Browser author = login(owner);
        UUID post = approvedPost(author, "自赞-" + UUID.randomUUID());
        assertThat(author.call("PUT", "/api/v1/community/posts/" + post + "/like", Map.of()).statusCode()).isEqualTo(200);
        boolean hasLike = false;
        for (JsonNode item : messages(author)) {
            if ("LIKE_POST".equals(item.path("category").asString())) hasLike = true;
        }
        assertThat(hasLike).isFalse();
    }

    @Test
    void commentLikesAggregateAndKeepTheThreadRootForFocus() throws Exception {
        Browser author = login(owner), commenter = login(other);
        Browser fan1 = login(account("user")), fan2 = login(account("user"));
        UUID post = approvedPost(author, "评论点赞-" + UUID.randomUUID());
        var created = commenter.call("POST", "/api/v1/community/posts/" + post + "/comments",
                Map.of("clientRequestId", UUID.randomUUID(), "content", "值得一试-" + UUID.randomUUID()));
        assertThat(created.statusCode()).isEqualTo(202);
        UUID comment = UUID.fromString(body(created).path("data").path("id").asString());
        approveComment(comment);

        assertThat(fan1.call("PUT", "/api/v1/community/comments/" + comment + "/like", Map.of())
                .statusCode()).isEqualTo(200);
        assertThat(fan2.call("PUT", "/api/v1/community/comments/" + comment + "/like", Map.of())
                .statusCode()).isEqualTo(200);

        JsonNode message = only(messages(commenter), "LIKE_COMMENT");
        assertThat(message.path("actorCount").asInt()).isEqualTo(2);
        assertThat(message.path("refId").asString()).isEqualTo(post.toString());
        assertThat(message.path("refCommentId").asString()).isEqualTo(comment.toString());
        assertThat(message.path("refRootCommentId").asString()).isEqualTo(comment.toString());
        assertThat(message.path("body").asString()).contains("赞了你的评论");
    }

    @Test
    void approvedRootCommentAndReplyNotifyTheRightOwner() throws Exception {
        Browser author = login(owner), commenter = login(other);
        Account replierAccount = account("user");
        Browser replier = login(replierAccount);
        rename(other, "评论者");
        rename(replierAccount, "回复者");
        UUID post = approvedPost(author, "评论通知-" + UUID.randomUUID());

        String rootContent = "这条食记真不错-" + UUID.randomUUID();
        var created = commenter.call("POST", "/api/v1/community/posts/" + post + "/comments",
                Map.of("clientRequestId", UUID.randomUUID(), "content", rootContent));
        assertThat(created.statusCode()).isEqualTo(202);
        UUID root = UUID.fromString(body(created).path("data").path("id").asString());
        approveComment(root);

        JsonNode rootMessage = only(messages(author), "COMMENT_POST");
        assertThat(rootMessage.path("refId").asString()).isEqualTo(post.toString());
        assertThat(rootMessage.path("refCommentId").asString()).isEqualTo(root.toString());
        assertThat(rootMessage.path("refRootCommentId").asString()).isEqualTo(root.toString());
        assertThat(rootMessage.path("body").asString()).contains("评论者").contains(rootContent);

        String replyContent = "我也觉得-" + UUID.randomUUID();
        var reply = replier.call("POST", "/api/v1/community/posts/" + post + "/comments",
                Map.of("clientRequestId", UUID.randomUUID(), "content", replyContent,
                        "rootCommentId", root.toString(), "replyToCommentId", root.toString()));
        assertThat(reply.statusCode()).isEqualTo(202);
        UUID replyId = UUID.fromString(body(reply).path("data").path("id").asString());
        approveComment(replyId);

        JsonNode replyMessage = only(messages(commenter), "REPLY_COMMENT");
        assertThat(replyMessage.path("refId").asString()).isEqualTo(post.toString());
        assertThat(replyMessage.path("refCommentId").asString()).isEqualTo(replyId.toString());
        assertThat(replyMessage.path("refRootCommentId").asString()).isEqualTo(root.toString());
        assertThat(replyMessage.path("body").asString()).contains("回复者").contains("回复你").contains(replyContent);
    }

    private void rename(Account account, String name) {
        jdbc.update("UPDATE foodtime.users SET display_name=? WHERE id=?", name, account.id());
    }

    private UUID approvedPost(Browser author, String content) throws Exception {
        var created = author.call("POST", "/api/v1/community/posts", communityPost(author, content));
        assertThat(created.statusCode()).isEqualTo(202);
        UUID post = UUID.fromString(body(created).path("data").path("id").asString());
        Browser admin = login(account("admin"));
        assertThat(admin.call("POST", "/api/v1/admin/community/posts/" + post,
                Map.of("action", "approve")).statusCode()).isEqualTo(200);
        return post;
    }

    private void approveComment(UUID comment) throws Exception {
        Browser admin = login(account("admin"));
        assertThat(admin.call("POST", "/api/v1/admin/community/comments/" + comment,
                Map.of("action", "approve")).statusCode()).isEqualTo(200);
    }

    private JsonNode messages(Browser browser) throws Exception {
        return body(browser.call("GET", "/api/v1/users/me/messages?page=1&size=100", null))
                .path("data").path("items");
    }

    private JsonNode only(JsonNode items, String category) {
        JsonNode found = null;
        int count = 0;
        for (JsonNode item : items) {
            if (category.equals(item.path("category").asString())) {
                found = item;
                count++;
            }
        }
        assertThat(count).as("messages for " + category).isEqualTo(1);
        return found;
    }
}
