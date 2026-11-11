package com.FoodtimeNeo.community.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RecordPostViewRequest(@NotNull UUID clientRequestId) { }
