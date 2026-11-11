package com.FoodtimeNeo.notice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

/** 公告 DTO：时间统一用 Instant，校验用 jakarta validation。 */
public final class NoticeDtos {
    private NoticeDtos() { }

    public record CurrentNoticeResponse(UUID id, String content, Instant createdTime,
                                        boolean isRead, boolean dontShowAgain,Instant publishedAt,
                                        Instant displayEndsAt,boolean isUpdate) {
        public CurrentNoticeResponse(UUID id,String content,Instant createdTime,boolean isRead,boolean dontShowAgain) {
            this(id,content,createdTime,isRead,dontShowAgain,createdTime,null,false);
        }
    }

    public record ConfirmNoticeRequest(@NotNull Boolean dontShowAgain) { }

    public record ConfirmNoticeResponse(boolean isRead, boolean dontShowAgain) { }
    public record Schedule(Instant publishedAt,Instant displayStartsAt,Instant displayEndsAt,LocalDate expiresOn,
                           boolean isUpdate,boolean published,boolean enabled) {}
    public record MarkdownRequest(@NotBlank @Size(max=20000) String content,@Valid Schedule schedule) {
        public MarkdownRequest(String content){this(content,null);}
    }
    public record SettingsRequest(UUID noticeId) { }
    public record SettingsResponse(UUID noticeId) { }
    public record UnreadCountResponse(long unreadCount) { }

    @Schema(description = "公告条目；seq 仅作兼容增量游标，id 才是身份标识")
    public record NoticeResponse(
            UUID id,
            String title,
            String body,
            Instant publishedAt,
            String content,Instant createdTime,Instant displayStartsAt,Instant displayEndsAt,
            LocalDate expiresOn,boolean isUpdate,boolean published,boolean enabled,long seq,String status) {
        public NoticeResponse(UUID id,String title,String body,Instant publishedAt,String content) {
            this(id,title,body,publishedAt,content,publishedAt,publishedAt,null,null,false,true,false,0,"paused");
        }
        public NoticeResponse(UUID id, String title, String body, Instant publishedAt) {
            this(id, title, body, publishedAt, "# " + title + "\n\n" + body);
        }
    }

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
