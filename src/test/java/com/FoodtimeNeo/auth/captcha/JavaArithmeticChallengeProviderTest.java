package com.FoodtimeNeo.auth.captcha;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class JavaArithmeticChallengeProviderTest {
    private static final Pattern EXPRESSION = Pattern.compile("(\\d+) ([+×÷]) (\\d+) = \\?");

    @Test
    void generatesOnlyAdditionMultiplicationAndExactDivision() {
        JavaArithmeticChallengeProvider provider = new JavaArithmeticChallengeProvider();
        for (int i = 0; i < 200; i++) {
            GeneratedChallenge challenge = provider.generate();
            Matcher matcher = EXPRESSION.matcher(challenge.expression());
            assertThat(matcher.matches()).isTrue();
            int left = Integer.parseInt(matcher.group(1));
            int right = Integer.parseInt(matcher.group(3));
            int expected = switch (matcher.group(2)) {
                case "+" -> left + right;
                case "×" -> left * right;
                case "÷" -> {
                    assertThat(right).isNotZero();
                    assertThat(left % right).isZero();
                    yield left / right;
                }
                default -> throw new AssertionError("unexpected operator");
            };
            assertThat(challenge.answer()).isEqualTo(expected);
            assertThat(challenge.expression()).doesNotContain("-");
            assertThat(challenge.image()).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4e, (byte) 0x47);
            assertThat(challenge.toString()).doesNotContain(challenge.expression());
        }
    }

    @Test
    void divisionCanUseATwoDigitDividend() {
        JavaArithmeticChallengeProvider provider = new JavaArithmeticChallengeProvider();
        boolean twoDigit = false;
        for (int i = 0; i < 500 && !twoDigit; i++) {
            GeneratedChallenge challenge = provider.generate();
            Matcher matcher = EXPRESSION.matcher(challenge.expression());
            if (matcher.matches() && matcher.group(2).equals("÷")) {
                twoDigit = Integer.parseInt(matcher.group(1)) >= 10;
            }
        }
        assertThat(twoDigit).isTrue();
    }
}
