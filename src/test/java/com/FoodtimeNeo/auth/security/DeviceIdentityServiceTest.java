package com.FoodtimeNeo.auth.security;

import com.FoodtimeNeo.config.properties.AuthProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceIdentityServiceTest {
    private final AuthProperties auth = new AuthProperties(Duration.ofDays(7), false,
            Duration.ofMinutes(15), 10, 1000, "test:login");
    private final DeviceIdentityService service = new DeviceIdentityService(auth,
            "test-device-secret-at-least-32-characters", "foodtime_device", Duration.ofDays(365));

    @Test
    void createsSignedCookieAndReusesTheSameIdentity() {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        String id = service.resolve(request, response);

        Cookie cookie = response.getCookie("foodtime_device");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isFalse();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isEqualTo(365 * 24 * 60 * 60);
        assertThat(cookie.getValue()).startsWith(id + ".");
        assertThat(response.getHeader("Set-Cookie")).contains("SameSite=Lax");

        var next = new MockHttpServletRequest();
        next.setCookies(new Cookie("foodtime_device", cookie.getValue()));
        var nextResponse = new MockHttpServletResponse();
        assertThat(service.resolve(next, nextResponse)).isEqualTo(id);
        assertThat(nextResponse.getCookie("foodtime_device")).isNull();
    }

    @Test
    void tamperedCookieIsReplacedInsteadOfTrusted() {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        String id = service.resolve(request, response);
        Cookie original = response.getCookie("foodtime_device");

        var tampered = new MockHttpServletRequest();
        tampered.setCookies(new Cookie("foodtime_device", original.getValue() + "x"));
        var tamperedResponse = new MockHttpServletResponse();
        String replacement = service.resolve(tampered, tamperedResponse);

        assertThat(replacement).isNotEqualTo(id);
        assertThat(tamperedResponse.getCookie("foodtime_device")).isNotNull();
    }

    @Test
    void secureFlagFollowsAuthCookieConfiguration() {
        var secureAuth = new AuthProperties(Duration.ofDays(7), true,
                Duration.ofMinutes(15), 10, 1000, "test:login");
        var secureService = new DeviceIdentityService(secureAuth,
                "test-device-secret-at-least-32-characters", "foodtime_device", Duration.ofDays(365));
        var response = new MockHttpServletResponse();

        secureService.resolve(new MockHttpServletRequest(), response);

        assertThat(response.getCookie("foodtime_device").getSecure()).isTrue();
    }
}
