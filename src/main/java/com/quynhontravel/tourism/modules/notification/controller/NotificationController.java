package com.quynhontravel.tourism.modules.notification.controller;

import com.quynhontravel.tourism.common.response.ApiResponse;
import com.quynhontravel.tourism.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * API gửi thử nghiệm email xác nhận đặt tour kèm QR Code
     */
    @GetMapping("/test-email/{bookingId}")
    public ResponseEntity<ApiResponse<String>> sendTestEmail(@PathVariable UUID bookingId) {
        notificationService.sendBookingConfirmationEmail(bookingId);
        return ResponseEntity.ok(ApiResponse.success(
                "Đã kích hoạt yêu cầu gửi thử nghiệm email. Vui lòng kiểm tra hộp thư.",
                "Thành công"
        ));
    }
}
