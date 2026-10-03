package com.FoodtimeNeo.config;

import com.FoodtimeNeo.config.properties.AuthProperties;
import com.FoodtimeNeo.auth.security.SecurityErrorWriter;
import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.auth.security.SessionValidationFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.CookieSerializer;
import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {
    public static final String SESSION_COOKIE = "FOODTIME_SESSION";

    @Bean
    public Clock authClock() { return Clock.systemUTC(); }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public CookieSerializer cookieSerializer(AuthProperties properties) {
        var cookie = new DefaultCookieSerializer();
        cookie.setCookieName(SESSION_COOKIE);
        cookie.setCookiePath("/");
        cookie.setUseHttpOnlyCookie(true);
        cookie.setUseSecureCookie(properties.cookieSecure());
        cookie.setSameSite("Lax");
        cookie.setCookieMaxAge(Math.toIntExact(properties.sessionTtl().toSeconds()));
        return cookie;
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository contexts,
                                                   SessionAuthenticationService sessions, SecurityErrorWriter errors)
            throws Exception {
        http.cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .securityContext(context -> context.securityContextRepository(contexts).requireExplicitSave(true))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .csrf(csrf -> csrf.csrfTokenRepository(new HttpSessionCsrfTokenRepository())
                        // Registration neither authenticates the browser nor changes an existing account.
                        .ignoringRequestMatchers("/api/v1/auth/register"))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                errors.write(response, 401, "UNAUTHENTICATED", "请先登录或重新登录"))
                        .accessDeniedHandler((request, response, exception) -> {
                            if (exception instanceof CsrfException) {
                                errors.write(response, 403, "CSRF_INVALID", "CSRF令牌缺失或无效，请重新获取后重试");
                            } else {
                                errors.write(response, 403, "ACCESS_DENIED", "无权访问该资源");
                            }
                        }))
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf", "/api/v1/system/ping",
                                "/actuator/health", "/actuator/health/**", "/actuator/info",
                                "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/register",
                                "/api/v1/auth/register/email-code").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new SessionValidationFilter(sessions, errors), CsrfFilter.class);
        return http.build();
    }
}
