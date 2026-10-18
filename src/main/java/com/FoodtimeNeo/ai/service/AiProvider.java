package com.FoodtimeNeo.ai.service;

import com.FoodtimeNeo.ai.dto.AiVerdict;

/**
 * Diagnoses pending submissions; never persists or publishes anything.
 * The model id selects which deployment answers; providers are stateless
 * except the test double, which remembers content to demonstrate escalation.
 */
public interface AiProvider {
    AiVerdict checkReview(String model, int rating, String content) throws AiUnavailableException;

    /** Rating-free variant for community posts, which carry no score. */
    default AiVerdict checkText(String model, String content) throws AiUnavailableException {
        return checkReview(model, 0, content);
    }

    AiVerdict checkImage(String model, byte[] image, String contentType) throws AiUnavailableException;
}
