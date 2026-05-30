package com.quynhontravel.tourism.modules.auth.service;

import com.quynhontravel.tourism.common.enums.OtpPurpose;
import com.quynhontravel.tourism.modules.auth.entity.OtpSession;
import com.quynhontravel.tourism.modules.auth.repository.OtpSessionRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpSessionRepository otpSessionRepository;
    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final String REDIS_OTP_KEY_PREFIX = "otp:";

    @Transactional
    public void generateAndSendOtp(String email, OtpPurpose purpose) {
        // 1. Sinh mã OTP 6 chữ số ngẫu nhiên
        String otpCode = String.format("%06d", secureRandom.nextInt(1000000));

        // 2. Lưu vào Redis với TTL là 3 phút
        String redisKey = REDIS_OTP_KEY_PREFIX + email + ":" + purpose.name();
        redisTemplate.opsForValue().set(redisKey, otpCode, 3, TimeUnit.MINUTES);
        log.info("Lưu mã OTP cho email {} vào Redis thành công", email);

        // 3. Lưu vào database PostgreSQL để đối chiếu / audit log
        OtpSession otpSession = OtpSession.builder()
                .email(email)
                .otpCode(otpCode)
                .purpose(purpose)
                .expiredAt(OffsetDateTime.now().plusMinutes(3))
                .isUsed(false)
                .build();
        otpSessionRepository.save(otpSession);

        // 4. Gửi email xác thực chứa mã OTP
        sendOtpEmail(email, otpCode);
    }

    @Transactional
    public boolean verifyOtp(String email, String otpCode, OtpPurpose purpose) {
        String redisKey = REDIS_OTP_KEY_PREFIX + email + ":" + purpose.name();
        String cachedOtp = redisTemplate.opsForValue().get(redisKey);

        if (cachedOtp != null && cachedOtp.equals(otpCode)) {
            // Xóa khỏi Redis (OTP chỉ được sử dụng một lần)
            redisTemplate.delete(redisKey);
            
            // Cập nhật trạng thái trong database
            otpSessionRepository.findFirstByEmailAndPurposeAndIsUsedOrderByCreatedAtDesc(email, purpose, false)
                    .ifPresent(session -> {
                        session.setIsUsed(true);
                        otpSessionRepository.save(session);
                    });
            return true;
        }

        // Cơ chế dự phòng: Kiểm tra từ PostgreSQL nếu cache Redis gặp lỗi
        return otpSessionRepository.findFirstByEmailAndPurposeAndIsUsedOrderByCreatedAtDesc(email, purpose, false)
                .map(session -> {
                    if (session.getOtpCode().equals(otpCode) && session.getExpiredAt().isAfter(OffsetDateTime.now())) {
                        session.setIsUsed(true);
                        otpSessionRepository.save(session);
                        return true;
                    }
                    return false;
                }).orElse(false);
    }

    private void sendOtpEmail(String toEmail, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("Mã xác thực đăng nhập Quy Nhơn Travel");
            
            String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 5px;'>" +
                    "<h2 style='color: #007bff; text-align: center;'>Quy Nhơn Travel</h2>" +
                    "<p>Chào bạn,</p>" +
                    "<p>Bạn đã yêu cầu mã OTP để đăng nhập/đăng ký vào hệ thống Quản lý và Đặt tour Quy Nhơn Travel.</p>" +
                    "<div style='background-color: #f7f7f7; padding: 15px; text-align: center; border-radius: 4px; margin: 20px 0;'>" +
                    "<span style='font-size: 24px; font-weight: bold; letter-spacing: 5px; color: #333;'>" + otpCode + "</span>" +
                    "</div>" +
                    "<p style='color: #ff0000; font-size: 14px;'>Mã OTP này có hiệu lực trong vòng <b>3 phút</b> và chỉ sử dụng được 1 lần.</p>" +
                    "<p>Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.</p>" +
                    "<hr style='border: none; border-top: 1px solid #eee; margin: 20px 0;'>" +
                    "<p style='font-size: 12px; color: #888; text-align: center;'>Quy Nhơn Travel - Hệ thống đặt Tour du lịch thông minh.</p>" +
                    "</div>";
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Đã gửi email chứa OTP đến {}", toEmail);
        } catch (MessagingException e) {
            log.error("Lỗi khi gửi email OTP đến {}", toEmail, e);
            throw new RuntimeException("Không thể gửi email OTP. Vui lòng thử lại sau.");
        }
    }
}
