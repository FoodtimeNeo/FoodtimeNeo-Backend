package com.FoodtimeNeo.notice;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class NoticeHeadingTest {
 @Test void parsesRealHeadings() {
  assertThat(NoticeHeading.title("```md\n# 假标题\n```\n\n## 次级\n\n# **最高** [链接](https://example.com)\n\n# 第二个")).isEqualTo("最高 链接");
  assertThat(NoticeHeading.title("Setext 标题\n===")).isEqualTo("Setext 标题");
  assertThat(NoticeHeading.title("没有标题")).isEmpty();
 }
}
