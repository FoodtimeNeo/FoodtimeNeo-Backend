package com.FoodtimeNeo.common.api;

import com.FoodtimeNeo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** Pagination protocol values; these are not persisted business fields. */
public record PageRequest(int page, int size) {
    public PageRequest {
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException("VALIDATION_ERROR", "page必须大于等于1，size必须在1到100之间", HttpStatus.BAD_REQUEST);
        }
    }
    public long offset() { return ((long) page - 1) * size; }
}
