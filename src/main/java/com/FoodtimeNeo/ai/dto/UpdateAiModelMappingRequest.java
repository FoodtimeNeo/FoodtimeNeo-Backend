package com.FoodtimeNeo.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateAiModelMappingRequest(
        @NotBlank(message = "槽位不能为空")
        @Schema(description = "逻辑槽位", allowableValues = {"primary-model", "escalation-model", "fallback-model", "photo-model"})
        String slot,

        @NotBlank(message = "模型 ID 不能为空")
        @Size(max = 200, message = "模型 ID 过长")
        @Schema(description = "NVIDIA 兼容接口的模型 ID，如 nvidia/nemotron-3.5-lightning-30b-a3b", example = "nvidia/nemotron-3.5-lightning-30b-a3b")
        String model,

        @Size(max = 200, message = "备注过长")
        @Schema(description = "切换原因，如 monitor-2026-10-21-03：新模型晋升", nullable = true)
        String reason) {
}
