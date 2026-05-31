package com.quynhontravel.tourism.modules.review.dto;

import com.quynhontravel.tourism.common.enums.SentimentType;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private UUID id;
    private UUID customerId;
    private UUID tourId;
    private Integer rating;
    private String comment;
    private SentimentType aiSentiment;
    private Boolean isReported;
    private OffsetDateTime createdAt;
}
