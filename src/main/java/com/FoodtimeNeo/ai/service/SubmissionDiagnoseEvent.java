package com.FoodtimeNeo.ai.service;

import java.util.UUID;

/**
 * Fired when a triageable submission is created; the AI listener diagnoses it
 * right away instead of waiting for the next scheduled sweep.
 * Type matches the verdict table, including catalog "dish" and standalone "photo".
 */
public record SubmissionDiagnoseEvent(String type, UUID submissionId) { }
