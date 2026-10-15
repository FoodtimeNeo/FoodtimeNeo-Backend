package com.FoodtimeNeo.config;

import com.FoodtimeNeo.config.properties.CorsProperties;
import com.FoodtimeNeo.config.validation.ProductionConfigurationValidator;
import com.FoodtimeNeo.config.properties.EmailVerificationProperties;
import com.FoodtimeNeo.config.properties.TestVerificationProperties;
import com.FoodtimeNeo.config.validation.MailConfigurationValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentConfigurationTest {
    @TempDir
    Path temporaryDirectory;

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(context -> {
                // Neither the shell environment nor local credentials may affect configuration tests.
                context.getEnvironment().getPropertySources().remove("systemEnvironment");
                TestPropertyValues.of("spring.config.location=" + temporaryDirectory.toUri()).applyTo(context);
                new ConfigDataApplicationContextInitializer().initialize(context);
            })
            .withUserConfiguration(TestConfiguration.class);

    @BeforeEach
    void isolateLocalConfigurationImports() throws Exception {
        for (String filename : new String[] {"application.yml", "application-dev.yml", "application-prod.yml"}) {
            String contents;
            try (var input = new ClassPathResource(filename).getInputStream()) {
                contents = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
            contents = contents.replace("optional:file:./.env[.properties]",
                            "optional:" + temporaryDirectory.resolve(".env").toUri() + "[.properties]")
                    .replace("optional:file:./config/mail.yml",
                            "optional:" + temporaryDirectory.resolve("mail.yml").toUri());
            Files.writeString(temporaryDirectory.resolve(filename), contents);
        }
    }

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
                    assertThat(context.getBean(TestVerificationProperties.class).enabled()).isFalse();
                    assertThat(context.getEnvironment().getProperty("spring.datasource.username")).isEqualTo("test_user");
                    assertThat(context.getEnvironment().getProperty("springdoc.api-docs.enabled", Boolean.class)).isFalse();
                    assertThat(context.getEnvironment().getProperty("springdoc.swagger-ui.enabled", Boolean.class)).isFalse();
                    assertThat(context.getBean(CorsProperties.class).allowedOrigins()).containsExactly("https://foodtime.example");
                });
    }

    @Test
    void developmentEnablesTestVerificationByDefaultAndCanDisableIt() {
        runner.withPropertyValues("SPRING_PROFILES_ACTIVE=dev")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(TestVerificationProperties.class).enabled()).isTrue();
                });
        runner.withPropertyValues("SPRING_PROFILES_ACTIVE=dev", "TEST_VERIFICATION_ENABLED=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(TestVerificationProperties.class).enabled()).isFalse();
                });
    }

    @Test
    void productionRejectsExplicitTestVerificationIncludingMixedProfiles() {
        for (String profiles : new String[] {"prod", "dev,prod", "prod,dev"}) {
            runner.withPropertyValues("SPRING_PROFILES_ACTIVE=" + profiles, "TEST_VERIFICATION_ENABLED=true",
                            "DB_URL=jdbc:postgresql://localhost/foodtime_test?currentSchema=foodtime",
                            "DB_USERNAME=test_user", "DB_PASSWORD=test_password", "REDIS_HOST=localhost",
                            "REDIS_PASSWORD=test_redis_password", "AUTH_COOKIE_SECURE=true")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure()).hasRootCauseMessage("Production must disable test verification");
                    });
        }
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

    @Test
    void enabledVerificationRequiresSecretAndRealSmtpHost() {
        runner.withPropertyValues("EMAIL_VERIFICATION_ENABLED=true", "MAIL_FROM=sender@example.test", "MAIL_HOST=127.0.0.1")
                .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("EMAIL_VERIFICATION_ENABLED=true", "MAIL_FROM=sender@example.test",
                        "EMAIL_VERIFICATION_SECRET=test-only-32-character-long-secret", "MAIL_HOST=smtp.example.com")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage("Enabled email verification requires a real spring.mail.host");
                });
    }

    @Test
    void productionMailRequiresTls() {
        runner.withPropertyValues("SPRING_PROFILES_ACTIVE=prod", "EMAIL_VERIFICATION_ENABLED=true",
                        "DB_URL=jdbc:postgresql://localhost/foodtime_test?currentSchema=foodtime", "DB_USERNAME=test_user",
                        "DB_PASSWORD=test_password", "REDIS_HOST=localhost", "REDIS_PASSWORD=test_redis_password",
                        "MAIL_FROM=sender@example.test", "MAIL_HOST=127.0.0.1", "EMAIL_VERIFICATION_SECRET=test-only-32-character-long-secret",
                        "spring.mail.properties[mail.smtp.starttls.enable]=false",
                        "spring.mail.properties[mail.smtp.starttls.required]=false", "spring.mail.properties[mail.smtp.ssl.enable]=false")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage("Production SMTP requires TLS");
                });
    }

    @Test
    void validProductionStartTlsConfigurationLoads() {
        runner.withPropertyValues("SPRING_PROFILES_ACTIVE=prod", "EMAIL_VERIFICATION_ENABLED=true",
                        "DB_URL=jdbc:postgresql://localhost/foodtime_test?currentSchema=foodtime", "DB_USERNAME=test_user",
                        "DB_PASSWORD=test_password", "REDIS_HOST=localhost", "REDIS_PASSWORD=test_redis_password",
                        "MAIL_FROM=sender@example.test", "MAIL_HOST=127.0.0.1", "EMAIL_VERIFICATION_SECRET=test-only-32-character-long-secret",
                        "MAIL_USERNAME=test_sender", "MAIL_PASSWORD=test_smtp_password")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({CorsProperties.class, EmailVerificationProperties.class, TestVerificationProperties.class})
    @Import({ProductionConfigurationValidator.class, MailConfigurationValidator.class})
    static class TestConfiguration { }
}
