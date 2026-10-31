package com.FoodtimeNeo.ai.service;

import com.FoodtimeNeo.ai.dto.AiVerdict;
import com.FoodtimeNeo.config.properties.SingleModerationProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 短桩验证：单 key 单模型直判，无轮询、无重试、无网络前置失败。 */
class SingleKeyModerationProviderTest {
    private HttpServer server;
    private final AtomicInteger requestCount = new AtomicInteger();
    private final AtomicReference<String> lastAuthorization = new AtomicReference<>();
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private volatile int statusToReturn = 200;

    private static final String VERDICT = "{\"choices\":[{\"message\":{\"content\":"
            + "\"{\\\"risk\\\":\\\"LOW\\\",\\\"confidence\\\":0.99,\\\"labels\\\":[],\\\"reason\\\":\\\"测试通过\\\"}\"}}]}";

    @BeforeEach
    void stubEndpoint() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            requestCount.incrementAndGet();
            lastAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] payload = (statusToReturn == 200 ? VERDICT : "{\"error\":\"busy\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusToReturn, payload.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(payload);
            }
        });
        server.start();
    }

    @AfterEach
    void stopEndpoint() {
        server.stop(0);
    }

    private SingleKeyModerationProvider provider(String key) {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        return new SingleKeyModerationProvider(
                new SingleModerationProperties(true, baseUrl, key, "deepseek-flash", Duration.ofSeconds(10)));
    }

    @Test
    void usesConfiguredKeyAndModelVerbatim() throws Exception {
        AiVerdict verdict = provider("k1").checkReview("caller-model-is-ignored", 5, "很好吃");

        assertThat(verdict.riskLevel()).isEqualTo("LOW");
        assertThat(verdict.confidence()).isEqualTo(0.99);
        assertThat(verdict.model()).isEqualTo("deepseek-flash");
        assertThat(lastAuthorization.get()).isEqualTo("Bearer k1");
        assertThat(lastBody.get()).contains("\"model\":\"deepseek-flash\"");
        assertThat(requestCount.get()).isEqualTo(1);
    }

    @Test
    void rejectionIsNotRetried() {
        statusToReturn = 429;

        assertThatThrownBy(() -> provider("k1").checkReview("deepseek-flash", 5, "很好吃"))
                .isInstanceOf(AiUnavailableException.class);
        assertThat(requestCount.get()).isEqualTo(1);
    }

    @Test
    void blankOrMultiKeyNeverTouchesNetwork() {
        assertThatThrownBy(() -> provider(" ").checkReview("deepseek-flash", 5, "很好吃"))
                .isInstanceOf(AiUnavailableException.class);
        assertThatThrownBy(() -> provider("k1,k2").checkReview("deepseek-flash", 5, "很好吃"))
                .isInstanceOf(AiUnavailableException.class);
        assertThat(requestCount.get()).isZero();
    }
}
