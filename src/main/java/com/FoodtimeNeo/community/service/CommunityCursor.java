package com.FoodtimeNeo.community.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/** Scope-bound stable cursor; it never grants access to a target. */
public record CommunityCursor(Instant time, UUID id) {
    public String encode(String scope) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (scope+"|"+time+"|"+id).getBytes(StandardCharsets.UTF_8));
    }
    public static CommunityCursor parse(String value, String scope) {
        if (value == null || value.isBlank()) return null;
        try {
            if (value.length()>400) throw new IllegalArgumentException();
            String[] parts=new String(Base64.getUrlDecoder().decode(value),StandardCharsets.UTF_8).split("\\|",-1);
            if(parts.length!=3 || !parts[0].equals(scope)) throw new IllegalArgumentException();
            return new CommunityCursor(Instant.parse(parts[1]),UUID.fromString(parts[2]));
        } catch (RuntimeException invalid) {
            throw new BusinessException("CURSOR_INVALID","分页位置无效，请刷新列表",HttpStatus.BAD_REQUEST);
        }
    }
}
