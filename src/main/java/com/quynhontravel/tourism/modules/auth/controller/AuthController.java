package com.quynhontravel.tourism.modules.auth.controller;

import com.quynhontravel.tourism.common.response.ApiResponse;
import com.quynhontravel.tourism.modules.auth.dto.AuthResponse;
import com.quynhontravel.tourism.modules.auth.dto.RequestOtpRequest;
import com.quynhontravel.tourism.modules.auth.dto.VerifyOtpRequest;
import com.quynhontravel.tourism.modules.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * API gửi yêu cầu OTP qua Email đăng nhập
     */
    @PostMapping("/request-otp")
    public ResponseEntity<ApiResponse<Void>> requestOtp(@Valid @RequestBody RequestOtpRequest request) {
        authService.requestOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Mã OTP đã được gửi thành công, vui lòng kiểm tra hộp thư email của bạn."));
    }

    /**
     * API xác thực OTP và cấp phát JWT Token (Access Token trả về body, Refresh Token lưu cookie HttpOnly)
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletResponse response) {
        
        AuthService.AuthResponseAndCookie result = authService.verifyOtp(request);
        
        // Thiết lập HttpOnly Cookie cho Refresh Token nhằm chống XSS/CSRF
        Cookie refreshTokenCookie = new Cookie("refresh_token", result.getRefreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true); // Yêu cầu truyền qua HTTPS trong môi trường production
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // Thời gian sống 7 ngày
        refreshTokenCookie.setAttribute("SameSite", "Strict");
        response.addCookie(refreshTokenCookie);
        
        return ResponseEntity.ok(ApiResponse.success(result.getAuthResponse(), "Đăng nhập thành công!"));
    }
}
