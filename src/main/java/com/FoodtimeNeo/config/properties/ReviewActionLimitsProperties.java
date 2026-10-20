package com.FoodtimeNeo.config.properties;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix="app.review.actions")
public record ReviewActionLimitsProperties(@NotNull Duration window,@Min(1) int userLimit,@Min(1) int ipLimit,
        @NotBlank @Pattern(regexp="[A-Za-z0-9:_-]{1,100}") String redisNamespace) {
    @AssertTrue public boolean isWindowValid() {
        return window!=null && window.getSeconds()>0 && window.getNano()==0 && window.compareTo(Duration.ofDays(30))<=0;
    }
}
