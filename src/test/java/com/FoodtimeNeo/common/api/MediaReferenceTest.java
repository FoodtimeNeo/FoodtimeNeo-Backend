package com.FoodtimeNeo.common.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MediaReferenceTest {
    private static final String VALID = "/api/v1/media/0a3c0e9a-7405-585e-94ce-29d9275b1b6e";

    @Test
    void acceptsTheReferenceProducedByTheUploadEndpoint() {
        assertThat(MediaReference.matches(VALID)).isTrue();
    }

    @Test
    void rejectsUppercaseHexBecauseStorageComparesCanonicalLowercase() {
        assertThat(MediaReference.matches("/api/v1/media/0A3C0E9A-7405-585E-94CE-29D9275B1B6E")).isFalse();
    }

    @Test
    void rejectsNullAndBlank() {
        assertThat(MediaReference.matches(null)).isFalse();
        assertThat(MediaReference.matches("")).isFalse();
    }

    @Test
    void rejectsOtherOrigins() {
        assertThat(MediaReference.matches("https://evil.example/x.png")).isFalse();
        assertThat(MediaReference.matches("//evil.example/api/v1/media/0a3c0e9a-7405-585e-94ce-29d9275b1b6e")).isFalse();
        assertThat(MediaReference.matches("/api/v1/media/../etc/passwd")).isFalse();
    }

    @Test
    void rejectsTrailingSegments() {
        assertThat(MediaReference.matches(VALID + "/extra")).isFalse();
        assertThat(MediaReference.matches(VALID + "?x=1")).isFalse();
    }

    @Test
    void rejectsMalformedUuid() {
        assertThat(MediaReference.matches("/api/v1/media/not-a-uuid")).isFalse();
        assertThat(MediaReference.matches("/api/v1/media/0a3c0e9a7405585e94ce29d9275b1b6e")).isFalse();
    }
}
