package com.FoodtimeNeo.config;

import com.FoodtimeNeo.config.properties.ParticipationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import static org.assertj.core.api.Assertions.assertThat;

class ParticipationConfigurationTest {
    @Configuration(proxyBeanMethods=false)
    @EnableConfigurationProperties(ParticipationProperties.class)
    static class Config { }
    @Test void defaultIsOpenAndOnePropertyRestoresSchoolGating() {
        var runner=new ApplicationContextRunner().withUserConfiguration(Config.class);
        runner.run(context -> assertThat(context.getBean(ParticipationProperties.class).schoolEmailRequired()).isFalse());
        runner.withPropertyValues("app.participation.school-email-required=true")
                .run(context -> assertThat(context.getBean(ParticipationProperties.class).schoolEmailRequired()).isTrue());
    }
}
