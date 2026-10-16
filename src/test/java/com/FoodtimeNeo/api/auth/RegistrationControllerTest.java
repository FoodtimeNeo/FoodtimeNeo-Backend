package com.FoodtimeNeo.api.auth;

import com.FoodtimeNeo.auth.dto.RegisterRequest;
import com.FoodtimeNeo.auth.dto.RegisterResponse;
import com.FoodtimeNeo.auth.service.RegistrationService;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.common.exception.GlobalExceptionHandler;
import com.FoodtimeNeo.common.web.RequestIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RegistrationControllerTest {
    private final JsonMapper json = JsonMapper.builder().build();
    private RegistrationService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(RegistrationService.class);
        mvc = MockMvcBuilders.standaloneSetup(new RegistrationController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).addFilters(new RequestIdFilter()).build();
    }

    @Test
    void validRequestReturns201WithNormalizedEmailAndNoCredentials() throws Exception {
        when(service.register(any())).thenAnswer(call -> {
            RegisterRequest request = call.getArgument(0);
            return new RegisterResponse(UUID.randomUUID(), request.email(), "干饭人000012", "user");
        });
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Request-Id", "register-test")
                        .content(body(" 00123@BJTU.EDU.CN ", "Abc12345")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.email").value("00123@bjtu.edu.cn"))
                .andExpect(jsonPath("$.data.displayName").value("干饭人000012"))
                .andExpect(jsonPath("$.data.role").value("user"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.requestId").value("register-test"));
    }

    @ParameterizedTest
    @CsvSource({"abc@bjtu.edu.cn", "123a@bjtu.edu.cn", "123@example.com", "123@bjtu.edu.cn.evil.test",
            "@bjtu.edu.cn", "123@sub.bjtu.edu.cn", "１２３@bjtu.edu.cn"})
    void invalidEmailsNeverReachTheService(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body(email, "Abc12345")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data[*].field", hasItem("email")));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @CsvSource({"Abc1234", "abcdefgh", "12345678", "中文123456"})
    void weakPasswordsNeverReachTheService(String password) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body("123@bjtu.edu.cn", password)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data[*].field", hasItem("password")))
                .andExpect(jsonPath("$.data[0].rejectedValue").doesNotExist());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @MethodSource("passwordsWithDisallowedCharacters")
    void disallowedPasswordCharactersReturnFieldErrorsBeforeReachingTheService(String password) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body("123@bjtu.edu.cn", password)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data[*].field", hasItem("password")))
                .andExpect(jsonPath("$.data[*].message", hasItem("密码只能包含英文字母、数字和半角符号，不允许空白或其他字符")));
        verifyNoInteractions(service);
    }

    private static Stream<String> passwordsWithDisallowedCharacters() {
        return Stream.of("Abc12345中文", "A1😀😀😀", "Abc12345😀", "Abc12345！", "Abc12345é",
                "Abc12345１", " Abc12345", "Abc12345 ", "Abc 12345", "Abc12345\t", "Abc12345\n",
                "Abc12345\u0000", "Abc12345\u007f");
    }

    @Test
    void malformedUnicodePasswordReturns400InsteadOfReachingTheHasher() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"123@bjtu.edu.cn\",\"password\":\"Abc12345\\ud800\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data[*].field", hasItem("password")));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @MethodSource("validPasswords")
    void passwordsAtLengthBoundariesAndWithEveryAsciiSymbolAreAccepted(String password) throws Exception {
        when(service.register(any())).thenReturn(new RegisterResponse(UUID.randomUUID(), "123@bjtu.edu.cn", "干饭人000012", "user"));
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body("123@bjtu.edu.cn", password)))
                .andExpect(status().isCreated());
        verify(service).register(new RegisterRequest("123@bjtu.edu.cn", password, "012345"));
    }

    private static Stream<String> validPasswords() {
        return Stream.of("Abc12345", "A1" + "x".repeat(126), "Abc12345!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "12345", "1234567", "abcdef", "１２３４５６", "123456\n"})
    void malformedVerificationCodeNeverReachesRegistration(String code) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "123@bjtu.edu.cn", "password", "Abc12345", "verificationCode", code))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data[*].field", hasItem("verificationCode")));
        verifyNoInteractions(service);
    }

    @Test
    void passwordAndEmailAloneCannotBypassEmailVerification() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"123@bjtu.edu.cn\",\"password\":\"Abc12345\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data[*].field", hasItem("verificationCode")));
        verifyNoInteractions(service);
    }

    @Test
    void missingFieldsAndOversizedPasswordAreRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body("123@bjtu.edu.cn", "A1" + "x".repeat(127))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    @Test
    void duplicateEmailAndUnavailableServiceReturnActionableFailures() throws Exception {
        when(service.register(any())).thenThrow(new BusinessException("EMAIL_ALREADY_REGISTERED", "该邮箱已注册", HttpStatus.CONFLICT));
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body("123@bjtu.edu.cn", "Abc12345")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"))
                .andExpect(jsonPath("$.message").value("该邮箱已注册"));
        doThrow(new BusinessException("REGISTRATION_UNAVAILABLE", "注册服务暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE))
                .when(service).register(any());
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body("123@bjtu.edu.cn", "Abc12345")))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("REGISTRATION_UNAVAILABLE"));
    }

    @Test
    void malformedJsonReturnsBadRequestWithoutCallingTheService() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    private String body(String email, String password) {
        return json.writeValueAsString(Map.of("email", email, "password", password, "verificationCode", "012345"));
    }
}
