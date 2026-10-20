package com.FoodtimeNeo.ai.dto;

import java.util.List;

/** A triage-only verdict; never approves or rejects anything by itself. */
public record AiVerdict(String riskLevel, double confidence, List<String> labels, String reason, String model) {
    public AiVerdict {
        if (riskLevel == null || (!riskLevel.equals("LOW") && !riskLevel.equals("MEDIUM") && !riskLevel.equals("HIGH"))) {
            throw new IllegalArgumentException("riskLevel must be LOW, MEDIUM or HIGH");
        }
        labels = labels == null ? List.of() : List.copyOf(labels.stream().limit(10).toList());
        confidence = Math.min(1.0, Math.max(0.0, confidence));
        reason = reason == null ? "" : reason.length() > 500 ? reason.substring(0, 500) : reason;
    }
}
