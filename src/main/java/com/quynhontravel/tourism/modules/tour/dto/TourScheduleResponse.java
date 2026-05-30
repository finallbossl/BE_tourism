package com.quynhontravel.tourism.modules.tour.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourScheduleResponse {
    private UUID id;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private Integer maxSlots;
    private Integer availableSlots;
    private BigDecimal currentPrice;
    private String status;
    private UUID guideId;
}
