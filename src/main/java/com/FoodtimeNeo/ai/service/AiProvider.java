package com.FoodtimeNeo.ai.service;

import com.FoodtimeNeo.ai.dto.AiVerdict;

/** Diagnoses pending submissions; implementations must not persist or publish anything. */
public interface AiProvider {
    AiVerdict checkReview(int rating, String content) throws AiUnavailableException;
    AiVerdict checkImage(byte[] image, String contentType) throws AiUnavailableException;
}
