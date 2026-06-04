package com.quynhontravel.tourism.modules.loyalty.controller;

import com.quynhontravel.tourism.common.response.ApiResponse;
import com.quynhontravel.tourism.modules.loyalty.dto.LoyaltyBalanceResponse;
import com.quynhontravel.tourism.modules.loyalty.dto.LoyaltyHistoryResponse;
import com.quynhontravel.tourism.modules.loyalty.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    /**
     * API lấy số dư điểm tích lũy hiện tại của khách hàng
     */
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<LoyaltyBalanceResponse>> getBalance(@AuthenticationPrincipal Jwt jwt) {
        UUID customerId = UUID.fromString(jwt.getClaimAsString("userId"));
        LoyaltyBalanceResponse balance = loyaltyService.getBalance(customerId);
        return ResponseEntity.ok(ApiResponse.success(balance, "Lấy số dư điểm tích lũy thành công."));
    }

    /**
     * API lấy lịch sử biến động điểm (cộng điểm từ thanh toán / trừ điểm từ quy đổi giảm giá)
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<LoyaltyHistoryResponse>>> getHistory(@AuthenticationPrincipal Jwt jwt) {
        UUID customerId = UUID.fromString(jwt.getClaimAsString("userId"));
        List<LoyaltyHistoryResponse> history = loyaltyService.getHistory(customerId);
        return ResponseEntity.ok(ApiResponse.success(history, "Lấy lịch sử tích lũy/sử dụng điểm thành công."));
    }
}
