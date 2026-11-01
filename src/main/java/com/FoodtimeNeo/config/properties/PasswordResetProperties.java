package com.FoodtimeNeo.config.properties;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;

@Validated
@ConfigurationProperties("app.auth.password-reset")
public record PasswordResetProperties(
        @DefaultValue("foodtime:auth:password-reset") @Pattern(regexp="[A-Za-z0-9:_-]{1,100}") String redisNamespace,
        @DefaultValue("PT10M") Duration codeTtl,
        @DefaultValue("PT5M") Duration tokenTtl,
        @DefaultValue("PT1M") Duration resendInterval,
        @DefaultValue("5") @Min(1) int sendAccountLimit,
        @DefaultValue("10") @Min(1) int sendDeviceLimit,
        @DefaultValue("200") @Min(1) int sendIpLimit,
        @DefaultValue("10") @Min(1) int verifyAccountLimit,
        @DefaultValue("300") @Min(1) int verifyIpLimit) {
    @AssertTrue(message="Password reset TTLs must be whole seconds between 60 seconds and one hour")
    public boolean isDurationsValid() { return valid(codeTtl) && valid(tokenTtl) && valid(resendInterval); }
    private static boolean valid(Duration v) { return v!=null && v.getNano()==0 && v.getSeconds()>=60 && v.getSeconds()<=3600; }
}
