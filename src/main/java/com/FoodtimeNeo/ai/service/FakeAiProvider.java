package com.FoodtimeNeo.ai.service;

import com.FoodtimeNeo.ai.dto.AiVerdict;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Deterministic demo double for local development and integration tests.
 * Never use its verdicts for real moderation judgments.
 */
@Service
public class FakeAiProvider implements AiProvider {
    @Override
    public AiVerdict checkReview(int rating, String content) {
        if (content != null && content.contains("违禁")) {
            return new AiVerdict("HIGH", 0.95, List.of("违规"), "演示判定：命中测试标记", "fake");
        }
        return new AiVerdict("LOW", 0.9, List.of(), "演示判定：无风险", "fake");
    }

    @Override
    public AiVerdict checkImage(byte[] image, String contentType) {
        return new AiVerdict("LOW", 0.85, List.of(), "演示判定：无风险", "fake");
    }
}
