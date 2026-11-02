package com.FoodtimeNeo.user.service;

import com.FoodtimeNeo.auth.dto.EmailCodeRequest;
import com.FoodtimeNeo.auth.dto.EmailCodeResponse;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 学校邮箱绑定：访客通过验证校园邮箱解锁发布权限。
 * 验证码沿用注册发码通道，因此共用发送配额。
 */
@Service
public class SchoolEmailBindingService {
    private final UserMapper users;
    private final ProfileMapper profiles;
    private final EmailVerificationService verification;
    private final RegistrationRateLimiter limits;

    public SchoolEmailBindingService(UserMapper users, ProfileMapper profiles,
                                     EmailVerificationService verification, RegistrationRateLimiter limits) {
        this.users = users;
        this.profiles = profiles;
        this.verification = verification;
        this.limits = limits;
    }

    public EmailCodeResponse sendCode(UserProfile actor, SchoolEmailCodeRequest body,
                                      String remoteAddress, String deviceId) {
        if (users.schoolEmailBoundElsewhere(body.email(), actor.id())) {
            throw conflict();
        }
        limits.check(body.email(), remoteAddress, deviceId);
        return verification.send(new EmailCodeRequest(body.email()), remoteAddress, deviceId);
    }

    @Transactional
    public ProfileResponse bind(UserProfile actor, BindSchoolEmailRequest body) {
        VerificationClaim claim = verification.claim(body.email(), body.verificationCode());
        boolean bound = false;
        try {
            if (users.schoolEmailBoundElsewhere(body.email(), actor.id())) {
                throw conflict();
            }
            if (users.bindSchoolEmail(actor.id(), body.email()) == 0) {
                throw new BusinessException("UNAUTHENTICATED", "请重新登录", HttpStatus.UNAUTHORIZED);
            }
            bound = true;
        } catch (DuplicateKeyException race) {
            throw conflict();
        } finally {
            verification.finish(claim, bound);
        }
        return profiles.current(actor.id());
    }

    private BusinessException conflict() {
        return new BusinessException("SCHOOL_EMAIL_TAKEN", "该学校邮箱已被其他账号绑定", HttpStatus.CONFLICT);
    }
}
