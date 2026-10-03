package com.FoodtimeNeo;

import com.FoodtimeNeo.config.CorsProperties;
import com.FoodtimeNeo.config.WebConfig;
import com.FoodtimeNeo.system.SystemController;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CorsConfigurationTest {
    @Test
    void preflightAllowsOnlyConfiguredOrigins() throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(MvcTestConfig.class);
            context.refresh();
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
            mvc.perform(options("/api/v1/system/ping")
                            .header("Origin", "http://localhost:5173")
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                    .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
            mvc.perform(options("/api/v1/system/ping")
                            .header("Origin", "https://untrusted.example")
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }
    }

    @Test
    void configurationRejectsWildcardsPathsAndMalformedOrigins() {
        try (LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet();
            for (String origin : List.of("*", "https://example.com/path", "null", "https://user@example.com")) {
                var properties = new CorsProperties(List.of(origin), List.of("GET"), List.of("Content-Type"),
                        List.of("X-Request-Id"), Duration.ofHours(1));
                assertThat(validator.validate(properties)).isNotEmpty();
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({WebConfig.class, SystemController.class})
    static class MvcTestConfig {
        @Bean
        CorsProperties corsProperties() {
            return new CorsProperties(List.of("http://localhost:5173"), List.of("GET", "POST", "OPTIONS"),
                    List.of("Authorization", "Content-Type"), List.of("X-Request-Id"), Duration.ofHours(1));
        }
    }
}
