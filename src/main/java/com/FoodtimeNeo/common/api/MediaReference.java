package com.FoodtimeNeo.common.api;

import java.util.regex.Pattern;

/**
 * Same-site media reference produced by the upload endpoint: {@code /api/v1/media/{uuid}}.
 * <p>
 * Validation rejects malformed values before the transaction reaches storage, where
 * {@code ImageStorageService.requireOwnedReference} still enforces ownership.
 */
public final class MediaReference {
    public static final String PREFIX = "/api/v1/media/";
    public static final String PATTERN =
            "\\A/api/v1/media/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\z";
    public static final String MESSAGE = "图片引用不合法，请使用本人上传的图片";

    private static final Pattern COMPILED = Pattern.compile(PATTERN);

    private MediaReference() { }

    public static boolean matches(String value) {
        return value != null && COMPILED.matcher(value).matches();
    }
}
