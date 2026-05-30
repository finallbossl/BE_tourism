package com.quynhontravel.tourism.modules.ai.controller;

import com.quynhontravel.tourism.common.response.ApiResponse;
import com.quynhontravel.tourism.modules.ai.dto.AiChatRequest;
import com.quynhontravel.tourism.modules.ai.dto.AiPlannerRequest;
import com.quynhontravel.tourism.modules.ai.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * API lập lịch trình du lịch thông minh bằng AI (Yêu cầu đăng nhập để lưu trữ)
     */
    @PostMapping("/planner")
    public ResponseEntity<ApiResponse<String>> createTravelPlan(
            @Valid @RequestBody AiPlannerRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID customerId = UUID.fromString(jwt.getClaimAsString("userId"));
        String planJson = aiService.createTravelPlan(
                request.getBudget(),
                request.getDays(),
                request.getGuests(),
                request.getPreferences(),
                customerId
        );
        return ResponseEntity.ok(ApiResponse.success(planJson, "Tạo lịch trình du lịch Quy Nhơn bằng AI thành công."));
    }

    /**
     * API Chatbot tư vấn du lịch địa phương Quy Nhơn trực tuyến
     */
    @PostMapping("/chatbot")
    public ResponseEntity<ApiResponse<String>> chat(@Valid @RequestBody AiChatRequest request) {
        String response = aiService.chat(request.getMessage());
        return ResponseEntity.ok(ApiResponse.success(response, "Trợ lý ảo phản hồi thành công."));
    }

    /**
     * API kích hoạt quét và áp dụng điều chỉnh giá động (Chỉ dành cho vai trò MANAGER, ADMIN)
     */
    @PostMapping("/pricing/trigger")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> triggerDynamicPricing() {
        aiService.suggestDynamicPricing();
        return ResponseEntity.ok(ApiResponse.success("Đã kích hoạt thuật toán tính toán giá động AI thành công."));
    }
}
