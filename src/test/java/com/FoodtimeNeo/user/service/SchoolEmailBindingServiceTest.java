package com.FoodtimeNeo.user.service;

import com.FoodtimeNeo.auth.dto.SchoolEmailCodeRequest;
import com.FoodtimeNeo.auth.service.RegistrationRateLimiter;
import com.FoodtimeNeo.auth.verification.EmailVerificationService;
import com.FoodtimeNeo.auth.verification.VerificationClaim;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.user.dto.BindSchoolEmailRequest;
import com.FoodtimeNeo.user.dto.ProfileResponse;
import com.FoodtimeNeo.user.entity.UserProfile;
import com.FoodtimeNeo.user.mapper.ProfileMapper;
import com.FoodtimeNeo.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchoolEmailBindingServiceTest {
    private static final String SCHOOL = "20260001@bjtu.edu.cn";
    private final UserMapper users = mock(UserMapper.class);
    private final ProfileMapper profiles = mock(ProfileMapper.class);
    private final EmailVerificationService verification = mock(EmailVerificationService.class);
    private final RegistrationRateLimiter limits = mock(RegistrationRateLimiter.class);
    private final SchoolEmailBindingService service =
            new SchoolEmailBindingService(users, profiles, verification, limits);
    private final UUID userId = UUID.randomUUID();
    private final UserProfile actor = new UserProfile(userId, "someone@gmail.com", "访客", "visitor", "active", null);

    private static String codeOf(Throwable error) {
        return ((BusinessException) error).getCode();
    }

    @Test
    void sendCodeRejectsMailboxAlreadyBoundToAnotherAccount() {
        when(users.schoolEmailBoundElsewhere(SCHOOL, userId)).thenReturn(true);
        assertThatThrownBy(() -> service.sendCode(actor, new SchoolEmailCodeRequest(SCHOOL), "127.0.0.1", "device"))
                .isInstanceOf(BusinessException.class)
                .extracting(SchoolEmailBindingServiceTest::codeOf)
                .isEqualTo("SCHOOL_EMAIL_TAKEN");
        verify(verification, never()).send(any(), anyString(), anyString());
    }

    @Test
    void sendCodeGoesThroughTheSharedRegistrationQuota() {
        when(users.schoolEmailBoundElsewhere(SCHOOL, userId)).thenReturn(false);
        service.sendCode(actor, new SchoolEmailCodeRequest(SCHOOL), "127.0.0.1", "device");
        verify(limits).check(SCHOOL, "127.0.0.1", "device");
        verify(verification).send(any(), org.mockito.ArgumentMatchers.eq("127.0.0.1"),
                org.mockito.ArgumentMatchers.eq("device"));
    }

    @Test
    void bindPersistsTheSchoolEmailAndReturnsTheUpdatedProfile() {
        when(verification.claim(SCHOOL, "012345")).thenReturn(new VerificationClaim(SCHOOL, "claim"));
        when(users.bindSchoolEmail(userId, SCHOOL)).thenReturn(1);
        ProfileResponse expected = new ProfileResponse(userId, "someone@gmail.com", "访客", "user", null,
                0, 0, SCHOOL, true);
        when(profiles.current(userId)).thenReturn(expected);

        ProfileResponse result = service.bind(actor, new BindSchoolEmailRequest(SCHOOL, "012345"));

        assertThat(result).isEqualTo(expected);
        verify(verification).finish(any(VerificationClaim.class), org.mockito.ArgumentMatchers.eq(true));
    }

    @Test
    void bindConflictFromConcurrentClaimBecomesAConflict() {
        when(verification.claim(SCHOOL, "012345")).thenReturn(new VerificationClaim(SCHOOL, "claim"));
        when(users.bindSchoolEmail(userId, SCHOOL)).thenThrow(new DuplicateKeyException("unique"));
        assertThatThrownBy(() -> service.bind(actor, new BindSchoolEmailRequest(SCHOOL, "012345")))
                .isInstanceOf(BusinessException.class)
                .extracting(SchoolEmailBindingServiceTest::codeOf)
                .isEqualTo("SCHOOL_EMAIL_TAKEN");
    }
}
