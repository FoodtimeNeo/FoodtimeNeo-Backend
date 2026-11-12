package com.FoodtimeNeo.auth.security;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;
import java.util.regex.Pattern;

/** Precise request whitelist for a disabled account's appeal-only session. */
public final class AppealOnlyPolicy {
    public static final String RESTRICTED = AppealOnlyPolicy.class.getName() + ".restricted";
    private static final Set<String> GET = Set.of(
            "/api/v1/auth/csrf", "/api/v1/auth/me", "/api/v1/auth/captcha",
            "/api/v1/users/me/moderation-decisions", "/api/v1/users/me/appeals");
    private static final Set<String> POST = Set.of(
            "/api/v1/auth/login", "/api/v1/auth/logout", "/api/v1/uploads/images");
    private static final Pattern DECISION =
            Pattern.compile("^/api/v1/users/me/moderation-decisions/[0-9a-fA-F-]{36}$");
    private static final Pattern APPEAL =
            Pattern.compile("^/api/v1/users/me/appeals/[0-9a-fA-F-]{36}$");
    private static final Pattern SUBMIT_APPEAL =
            Pattern.compile("^/api/v1/users/me/moderation-decisions/[0-9a-fA-F-]{36}/appeals$");
    private static final Pattern MEDIA =
            Pattern.compile("^/api/v1/media/[0-9a-fA-F-]{36}$");

    private AppealOnlyPolicy() {}

    public static boolean allowed(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if ("OPTIONS".equals(method) || "HEAD".equals(method)) return true;
        if ("GET".equals(method)) {
            return GET.contains(path) || DECISION.matcher(path).matches()
                    || APPEAL.matcher(path).matches() || MEDIA.matcher(path).matches();
        }
        if ("POST".equals(method)) {
            return POST.contains(path) || SUBMIT_APPEAL.matcher(path).matches();
        }
        return false;
    }
}
