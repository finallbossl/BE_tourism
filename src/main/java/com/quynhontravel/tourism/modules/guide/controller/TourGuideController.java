package com.quynhontravel.tourism.modules.guide.controller;

import com.quynhontravel.tourism.common.response.ApiResponse;
import com.quynhontravel.tourism.modules.guide.dto.TourGuideCheckinResponse;
import com.quynhontravel.tourism.modules.guide.service.TourGuideService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tour-guide")
@RequiredArgsConstructor
public class TourGuideController {

    private final TourGuideService tourGuideService;

    /**
     * API Check-in hành khách bằng QR Code (Chỉ dành cho các vai trò GUIDE, ADMIN, MANAGER, STAFF)
     */
    @PostMapping("/checkin/{bookingId}")
    @PreAuthorize("hasAnyRole('GUIDE', 'ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<TourGuideCheckinResponse>> checkIn(@PathVariable UUID bookingId) {
        TourGuideCheckinResponse response = tourGuideService.checkIn(bookingId);
        return ResponseEntity.ok(ApiResponse.success(response, "Check-in hành khách thành công."));
    }
}
