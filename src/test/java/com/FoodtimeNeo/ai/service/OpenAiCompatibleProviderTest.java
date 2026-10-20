package com.FoodtimeNeo.ai.service;

import com.FoodtimeNeo.ai.dto.AiVerdict;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleProviderTest {
    @Test
    void validAnswerMapsToAVerdict() {
        AiVerdict verdict = OpenAiCompatibleProvider.parseVerdict(
                "前言 {\"risk\":\"HIGH\",\"confidence\":0.95,\"labels\":[\"广告\",\"无关\"],\"reason\":\"推广二维码\"} 后记", "test-model");
        assertThat(verdict.riskLevel()).isEqualTo("HIGH");
        assertThat(verdict.confidence()).isEqualTo(0.95);
        assertThat(verdict.labels()).containsExactly("广告", "无关");
        assertThat(verdict.reason()).isEqualTo("推广二维码");
        assertThat(verdict.model()).isEqualTo("test-model");
    }

    @Test
    void riskIsCaseInsensitiveConfidenceIsClampedAndLabelsCapAtTen() {
        AiVerdict verdict = OpenAiCompatibleProvider.parseVerdict(
                "{\"risk\":\"low\",\"confidence\":7,\"labels\":[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\",\"h\",\"i\",\"j\",\"k\"],\"reason\":\"ok\"}", "m");
        assertThat(verdict.riskLevel()).isEqualTo("LOW");
        assertThat(verdict.confidence()).isEqualTo(1.0);
        assertThat(verdict.labels()).hasSize(10);
    }

    @Test
    void incompleteOrMalformedAnswersAreRejected() {
        assertThatThrownBy(() -> OpenAiCompatibleProvider.parseVerdict("一切正常", "m"))
                .isInstanceOf(AiUnavailableException.class);
        assertThatThrownBy(() -> OpenAiCompatibleProvider.parseVerdict("{\"risk\":\"LOW\"}", "m"))
                .isInstanceOf(AiUnavailableException.class);
        assertThatThrownBy(() -> OpenAiCompatibleProvider.parseVerdict("{\"risk\":\"CRITICAL\",\"confidence\":0.5,\"reason\":\"x\"}", "m"))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void keyListIgnoresBlanksAndSurroundingSpaces() {
        assertThat(OpenAiCompatibleProvider.splitKeys("k1,k2 , k3,,"))
                .containsExactly("k1", "k2", "k3");
        assertThat(OpenAiCompatibleProvider.splitKeys("")).isEmpty();
        assertThat(OpenAiCompatibleProvider.splitKeys(null)).isEmpty();
    }
}
