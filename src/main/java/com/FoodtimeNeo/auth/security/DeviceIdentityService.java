package com.FoodtimeNeo.auth.security;

import com.FoodtimeNeo.config.properties.AuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * 匿名设备标识：用签名 Cookie 给每个浏览器一个随机 ID，仅用于限流，
 * 不采集 UA、TLS 或其他浏览器指纹。Cookie 被清除或伪造时会换成新 ID，
 * 因此它只降低校园 NAT 下的误伤，不能替代 IP 高上限和邮箱限流。
 */
@Service
public class DeviceIdentityService {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceIdentityService.class);
    private static final String REQUEST_ATTRIBUTE = DeviceIdentityService.class.getName() + ".id";
    private static final int ID_BYTES = 16;
    private static final String DEFAULT_COOKIE_NAME = "foodtime_device";

    private final SecretKeySpec key;
    private final String cookieName;
    private final Duration maxAge;
    private final boolean secure;
    private final SecureRandom random = new SecureRandom();

    public DeviceIdentityService(AuthProperties auth,
            @Value("${app.auth.device.secret:${EMAIL_VERIFICATION_SECRET:}}") String secret,
            @Value("${app.auth.device.cookie-name:foodtime_device}") String cookieName,
            @Value("${app.auth.device.max-age:P365D}") Duration maxAge) {
        this.key = new SecretKeySpec(secretBytes(secret), "HmacSHA256");
        this.cookieName = cookieName != null && cookieName.matches("[A-Za-z0-9_-]{1,64}")
                ? cookieName : DEFAULT_COOKIE_NAME;
        this.maxAge = maxAge == null || maxAge.isZero() || maxAge.isNegative()
                ? Duration.ofDays(365) : maxAge;
        this.secure = auth.cookieSecure();
    }

    /** Return the verified device ID, or create and set a new signed cookie when absent/invalid. */
    public String resolve(HttpServletRequest request, HttpServletResponse response) {
        Object existing = request.getAttribute(REQUEST_ATTRIBUTE);
        if (existing instanceof String id && !id.isBlank()) {
            return id;
        }
        String id = verified(request);
        if (id == null) {
            id = newId();
            writeCookie(response, id);
        }
        request.setAttribute(REQUEST_ATTRIBUTE, id);
        return id;
    }

    private String verified(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (!cookieName.equals(cookie.getName())) {
                continue;
            }
            String value = cookie.getValue();
            if (value == null) {
                return null;
            }
            int separator = value.indexOf('.');
            if (separator <= 0 || separator != value.lastIndexOf('.')) {
                return null;
            }
            String id = value.substring(0, separator);
            if (!validId(id)) {
                return null;
            }
            byte[] signature;
            try {
                signature = Base64.getUrlDecoder().decode(value.substring(separator + 1));
            } catch (IllegalArgumentException exception) {
                return null;
            }
            if (!MessageDigest.isEqual(sign(id), signature)) {
                return null;
            }
            return id;
        }
        return null;
    }

    private void writeCookie(HttpServletResponse response, String id) {
        if (response.isCommitted()) {
            return;
        }
        ResponseCookie cookie = ResponseCookie.from(cookieName, id + "." + encode(sign(id)))
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String newId() {
        byte[] bytes = new byte[ID_BYTES];
        random.nextBytes(bytes);
        return encode(bytes);
    }

    private static boolean validId(String id) {
        if (!id.matches("[A-Za-z0-9_-]{22}")) {
            return false;
        }
        try {
            return Base64.getUrlDecoder().decode(id).length == ID_BYTES;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private byte[] sign(String id) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return mac.doFinal(("device-cookie-v1:" + id).getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HmacSHA256 is required", exception);
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] secretBytes(String secret) {
        if (secret != null && secret.strip().length() >= 32) {
            return secret.strip().getBytes(StandardCharsets.UTF_8);
        }
        byte[] fallback = new byte[32];
        new SecureRandom().nextBytes(fallback);
        LOG.warn("Device cookie secret is not configured; using an ephemeral secret for this process");
        return fallback;
    }
}
