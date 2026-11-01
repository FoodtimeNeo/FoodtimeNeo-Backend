package com.FoodtimeNeo.community.dto;
import jakarta.validation.constraints.Size;
/** Optional legacy annotation; deletion never requires the user to supply a reason. */
public record RemoveCommunityRequest(@Size(max=500) String reason) { }
