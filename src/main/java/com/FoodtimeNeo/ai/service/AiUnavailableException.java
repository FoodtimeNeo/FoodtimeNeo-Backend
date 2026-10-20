package com.FoodtimeNeo.ai.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class AiUnavailableException extends BusinessException {
    public AiUnavailableException(String message) {
        super("AI_UNAVAILABLE", message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
