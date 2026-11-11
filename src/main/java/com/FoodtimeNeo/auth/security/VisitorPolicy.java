package com.FoodtimeNeo.auth.security;

import com.FoodtimeNeo.user.entity.UserProfile;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

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
            "/api/v1/users/me/profile",
            "/api/v1/users/me/feedback");
    private static final List<String> ALLOWED_WRITE_PREFIXES = List.of(
            "/api/v1/users/me/notices/",
            "/api/v1/users/me/account-lifecycle/",
            "/api/v1/users/me/messages/");
    /** 私信可读、可标已读、可隐藏，但发送与新建会话仍受只读限制。 */
    private static final Pattern DIRECT_MESSAGE_BOOKKEEPING =
            Pattern.compile("^/api/v1/users/me/conversations/[0-9a-fA-F-]{36}(/read)?$");
    private static final Pattern USER_BLOCK = Pattern.compile("^/api/v1/users/[0-9a-fA-F-]{36}/block$");

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
        // A read-only account can still receive unwanted messages and report visible content.
        if (("POST".equals(method) && "/api/v1/reports".equals(path))
                || (("PUT".equals(method) || "DELETE".equals(method)) && USER_BLOCK.matcher(path).matches())) {
            return false;
        }
        if (ALLOWED_WRITE_PATHS.contains(path)) {
            return false;
        }
        for (String prefix : ALLOWED_WRITE_PREFIXES) {
            if (path.startsWith(prefix)) {
                return false;
            }
        }
        return !DIRECT_MESSAGE_BOOKKEEPING.matcher(path).matches();
    }
}
