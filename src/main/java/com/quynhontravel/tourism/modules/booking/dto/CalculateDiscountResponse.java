package com.quynhontravel.tourism.modules.booking.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculateDiscountResponse {
    private BigDecimal totalRawPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
    private Integer pointsUsed;
    private Integer pointsBalance;
}
