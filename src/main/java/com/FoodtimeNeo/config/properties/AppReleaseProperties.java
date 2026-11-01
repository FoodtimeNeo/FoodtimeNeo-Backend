package com.FoodtimeNeo.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Android 应用内更新检查的公开配置。
 * <p>
 * {@code versionCode} 为 0 表示尚未配置，客户端据此判定「无需更新」，因此默认值安全。
 */
@Validated
@ConfigurationProperties(prefix = "app.release")
public record AppReleaseProperties(@Min(0) int versionCode,
        @Size(max = 32) String versionName,
        @Size(max = 500) String apkUrl,
        @Size(max = 1000) String notes,
        @Min(0) int minVersionCode) {
    public AppReleaseProperties {
        versionName = versionName == null ? "" : versionName.strip();
        apkUrl = apkUrl == null ? "" : apkUrl.strip();
        notes = notes == null ? "" : notes.strip();
    }
}
