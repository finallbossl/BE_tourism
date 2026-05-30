package com.quynhontravel.tourism.modules.guide.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourGuideCheckinResponse {
    private UUID bookingId;
    private String customerName;
    private String phoneNumber;
    private Integer quantityAdults;
    private Integer quantityChildren;
    private String status;
}
