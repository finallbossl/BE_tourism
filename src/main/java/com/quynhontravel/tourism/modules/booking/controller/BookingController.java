package com.quynhontravel.tourism.modules.booking.controller;

import com.quynhontravel.tourism.common.response.ApiResponse;
import com.quynhontravel.tourism.modules.booking.dto.BookingResponse;
import com.quynhontravel.tourism.modules.booking.dto.CalculateDiscountRequest;
import com.quynhontravel.tourism.modules.booking.dto.CalculateDiscountResponse;
import com.quynhontravel.tourism.modules.booking.dto.CreateBookingRequest;
import com.quynhontravel.tourism.modules.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /**
     * API đặt tour du lịch mới cho khách hàng (Yêu cầu Token xác thực để lấy userId)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID customerId = UUID.fromString(jwt.getClaimAsString("userId"));
        BookingResponse response = bookingService.createBooking(request, customerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Đặt chỗ thành công! Vui lòng tiến hành thanh toán trong 30 phút."));
    }

    /**
     * API xem lịch sử giao dịch/đặt tour của cá nhân khách hàng hiện tại
     */
    @GetMapping("/my-history")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyHistory(@AuthenticationPrincipal Jwt jwt) {
        UUID customerId = UUID.fromString(jwt.getClaimAsString("userId"));
        List<BookingResponse> history = bookingService.getMyHistory(customerId);
        return ResponseEntity.ok(ApiResponse.success(history, "Lấy lịch sử đặt chỗ thành công."));
    }

    /**
     * API tính toán trước số tiền giảm trừ thực tế khi áp dụng điểm tích lũy
     */
    @PostMapping("/calculate-discount")
    public ResponseEntity<ApiResponse<CalculateDiscountResponse>> calculateDiscount(
            @Valid @RequestBody CalculateDiscountRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID customerId = UUID.fromString(jwt.getClaimAsString("userId"));
        CalculateDiscountResponse response = bookingService.calculateDiscount(request, customerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Tính toán giảm giá thành công."));
    }
}
