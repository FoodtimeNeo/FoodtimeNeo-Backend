package com.FoodtimeNeo.community.dto;

import com.FoodtimeNeo.common.api.MediaReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreatePostRequest(
        @NotBlank(message = "标题不能为空") @Size(max=50,message="标题最多50字") String title,
        @NotNull(message="请提供发布请求编号") UUID clientRequestId,
        @NotBlank(message="正文不能为空")
        @Size(min = 1, max = 2000, message = "正文长度须在1到2000字之间")
        @Schema(description = "动态正文，纯文本", maxLength = 2000)
        String content,

        @Size(max = 9, message = "配图最多9张")
        @Schema(description = "本人已上传的暂存图片引用（/api/v1/media/{id}），最多9张")
        List<@NotBlank(message = "图片引用不能为空")
             @Pattern(regexp = MediaReference.PATTERN, message = MediaReference.MESSAGE) String> imageUrls,

        @Schema(description = "转发来源动态ID；原创不填", nullable = true)
        UUID repostOfId) {

    public CreatePostRequest {
        title = title == null ? null : title.strip();
        content = content == null ? null : content.strip();
        imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
    }

    /** Internal reposts retain the original endpoint without bypassing the public title contract. */
    public CreatePostRequest(String content,List<String> images,UUID repostOfId) {
        this(content==null?null:content.strip().substring(0,Math.min(50,content.strip().length())),UUID.randomUUID(),content,images,repostOfId);
    }
}
