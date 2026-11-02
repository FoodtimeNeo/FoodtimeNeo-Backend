package com.FoodtimeNeo.auth.security;

import com.FoodtimeNeo.user.entity.UserProfile;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Set;

/**
 * 访客账号只读：除身份、资料与阅读状态外，不允许任何写请求。
 * <p>
 * 写动作以白名单维护，新接口默认对访客关闭，避免遗漏。
 */
public final class VisitorPolicy {
    private static final Set<String> ALLOWED_WRITE_PATHS = Set.of(
            "/api/v1/auth/logout",
            "/api/v1/auth/bind-email/email-code",
            "/api/v1/users/me/bind-email",
            "/api/v1/users/me/password",
            "/api/v1/users/me/profile");
    private static final List<String> ALLOWED_WRITE_PREFIXES = List.of(
            "/api/v1/users/me/notices/",
            "/api/v1/users/me/messages/");

    private VisitorPolicy() { }

    public static boolean blocked(UserProfile user, HttpServletRequest request) {
        if (user == null || !"visitor".equals(user.role())) {
            return false;
        }
        String method = request.getMethod();
        if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
            return false;
        }
        String path = request.getRequestURI();
        if (ALLOWED_WRITE_PATHS.contains(path)) {
            return false;
        }
        for (String prefix : ALLOWED_WRITE_PREFIXES) {
            if (path.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }
}
