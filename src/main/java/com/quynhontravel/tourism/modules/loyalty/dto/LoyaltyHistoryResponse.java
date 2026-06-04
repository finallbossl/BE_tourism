package com.quynhontravel.tourism.modules.loyalty.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyHistoryResponse {
    private String transactionType; // EARNED, SPENT
    private Integer points;
    private String description;
    private UUID bookingId;
    private OffsetDateTime createdAt;
}
