package com.FoodtimeNeo.common.web;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.FoodtimeNeo.config.properties.MonitoringProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SlowRequestLoggingFilterTest {
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        var logger = (Logger) LoggerFactory.getLogger(SlowRequestLoggingFilter.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setAdditive(false);
    }

    @AfterEach
    void detachAppender() {
        var logger = (Logger) LoggerFactory.getLogger(SlowRequestLoggingFilter.class);
        logger.detachAppender(appender);
        logger.setAdditive(true);
    }

    @Test
    void slowApiResponsesAreLoggedWithMethodAndPath() throws Exception {
        var filter = new SlowRequestLoggingFilter(new MonitoringProperties("127.0.0.1", Duration.ZERO));
        var request = new MockHttpServletRequest("GET", "/api/v1/dishes");
        var chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage()).contains("Slow request GET /api/v1/dishes");
    }

    @Test
    void fastAndNonApiRequestsStayQuiet() throws Exception {
        var filter = new SlowRequestLoggingFilter(new MonitoringProperties("127.0.0.1", Duration.ofHours(1)));
        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/dishes"), new MockHttpServletResponse(),
                new MockFilterChain());
        filter.doFilter(new MockHttpServletRequest("GET", "/actuator/prometheus"), new MockHttpServletResponse(),
                new MockFilterChain());
        assertThat(appender.list).isEmpty();
    }
}
