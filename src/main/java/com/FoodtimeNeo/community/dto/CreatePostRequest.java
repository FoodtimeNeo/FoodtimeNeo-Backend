package com.FoodtimeNeo.community.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreatePostRequest(
        @Size(min = 1, max = 2000, message = "正文长度须在1到2000字之间")
        @Schema(description = "动态正文，纯文本", maxLength = 2000)
        String content,

        @Size(max = 9, message = "配图最多9张")
        @Schema(description = "本人已上传的暂存图片引用（/api/v1/media/{id}），最多9张")
        List<String> imageUrls,

        @Schema(description = "转发来源动态ID；原创不填", nullable = true)
        UUID repostOfId) {

    public CreatePostRequest {
        content = content == null ? null : content.strip();
        imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
    }
}
