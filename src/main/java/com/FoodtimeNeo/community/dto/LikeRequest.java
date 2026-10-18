package com.FoodtimeNeo.community.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LikeRequest(
        @NotBlank(message = "目标类型不能为空")
        @Pattern(regexp = "post|comment", message = "目标类型不合法")
        @Schema(description = "点赞目标：post 动态 / comment 评论", allowableValues = {"post", "comment"})
        String targetType,

        @Schema(description = "目标ID")
        @NotNull(message = "目标ID不能为空")
        UUID targetId) { }
