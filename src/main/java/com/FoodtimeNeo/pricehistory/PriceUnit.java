package com.FoodtimeNeo.pricehistory;

import com.FoodtimeNeo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import java.util.Set;

/** Stable wire values, independent of localized labels and chart rendering. */
public final class PriceUnit {
    public static final Set<String> VALUES=Set.of("unspecified","serving","piece","bowl","jin","100g","kg");
    private PriceUnit() {}
    public static String require(String value) {
        if (value==null || !VALUES.contains(value))
            throw new BusinessException("VALIDATION_ERROR","计价单位不合法",HttpStatus.BAD_REQUEST);
        return value;
    }
}
