package com.FoodtimeNeo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {
    @Bean
    public OpenAPI foodTimeOpenApi() {
        return new OpenAPI().info(new Info()
                .title("FoodTimeNeo Backend API")
                .version("v1")
                .description("校园食堂菜品展示与点评平台 API。当前提供初始化与运行检查接口。"));
    }
}
