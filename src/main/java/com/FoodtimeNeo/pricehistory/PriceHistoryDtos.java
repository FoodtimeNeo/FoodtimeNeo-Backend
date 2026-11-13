package com.FoodtimeNeo.pricehistory;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PriceHistoryDtos {
    private PriceHistoryDtos() {}
    public record Entry(String id, BigDecimal price, String priceUnit, Instant recordedAt,
                        String origin, long revision, boolean carriedForward) {}
    public record History(UUID dishId, String dishName, BigDecimal currentPrice, String priceUnit,
                          long revision, Instant windowStart, Instant windowEnd,
                          boolean canManage, long totalRecords, List<Entry> records) {
        public History { records=List.copyOf(records); }
    }
    public record Edit(@DecimalMin("0") @Digits(integer=8,fraction=2) BigDecimal price,
                       @NotBlank String priceUnit, @NotNull @Min(1) Long expectedRevision) {}
}
