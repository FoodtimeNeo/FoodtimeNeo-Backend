package com.FoodtimeNeo.community.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateCommentRequest(
        @Size(min = 1, max = 500, message = "评论长度须在1到500字之间")
        @Schema(description = "评论正文，纯文本", maxLength = 500)
        String content) {

    public CreateCommentRequest {
        content = content == null ? null : content.strip();
    }
}
