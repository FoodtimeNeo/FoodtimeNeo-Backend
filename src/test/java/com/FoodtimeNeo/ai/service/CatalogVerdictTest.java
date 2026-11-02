package com.FoodtimeNeo.ai.service;

import com.FoodtimeNeo.ai.dto.AiVerdict;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class CatalogVerdictTest {
    private AiVerdict verdict(String risk,double confidence,String label){return new AiVerdict(risk,confidence,List.of(label),label,"test-model");}
    @Test void unsafeImageCannotBeHiddenByConfidentCleanText(){
        var result=AiTriageService.catalogVerdict(verdict("LOW",1,"文字正常"),verdict("HIGH",.97,"图片风险"));
        assertThat(result.riskLevel()).isEqualTo("HIGH");assertThat(result.confidence()).isEqualTo(.97);
        assertThat(result.reason()).contains("文字正常","图片风险");
    }
    @Test void cleanAdviceUsesTheWeakerConfidenceAndKeepsBothLabels(){
        var result=AiTriageService.catalogVerdict(verdict("LOW",.61,"文字"),verdict("LOW",.99,"图片"));
        assertThat(result.riskLevel()).isEqualTo("LOW");assertThat(result.confidence()).isEqualTo(.61);
        assertThat(result.labels()).containsExactly("文字","图片");
    }
    @Test void uncertaintyIsNotConvertedToCleanAdvice(){
        assertThat(AiTriageService.catalogVerdict(verdict("MEDIUM",.99,"文字存疑"),verdict("LOW",1,"图片正常")).riskLevel()).isEqualTo("MEDIUM");
    }
}
