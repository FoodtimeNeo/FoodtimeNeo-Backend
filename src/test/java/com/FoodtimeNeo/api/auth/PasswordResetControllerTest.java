package com.FoodtimeNeo.api.auth;
import com.FoodtimeNeo.auth.reset.PasswordResetService;
import com.FoodtimeNeo.auth.security.DeviceIdentityService;
import com.FoodtimeNeo.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordResetControllerTest {
    @Test void directEmailPasswordOrClientVerifiedFlagCannotReset() throws Exception {
        var resets=mock(PasswordResetService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new PasswordResetController(resets,mock(DeviceIdentityService.class)))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
        for(String body:new String[]{"{\"email\":\"admin@example.test\",\"newPassword\":\"New12345!\"}",
            "{\"verified\":true,\"email\":\"admin@example.test\",\"newPassword\":\"New12345!\"}",
            "{\"resetToken\":\"123456\",\"newPassword\":\"New12345!\"}"}){
            mvc.perform(post("/api/v1/auth/password-reset").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(resets);
    }
}
