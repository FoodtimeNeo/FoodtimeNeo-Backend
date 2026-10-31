package com.FoodtimeNeo.config.properties;

import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;

/**
 * 单 key 直判模式：同一个 key 同一个模型处理全部审核（点评、图片、社区）。
 * 启用后多 key 轮询、升级复核、兜底模型与 Redis 模型映射一律不再进入；
 * 关掉本开关即回到 app.ai.triage 的链路，本类只做配置承载。
 */
@Validated
@ConfigurationProperties(prefix = "app.ai.single")
public record SingleModerationProperties(boolean enabled,
        String baseUrl, String apiKey, String model,
        @NotNull Duration timeout) {
    @AssertTrue(message = "Single moderation timeout must be whole positive seconds and no more than 120")
    public boolean isTimeoutValid() {
        return timeout != null && timeout.getSeconds() > 0 && timeout.getNano() == 0
                && timeout.compareTo(Duration.ofSeconds(120)) <= 0;
    }

    @AssertFalse(message = "Single moderation requires base-url, api-key and model")
    public boolean isIncomplete() {
        return enabled && (blank(baseUrl) || blank(apiKey) || blank(model));
    }

    @AssertFalse(message = "Single moderation accepts exactly one api-key; comma-separated rotation is disabled")
    public boolean isMultiKey() {
        return enabled && apiKey != null && apiKey.contains(",");
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
