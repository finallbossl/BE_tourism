package com.quynhontravel.tourism.modules.booking.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private UUID id;
    private UUID customerId;
    private UUID scheduleId;
    private Integer quantityAdults;
    private Integer quantityChildren;
    private BigDecimal totalPrice;
    private Integer pointsUsed;
    private String status;
    private OffsetDateTime checkInAt;
    private OffsetDateTime createdAt;
}
