package com.FoodtimeNeo.api.system;

import com.FoodtimeNeo.common.exception.GlobalExceptionHandler;
import com.FoodtimeNeo.config.properties.AppReleaseProperties;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SystemControllerTest {
    private static MockMvc mvc(int versionCode) {
        return MockMvcBuilders.standaloneSetup(new SystemController(new AppReleaseProperties(
                        versionCode, "1.2.0", "/downloads/app.apk?v=1.2.0-9", "修复若干问题", 0)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void exposesTheConfiguredReleaseWithoutAuthentication() throws Exception {
        mvc(9).perform(get("/api/v1/system/app-release"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.versionCode").value(9))
                .andExpect(jsonPath("$.data.versionName").value("1.2.0"))
                .andExpect(jsonPath("$.data.apkUrl").value("/downloads/app.apk?v=1.2.0-9"))
                .andExpect(jsonPath("$.data.notes").value("修复若干问题"))
                .andExpect(jsonPath("$.data.minVersionCode").value(0));
    }

    @Test
    void zeroVersionMeansThePromptIsDisabled() throws Exception {
        mvc(0).perform(get("/api/v1/system/app-release"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.versionCode").value(0));
    }
}
