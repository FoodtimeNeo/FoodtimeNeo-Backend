package com.FoodtimeNeo;

import com.FoodtimeNeo.config.CorsProperties;
import com.FoodtimeNeo.config.ProductionConfigurationValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentConfigurationTest {
    @TempDir
    Path temporaryDirectory;

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(context -> {
                // Test the fixture file independently of the developer's real DB / Redis variables.
                context.getEnvironment().getPropertySources().remove("systemEnvironment");
                new ConfigDataApplicationContextInitializer().initialize(context);
            })
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void envFileSelectsProductionAndResolvesConnectionSettings() throws Exception {
        Path envFile = temporaryDirectory.resolve(".env");
        Files.writeString(envFile, """
                SPRING_PROFILES_ACTIVE=prod
                DB_URL=jdbc:postgresql://localhost:25432/foodtime_test?currentSchema=foodtime
                DB_USERNAME=test_user
                DB_PASSWORD=test_password
                REDIS_HOST=localhost
                REDIS_PASSWORD=test_redis_password
                CORS_ALLOWED_ORIGINS=https://foodtime.example
                """);
        runner.withPropertyValues("spring.config.import=" + envFile.toUri() + "[.properties]")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("prod");
                    assertThat(context).hasSingleBean(ProductionConfigurationValidator.class);
                    assertThat(context.getEnvironment().getProperty("spring.datasource.username")).isEqualTo("test_user");
                    assertThat(context.getEnvironment().getProperty("springdoc.api-docs.enabled", Boolean.class)).isFalse();
                    assertThat(context.getEnvironment().getProperty("springdoc.swagger-ui.enabled", Boolean.class)).isFalse();
                    assertThat(context.getBean(CorsProperties.class).allowedOrigins()).containsExactly("https://foodtime.example");
                });
    }

    @Test
    void productionRejectsAnEmptyRedisPassword() {
        runner.withPropertyValues("SPRING_PROFILES_ACTIVE=prod",
                        "DB_URL=jdbc:postgresql://localhost/foodtime_test?currentSchema=foodtime",
                        "DB_USERNAME=test_user", "DB_PASSWORD=test_password", "REDIS_HOST=localhost", "REDIS_PASSWORD=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage(
                            "Missing required production configuration: spring.data.redis.password");
                });
    }

    @Test
    void productionRejectsInsecureSessionCookies() {
        runner.withPropertyValues("SPRING_PROFILES_ACTIVE=prod",
                        "DB_URL=jdbc:postgresql://localhost/foodtime_test?currentSchema=foodtime",
                        "DB_USERNAME=test_user", "DB_PASSWORD=test_password", "REDIS_HOST=localhost",
                        "REDIS_PASSWORD=test_redis_password", "AUTH_COOKIE_SECURE=false")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage("Production authentication requires Secure cookies");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CorsProperties.class)
    @Import(ProductionConfigurationValidator.class)
    static class TestConfiguration { }
}
