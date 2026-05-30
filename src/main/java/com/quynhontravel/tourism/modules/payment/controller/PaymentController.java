package com.quynhontravel.tourism.modules.payment.controller;

import com.quynhontravel.tourism.common.response.ApiResponse;
import com.quynhontravel.tourism.modules.payment.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final VNPayService vnPayService;

    /**
     * API tạo liên kết thanh toán VNPay cho Booking du lịch (Yêu cầu đăng nhập)
     */
    @PostMapping("/{bookingId}/payment-url")
    public ResponseEntity<ApiResponse<String>> getPaymentUrl(
            @PathVariable UUID bookingId,
            HttpServletRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = "127.0.0.1";
        }
        
        String paymentUrl = vnPayService.createPaymentUrl(bookingId, ipAddress);
        return ResponseEntity.ok(ApiResponse.success(paymentUrl, "Sinh liên kết thanh toán VNPay thành công."));
    }

    /**
     * Webhook IPN xử lý bất đồng bộ từ cổng thanh toán VNPay gửi về
     */
    @GetMapping("/vnpay-callback")
    public ResponseEntity<String> vnpayCallback(@RequestParam Map<String, String> params) {
        String resultJson = vnPayService.processIpn(params);
        return ResponseEntity.ok(resultJson);
    }
}
