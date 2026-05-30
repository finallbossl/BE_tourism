package com.quynhontravel.tourism.modules.tour.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourDetailResponse {
    private TourResponse tour;
    private List<TourScheduleResponse> schedules;
}
