package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MonitoringIT extends IntegrationApiSupport {
    @Test
    void prometheusIsScrapableFromLoopbackWithApplicationMetrics() throws Exception {
        Browser browser = login(owner);
        assertThat(browser.call("GET", "/api/v1/users/me/profile", null).statusCode()).isEqualTo(200);
        var response = browser.call("GET", "/actuator/prometheus", null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("jvm_memory_used_bytes");
        assertThat(response.body()).contains("http_server_requests_seconds");
    }
}
