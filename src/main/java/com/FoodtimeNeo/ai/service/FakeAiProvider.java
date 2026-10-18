package com.FoodtimeNeo.ai.service;

import com.FoodtimeNeo.ai.dto.AiVerdict;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deterministic demo double for local development and integration tests.
 * Never use its verdicts for real moderation judgments.
 * Remembers content containing 存疑: first look is unsure, second look confirms,
 * which exercises the escalation path end to end.
 */
@Service
public class FakeAiProvider implements AiProvider {
    private final Set<String> seen = ConcurrentHashMap.newKeySet();

    @Override
    public AiVerdict checkReview(String model, int rating, String content) {
        if (content != null && content.contains("违禁")) {
            return new AiVerdict("HIGH", 0.95, List.of("违规"), "演示判定：命中测试标记", "fake");
        }
        if (content != null && content.contains("存疑")) {
            if (seen.add("review:" + content)) {
                return new AiVerdict("LOW", 0.5, List.of(), "演示判定：拿不准", "fake");
            }
            return new AiVerdict("LOW", 0.95, List.of(), "演示判定：复核确认无风险", "fake");
        }
        return new AiVerdict("LOW", 0.9, List.of(), "演示判定：无风险", "fake");
    }

    @Override
    public AiVerdict checkImage(String model, byte[] image, String contentType) {
        return new AiVerdict("LOW", 0.85, List.of(), "演示判定：无风险", "fake");
    }
}
