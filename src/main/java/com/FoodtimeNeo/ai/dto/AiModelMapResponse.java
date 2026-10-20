package com.FoodtimeNeo.ai.dto;

import java.util.List;

/** Effective model mapping for observability: which concrete id each slot resolves to and where it came from. */
public record AiModelMapResponse(List<SlotEntry> slots, String pinnedAt, String pinnedBy, String reason) {
    public record SlotEntry(String slot, String model, String source) {
    }
}
