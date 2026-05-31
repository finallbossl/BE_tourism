package com.quynhontravel.tourism.modules.review.controller;

import com.quynhontravel.tourism.common.response.ApiResponse;
import com.quynhontravel.tourism.modules.review.dto.CreateReviewRequest;
import com.quynhontravel.tourism.modules.review.dto.ReviewResponse;
import com.quynhontravel.tourism.modules.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * API gửi đánh giá tour (Yêu cầu đăng nhập, vai trò CUSTOMER)
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID customerId = UUID.fromString(jwt.getClaimAsString("userId"));
        ReviewResponse response = reviewService.submitReview(customerId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Đăng đánh giá thành công và đã gửi phân tích AI cảm xúc."));
    }

    /**
     * API xem danh sách đánh giá của một tour (Công khai)
     */
    @GetMapping("/tour/{tourId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getTourReviews(@PathVariable UUID tourId) {
        List<ReviewResponse> response = reviewService.getTourReviews(tourId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách đánh giá thành công."));
    }

    /**
     * API báo cáo vi phạm đánh giá (Yêu cầu đăng nhập, vai trò CUSTOMER)
     */
    @PostMapping("/{reviewId}/report")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> reportReview(@PathVariable UUID reviewId) {
        reviewService.reportReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Báo cáo đánh giá vi phạm thành công."));
    }

    /**
     * API xóa đánh giá (Chỉ dành cho ADMIN, MANAGER)
     */
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable UUID reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Xóa đánh giá thành công."));
    }
}
