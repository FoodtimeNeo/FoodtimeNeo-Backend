package com.FoodtimeNeo.feedback;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class FeedbackCsvTest {
    @Test void exportPreservesChineseCommasQuotesAndMultilineContent() {
        String content="功能反馈，包含逗号,\"引号\"\n以及第二行😊";
        var row=new FeedbackDtos.Row(UUID.randomUUID(),"function",content,"pending",Instant.EPOCH,Instant.EPOCH,"用户",null);
        String csv=FeedbackCsv.export(List.of(row));
        assertThat(csv).startsWith("\uFEFF反馈编号,反馈类型,反馈内容\r\n");
        assertThat(FeedbackCsv.parse(csv)).containsExactly(List.of("反馈编号","反馈类型","反馈内容"),
                List.of(row.id().toString(),"功能反馈",content));
    }
    @Test void spreadsheetFormulaPrefixesAreExportedAsText() {
        for(String value:List.of("=HYPERLINK(\"bad\")","+1","-1","@SUM(1)"," \t=1","\t123","\n123"))
            assertThat(FeedbackCsv.cell(value)).isEqualTo("'"+value);
        assertThat(FeedbackCsv.cell("普通内容😊")).isEqualTo("普通内容😊");
    }
    @Test void parsesAddedReplyStatusColumnsAndBothLineEndings() {
        assertThat(FeedbackCsv.parse("a,b,c\n1,\"第一行\r\n第二行\",\"含\"\"引号\"\"\"\r\n"))
                .containsExactly(List.of("a","b","c"),List.of("1","第一行\r\n第二行","含\"引号\""));
    }
    @Test void trailingEmptyFieldAndEmptyQuotedFieldsAreRetained() {
        assertThat(FeedbackCsv.parse("a,b,\n1,\"\",\n")).containsExactly(List.of("a","b",""),List.of("1","",""));
    }
    @Test void rejectsBrokenQuotingInsteadOfShiftingReplyToAnotherFeedback() {
        for(String value:List.of("a,\"unclosed","a,b\"c","\"closed\"x,c"))
            assertThatThrownBy(()->FeedbackCsv.parse(value)).isInstanceOf(IllegalArgumentException.class);
    }
}
