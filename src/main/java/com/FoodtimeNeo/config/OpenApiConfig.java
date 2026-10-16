package com.FoodtimeNeo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {
    @Bean
    public OpenAPI foodTimeOpenApi() {
        return new OpenAPI().info(new Info()
                .title("FoodTimeNeo Backend API")
                .version("v1")
                .description("校园食堂菜品展示与点评平台 API，包含注册、登录、CSRF及会话管理。"))
                .components(new Components().addSecuritySchemes("sessionCookie", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name(SecurityConfig.SESSION_COOKIE)));
    }
}
