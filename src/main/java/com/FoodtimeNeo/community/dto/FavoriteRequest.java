package com.FoodtimeNeo.community.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FavoriteRequest(
        @Schema(description = "动态ID")
        @NotNull(message = "动态ID不能为空")
        UUID postId) { }
