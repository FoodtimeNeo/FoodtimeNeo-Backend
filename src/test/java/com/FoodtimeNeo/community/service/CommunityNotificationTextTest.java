package com.FoodtimeNeo.community.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommunityNotificationTextTest {
    @Test
    void oneLikeShowsTheLikerName() {
        assertThat(CommunityNotificationText.likeBody("小明", "", 1, true))
                .isEqualTo("小明赞了你的食记");
    }

    @Test
    void twoLikesJoinBothNames() {
        assertThat(CommunityNotificationText.likeBody("小明", "小红", 2, false))
                .isEqualTo("小明和小红赞了你的评论");
    }

    @Test
    void manyLikesAggregateWithTotalCount() {
        assertThat(CommunityNotificationText.likeBody("小明", "小红", 12, true))
                .isEqualTo("小明和小红等总计 12 人赞了你的食记");
    }

    @Test
    void blankNamesFallBackToAnonymous() {
        assertThat(CommunityNotificationText.likeBody("", "", 1, false))
                .isEqualTo("一位同学赞了你的评论");
    }

    @Test
    void commentBodyMarksRepliesAndTruncatesLongText() {
        String body = CommunityNotificationText.commentBody("小明", "很好吃\n下次还来", false);
        assertThat(body).isEqualTo("小明：很好吃 下次还来");
        String reply = CommunityNotificationText.commentBody("小红", "x".repeat(100), true);
        assertThat(reply).startsWith("小红 回复你：").endsWith("…");
    }
}
