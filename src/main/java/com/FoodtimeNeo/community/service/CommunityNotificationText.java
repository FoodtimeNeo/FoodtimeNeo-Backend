package com.FoodtimeNeo.community.service;

/** Pure text rules for aggregated community interaction messages. */
public final class CommunityNotificationText {
    private CommunityNotificationText() { }

    public static String likeBody(String first, String second, long count, boolean post) {
        String target = post ? "食记" : "评论";
        if (count <= 1) return name(first) + "赞了你的" + target;
        if (count == 2) return name(first) + "和" + name(second) + "赞了你的" + target;
        return name(first) + "和" + name(second) + "等总计 " + count + " 人赞了你的" + target;
    }

    public static String commentBody(String author, String content, boolean reply) {
        return name(author) + (reply ? " 回复你：" : "：") + snippet(content);
    }

    public static String snippet(String content) {
        String text = content == null ? "" : content.replaceAll("\\s+", " ").strip();
        return text.length() <= 80 ? text : text.substring(0, 80) + "…";
    }

    private static String name(String value) {
        return value == null || value.isBlank() ? "一位同学" : value.strip();
    }
}
