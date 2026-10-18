package com.FoodtimeNeo.community.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RepostRequest(
        @Size(min = 1, max = 2000, message = "转发语长度须在1到2000字之间")
        @Schema(description = "转发语，必填", maxLength = 2000)
        String content) {

    public RepostRequest {
        content = content == null ? null : content.strip();
    }
}
