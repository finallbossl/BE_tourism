package com.quynhontravel.tourism.modules.tour.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourResponse {
    private UUID id;
    private String title;
    private String slug;
    private String description;
    private BigDecimal basePrice;
    private Integer durationDays;
    private Integer durationNights;
    private String coverImage;
    private String[] imagesGallery;
    private String categoryName;
    private String categorySlug;
}
