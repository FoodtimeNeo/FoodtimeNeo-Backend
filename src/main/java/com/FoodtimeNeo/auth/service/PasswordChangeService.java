package com.FoodtimeNeo.auth.service;

import com.FoodtimeNeo.auth.dto.ChangePasswordRequest;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.user.entity.LoginAccount;
import com.FoodtimeNeo.user.entity.UserProfile;
import com.FoodtimeNeo.user.mapper.UserMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class PasswordChangeService {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordChangeService.class);
    private final UserMapper users;
    private final PasswordEncoder passwords;
    public PasswordChangeService(UserMapper users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }
    public void change(@NotNull UserProfile current, @NotNull @Valid ChangePasswordRequest request) {
        try {
            LoginAccount account = users.findLoginAccountById(current.id());
            if (account == null || !"active".equals(account.status())
                    || !account.passwordChangedAt().equals(current.passwordChangedAt())) {
                throw new BusinessException("UNAUTHENTICATED", "请先登录或重新登录", HttpStatus.UNAUTHORIZED);
            }
            if (!passwords.matches(request.oldPassword(), account.passwordHash())) {
                throw new BusinessException("OLD_PASSWORD_INCORRECT", "旧密码错误", HttpStatus.BAD_REQUEST);
            }
            if (request.oldPassword().equals(request.newPassword())) {
                throw new BusinessException("PASSWORD_UNCHANGED", "新密码不能与旧密码相同", HttpStatus.BAD_REQUEST);
            }
            String hash = passwords.encode(request.newPassword());
            // One conditional UPDATE commits both hash and version; competing requests cannot overwrite it.
            if (users.changePassword(account.id(), account.passwordHash(), account.passwordChangedAt(), hash) != 1) {
                throw new BusinessException("PASSWORD_CHANGE_CONFLICT", "账号信息已变更，请重新登录后重试", HttpStatus.CONFLICT);
            }
        } catch (DataAccessException exception) {
            LOG.error("Password change persistence failed: {}", exception.getClass().getSimpleName());
            throw unavailable();
        }
    }
    public static BusinessException unavailable() {
        return new BusinessException("PASSWORD_CHANGE_UNAVAILABLE", "修改密码服务暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
