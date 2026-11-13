package com.FoodtimeNeo.community.edit;

import com.FoodtimeNeo.common.api.MediaReference;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

/** Whole replacement of author-owned editable fields; repost source and creation time are immutable. */
public record EditPostRequest(@NotNull @Min(1) Long expectedRevision, @NotNull UUID clientRequestId,
        @NotBlank @Size(max=50) String title, @NotBlank @Size(max=2000) String content,
        @NotNull @Size(max=9) List<@NotBlank @Pattern(regexp=MediaReference.PATTERN) String> imageUrls,
        UUID dishId) {
    public EditPostRequest {
        title=title==null?null:title.strip();content=content==null?null:content.strip();
        imageUrls=imageUrls==null?null:java.util.Collections.unmodifiableList(new java.util.ArrayList<>(imageUrls));
    }
}
