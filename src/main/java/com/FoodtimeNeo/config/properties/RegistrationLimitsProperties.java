package com.FoodtimeNeo.config.properties;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Validated
@ConfigurationProperties(prefix = "app.auth.registration")
public record RegistrationLimitsProperties(@NotNull RegistrationMode mode, @NotNull Duration window,
        @Min(1) int accountLimit, @Min(1) int deviceLimit, @Min(1) int ipLimit,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9:_-]{1,100}") String redisNamespace,
        @NotEmpty List<@NotBlank String> allowedEmailDomains) {

    /** 国内外主流邮箱；子域（如 vip.qq.com）同样允许。 */
    public static final List<String> DEFAULT_ALLOWED_EMAIL_DOMAINS = List.of(
            "qq.com", "foxmail.com", "163.com", "126.com", "yeah.net",
            "sina.com", "sina.cn", "sohu.com", "139.com", "189.cn", "wo.cn", "aliyun.com", "21cn.com",
            "gmail.com", "outlook.com", "hotmail.com", "live.com", "msn.com",
            "icloud.com", "me.com", "mac.com", "yahoo.com", "proton.me", "protonmail.com",
            "bjtu.edu.cn");

    public RegistrationLimitsProperties {
        allowedEmailDomains = allowedEmailDomains == null ? List.of() : allowedEmailDomains.stream()
                .filter(Objects::nonNull)
                .map(domain -> domain.strip().toLowerCase(Locale.ROOT))
                .map(domain -> domain.startsWith("@") ? domain.substring(1) : domain)
                .filter(domain -> !domain.isEmpty())
                .distinct()
                .toList();
    }

    @AssertTrue(message = "Registration window must be whole positive seconds and no more than 30 days")
    public boolean isWindowValid() {
        return window != null && window.getSeconds() > 0 && window.getNano() == 0
                && window.compareTo(Duration.ofDays(30)) <= 0;
    }
}
