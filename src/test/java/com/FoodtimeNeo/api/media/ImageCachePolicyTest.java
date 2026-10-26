package com.FoodtimeNeo.api.media;

import com.FoodtimeNeo.media.service.ImageStorageService;
import com.FoodtimeNeo.media.service.UploadRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ImageCachePolicyTest {
    @Test void validConditionalResponseDoesNotReadImageBytes() {
        var images = mock(ImageStorageService.class);
        var id = UUID.randomUUID();
        var info = new ImageStorageService.ImageInfo(id, "image/png",
                ImageStorageService.CachePolicy.PRIVATE_REVALIDATE, 1000);
        when(images.inspect(id, null)).thenReturn(info);
        var request = new MockHttpServletRequest();
        request.addHeader("If-None-Match", "\"other\", W/\"" + id + "-thumb\"");
        var response = new ImageStorageController(images, mock(UploadRateLimiter.class)).read(id, "thumb", request);
        assertThat(response.getStatusCode().value()).isEqualTo(304);
        assertThat(response.getHeaders().getCacheControl()).contains("private", "no-cache");
        assertThat(response.getHeaders().getVary()).contains("Cookie");
        verify(images, never()).readBytes(any(), anyBoolean());
    }

    @Test void privatePreviewIgnoresValidatorsAndNeverBecomesReusable() {
        var images = mock(ImageStorageService.class);
        var id = UUID.randomUUID();
        var info = new ImageStorageService.ImageInfo(id, "image/png", ImageStorageService.CachePolicy.NO_STORE, 1000);
        when(images.inspect(id, null)).thenReturn(info);
        when(images.readBytes(info, false)).thenReturn(new byte[]{1});
        var request = new MockHttpServletRequest();
        request.addHeader("If-None-Match", "*");
        var response = new ImageStorageController(images, mock(UploadRateLimiter.class)).read(id, "full", request);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getHeaders().getETag()).isNull();
    }
}
