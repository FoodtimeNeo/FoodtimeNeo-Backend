package com.FoodtimeNeo.invitation.service;

import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.InvitationProperties;
import com.FoodtimeNeo.invitation.dto.*;
import com.FoodtimeNeo.invitation.entity.InvitationCode;
import com.FoodtimeNeo.invitation.mapper.InvitationCodeStore;
import com.FoodtimeNeo.user.entity.UserProfile;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.validation.annotation.Validated;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
@Validated
public class InvitationService {
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 12;
    private final InvitationCodeStore codes;
    private final InvitationProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public InvitationService(InvitationCodeStore codes, InvitationProperties properties, Clock clock) {
        this.codes = codes; this.properties = properties; this.clock = clock;
    }

    @Transactional
    public CreatedInvitationResponse generate(UserProfile actor, @Valid CreateInvitationRequest body) {
        guard(actor);
        requireSecret();
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = randomCode();
            UUID id = UUID.randomUUID();
            if (codes.insert(id, digest(code), body.label(), body.maxUses(), body.expiresAt(), actor.id()) == 1) {
                var created = new InvitationCodeResponse(id, body.label(), "active", body.maxUses(), 0,
                        body.expiresAt(), clock.instant());
                return new CreatedInvitationResponse(id, code, body.label(), "active",
                        body.maxUses(), body.expiresAt(), created.createdAt());
            }
        }
        throw unavailable();
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public PageResponse<InvitationCodeResponse> list(UserProfile actor, String status, PageRequest page) {
        guard(actor);
        if (status != null && !status.equals("active") && !status.equals("disabled")) {
            throw new BusinessException("VALIDATION_ERROR", "状态只能为active或disabled", HttpStatus.BAD_REQUEST);
        }
        return PageResponse.of(codes.page(status, page.size(), page.offset()), codes.count(status), page);
    }

    @Transactional
    public void setStatus(UserProfile actor, UUID id, String status) {
        guard(actor);
        if (!status.equals("active") && !status.equals("disabled")) {
            throw new BusinessException("VALIDATION_ERROR", "状态只能为active或disabled", HttpStatus.BAD_REQUEST);
        }
        if (codes.setStatus(id, status) == 0) {
            throw new BusinessException("INVITATION_NOT_FOUND", "邀请码不存在", HttpStatus.NOT_FOUND);
        }
    }

    /** Validates an optional registration code and consumes one use. Returns the code id for attribution. */
    @Transactional
    public UUID consume(String rawCode) {
        requireSecret();
        InvitationCode locked = codes.lockByHash(digest(rawCode));
        if (locked == null || !locked.status().equals("active")
                || (locked.expiresAt() != null && !locked.expiresAt().isAfter(clock.instant()))
                || (locked.maxUses() != null && locked.usedCount() >= locked.maxUses())) {
            throw new BusinessException("INVITATION_CODE_INVALID", "邀请码无效或已失效", HttpStatus.BAD_REQUEST);
        }
        codes.incrementUsed(locked.id());
        return locked.id();
    }

    private void guard(UserProfile actor) {
        if (!actor.role().equals("superadmin") || !actor.status().equals("active")) {
            throw new BusinessException("ACCESS_DENIED", "需要平台管理员权限", HttpStatus.FORBIDDEN);
        }
    }

    private void requireSecret() {
        if (properties.secret() == null || properties.secret().length() < 32) {
            throw new BusinessException("INVITATION_UNAVAILABLE", "邀请码服务未配置，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private BusinessException unavailable() {
        return new BusinessException("INVITATION_UNAVAILABLE", "邀请码服务暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
    }

    private String randomCode() {
        var text = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) text.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        return text.toString();
    }

    private String digest(String code) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(hmac.doFinal(code.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception failure) {
            throw unavailable();
        }
    }

    /** Normalizes user input before validation and lookup. */
    public static String normalize(String rawCode) {
        return rawCode == null ? null : rawCode.strip().toUpperCase(Locale.ROOT);
    }
}
