package com.FoodtimeNeo.notice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 公告 DTO：时间统一用 Instant，校验用 jakarta validation。 */
public final class NoticeDtos {
    private NoticeDtos() { }

    @Schema(description = "公告条目；seq 仅作轮询游标，不对外当身份，故不在此暴露")
    public record NoticeResponse(
            UUID id,
            String title,
            String body,
            Instant publishedAt) { }

    @Schema(description = "增量轮询结果；items 为 seq 大于 sinceSeq 的正序，unreadCount 为该用户未读公告数")
    public record NoticeFeedResponse(
            List<NoticeResponse> items,
            long unreadCount) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "新建公告")
    public record CreateNoticeRequest(
            @NotBlank(message = "标题不能为空")
            @Size(max = 200, message = "标题不能超过200个字符")
            @Schema(description = "公告标题", maxLength = 200)
            String title,
            @NotBlank(message = "正文不能为空")
            @Size(max = 20000, message = "正文不能超过20000个字符")
            @Schema(description = "公告正文", maxLength = 20000)
            String body) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "编辑公告")
    public record UpdateNoticeRequest(
            @NotBlank(message = "标题不能为空")
            @Size(max = 200, message = "标题不能超过200个字符")
            @Schema(description = "公告标题", maxLength = 200)
            String title,
            @NotBlank(message = "正文不能为空")
            @Size(max = 20000, message = "正文不能超过20000个字符")
            @Schema(description = "公告正文", maxLength = 20000)
            String body) { }
}
