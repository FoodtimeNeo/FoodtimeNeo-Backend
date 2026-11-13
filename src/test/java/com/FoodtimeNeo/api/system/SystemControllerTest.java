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
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"2.0.0α","2.0.0β","2.0.0-alpha"})
    void testingReleaseRemainsExplicitAndOptionalForExistingClients(String name)throws Exception {
        MockMvc endpoint=MockMvcBuilders.standaloneSetup(new SystemController(new AppReleaseProperties(
                23,name,"https://example.test/v2.0.0%CE%B1/app-release.apk","新功能",23))).build();
        endpoint.perform(get("/api/v1/system/app-release")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.versionName").value(name))
                .andExpect(jsonPath("$.data.versionCode").value(23))
                .andExpect(jsonPath("$.data.minVersionCode").value(0))
                .andExpect(jsonPath("$.data.notes").value(org.hamcrest.Matchers.containsString("测试版")));
    }
    @Test void formalReleaseRetainsItsConfiguredMinimum()throws Exception {
        MockMvc endpoint=MockMvcBuilders.standaloneSetup(new SystemController(new AppReleaseProperties(
                24,"2.0.0","https://example.test/app-release.apk","正式版",24))).build();
        endpoint.perform(get("/api/v1/system/app-release"))
                .andExpect(jsonPath("$.data.minVersionCode").value(24));
    }
}
