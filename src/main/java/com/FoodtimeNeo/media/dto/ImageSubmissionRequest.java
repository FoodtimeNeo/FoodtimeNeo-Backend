package com.FoodtimeNeo.media.dto;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
public record ImageSubmissionRequest(@NotBlank String imageUrl, UUID reviewSubmissionId) {
    public ImageSubmissionRequest(String imageUrl) { this(imageUrl, null); }
}
