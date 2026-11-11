package com.FoodtimeNeo.user.lifecycle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import com.FoodtimeNeo.user.dto.ProfileResponse;
import java.time.Instant;

public final class AccountLifecycleDtos {
    public record Summary(String verificationEmail,String schoolEmail,boolean canDelete,boolean canUnbind,Instant deletionCodeAvailableAt) {}
    public record Code(@NotBlank @Pattern(regexp="[0-9]{6}",message="请填写 6 位邮箱验证码") String verificationCode){@Override public String toString(){return "Code[redacted]";}}
    public record Sent(Instant expiresAt,Instant nextSendAt,String verificationEmail) {}
    public record Deleted(boolean deleted,Instant emailsReusableAt) {}
    public record Unbound(ProfileResponse profile,Instant schoolEmailReusableAt) {}
    private AccountLifecycleDtos(){}
}
