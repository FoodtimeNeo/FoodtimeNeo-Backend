package com.FoodtimeNeo.review.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown=true)
public record CreateReviewReplyRequest(@NotNull UUID clientRequestId, UUID replyToId,
        @NotBlank @Size(max=500) String content) { }
