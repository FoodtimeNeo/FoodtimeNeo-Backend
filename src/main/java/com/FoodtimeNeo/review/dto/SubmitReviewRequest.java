package com.FoodtimeNeo.review.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "评分与评论正文；提交人由服务器会话取得，提交后等待审核")
public record SubmitReviewRequest(
        @NotNull @Min(1) @Max(5) Integer rating,
        @NotBlank String content) { }
