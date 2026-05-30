package com.quynhontravel.tourism.modules.auth.service;

import com.quynhontravel.tourism.common.enums.OtpPurpose;
import com.quynhontravel.tourism.common.enums.UserRole;
import com.quynhontravel.tourism.modules.auth.dto.AuthResponse;
import com.quynhontravel.tourism.modules.auth.dto.RequestOtpRequest;
import com.quynhontravel.tourism.modules.auth.dto.VerifyOtpRequest;
import com.quynhontravel.tourism.modules.user.entity.User;
import com.quynhontravel.tourism.modules.user.repository.UserRepository;
import com.quynhontravel.tourism.common.utils.JwtUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final JwtUtils jwtUtils;

    /**
     * Yêu cầu mã OTP cho email. Nếu chưa đăng ký, tự động tạo mới tài khoản (Passwordless Registration).
     */
    @Transactional
    public void requestOtp(RequestOtpRequest request) {
        String email = request.getEmail();
        
        if (!userRepository.existsByEmail(email)) {
            log.info("Email {} chưa đăng ký, tiến hành đăng ký tài khoản khách hàng mới", email);
            User newUser = User.builder()
                    .email(email)
                    .fullName(email.split("@")[0]) // Lấy phần đầu email làm tên tạm thời
                    .role(UserRole.ROLE_CUSTOMER)
                    .loyaltyPoints(0)
                    .isActive(true)
                    .build();
            userRepository.save(newUser);
        }
        
        otpService.generateAndSendOtp(email, OtpPurpose.LOGIN);
    }

    /**
     * Xác thực mã OTP và sinh bộ đôi JWT Access Token + Refresh Token
     */
    @Transactional
    public AuthResponseAndCookie verifyOtp(VerifyOtpRequest request) {
        String email = request.getEmail();
        String otpCode = request.getOtpCode();
        
        boolean isValid = otpService.verifyOtp(email, otpCode, OtpPurpose.LOGIN);
        if (!isValid) {
            throw new RuntimeException("Mã OTP không hợp lệ hoặc đã hết hạn");
        }
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng phù hợp"));
        
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa");
        }
        
        String accessToken = jwtUtils.generateAccessToken(user);
        String refreshToken = jwtUtils.generateRefreshToken(user);
        
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
                
        return new AuthResponseAndCookie(authResponse, refreshToken);
    }
    
    @Getter
    @AllArgsConstructor
    public static class AuthResponseAndCookie {
        private final AuthResponse authResponse;
        private final String refreshToken;
    }
}
