package com.FoodtimeNeo.catalog.cache;

import com.FoodtimeNeo.common.api.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.databind.ObjectMapper;

import java.security.*;
import java.util.*;

@RestControllerAdvice
public class CatalogResponseAdvice implements ResponseBodyAdvice<Object> {
    private final ObjectMapper json;
    public CatalogResponseAdvice(ObjectMapper json) { this.json = json; }
    @Override public boolean supports(MethodParameter method, Class<? extends HttpMessageConverter<?>> converter) {
        return method.getContainingClass().isAnnotationPresent(PublicCatalog.class);
    }
    @Override public Object beforeBodyWrite(Object body, MethodParameter method, MediaType type,
            Class<? extends HttpMessageConverter<?>> converter, ServerHttpRequest request, ServerHttpResponse response) {
        if (request.getMethod() != HttpMethod.GET || !(body instanceof ApiResponse<?> envelope)
                || !"OK".equals(envelope.code())) return body;
        try {
            // Timestamp/request ID are transport metadata, not part of the public data fingerprint.
            String etag = "\"catalog-v1-" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(json.writeValueAsBytes(envelope.data()))) + "\"";
            var headers = response.getHeaders();
            headers.setETag(etag);
            headers.setCacheControl("private, no-cache");
            headers.setVary(List.of("Cookie"));
            headers.set("X-Foodtime-Public-Catalog", "1");
            String condition = request.getHeaders().getFirst(HttpHeaders.IF_NONE_MATCH);
            if (condition != null && Arrays.stream(condition.split(",")).map(String::trim)
                    .map(value -> value.startsWith("W/") ? value.substring(2) : value)
                    .anyMatch(value -> value.equals(etag) || value.equals("*"))) {
                // Runs after security, validation and the visible-parent query/cache, never before them.
                response.setStatusCode(HttpStatus.NOT_MODIFIED);
                return null;
            }
            return body;
        } catch (NoSuchAlgorithmException failure) { throw new IllegalStateException(failure); }
    }
}
