package com.quynhontravel.tourism.modules.notification.service;

import com.quynhontravel.tourism.modules.booking.entity.Booking;
import com.quynhontravel.tourism.modules.booking.repository.BookingRepository;
import com.quynhontravel.tourism.modules.tour.entity.TourSchedule;
import com.quynhontravel.tourism.modules.tour.repository.TourScheduleRepository;
import com.quynhontravel.tourism.modules.user.entity.User;
import com.quynhontravel.tourism.modules.user.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TourScheduleRepository tourScheduleRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Gửi email xác nhận đặt tour thành công kèm mã QR Code check-in (Xử lý bất đồng bộ ngầm)
     */
    @Async
    @Transactional(readOnly = true)
    public void sendBookingConfirmationEmail(UUID bookingId) {
        log.info("Bắt đầu xử lý gửi email xác nhận cho Booking ID: {}", bookingId);
        
        try {
            // 1. Lấy thông tin Booking
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Booking ID: " + bookingId));

            // 2. Lấy thông tin khách hàng
            User user = userRepository.findById(booking.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Khách hàng ID: " + booking.getCustomerId()));

            // 3. Lấy thông tin lịch trình & tour
            TourSchedule schedule = tourScheduleRepository.findById(booking.getScheduleId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Lịch trình ID: " + booking.getScheduleId()));
            
            String tourTitle = schedule.getTour().getTitle();
            String formattedStartDate = schedule.getStartDate().format(DATE_FORMATTER);
            String formattedPrice = String.format("%,.0f VNĐ", booking.getTotalPrice());

            // 4. Tạo nội dung HTML Email (Premium layout)
            String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + bookingId.toString();
            String htmlContent = buildHtmlEmailTemplate(user.getFullName(), bookingId.toString(), tourTitle, 
                    formattedStartDate, booking.getQuantityAdults(), booking.getQuantityChildren(), 
                    formattedPrice, qrCodeUrl);

            // 5. Cấu hình MimeMessage để gửi HTML email
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setTo(user.getEmail());
            helper.setSubject("Quy Nhơn Travel - Xác Nhận Đặt Tour Thành Công [Mã vé: " + bookingId.toString().substring(0, 8).toUpperCase() + "]");
            helper.setText(htmlContent, true);

            // 6. Gửi email
            mailSender.send(mimeMessage);
            log.info("Email xác nhận đặt tour đã được gửi thành công đến: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng khi thực hiện gửi email xác nhận cho Booking ID: {}", bookingId, e);
        }
    }

    /**
     * Tạo template HTML cho email xác nhận đặt tour
     */
    private String buildHtmlEmailTemplate(String customerName, String bookingId, String tourTitle, 
                                          String startDate, int adults, int children, 
                                          String totalPrice, String qrCodeUrl) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "  <meta charset='utf-8'>" +
                "  <style>" +
                "    body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f6f9fc; margin: 0; padding: 0; color: #333333; }" +
                "    .container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); border: 1px solid #e1e8ed; }" +
                "    .header { background: linear-gradient(135deg, #ff7e5f, #feb47b); padding: 35px 20px; text-align: center; color: #ffffff; }" +
                "    .header h1 { margin: 0; font-size: 26px; font-weight: 700; letter-spacing: 0.5px; }" +
                "    .header p { margin: 8px 0 0 0; font-size: 15px; opacity: 0.9; }" +
                "    .content { padding: 30px 25px; }" +
                "    .welcome-text { font-size: 16px; line-height: 1.6; color: #4a5568; margin-bottom: 25px; }" +
                "    .card { background-color: #f7fafc; border-radius: 8px; border: 1px solid #edf2f7; padding: 20px; margin-bottom: 25px; }" +
                "    .card-title { font-size: 15px; font-weight: 700; color: #2d3748; margin-top: 0; margin-bottom: 12px; border-bottom: 2px solid #edf2f7; padding-bottom: 6px; text-transform: uppercase; }" +
                "    .info-row { display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 14px; line-height: 1.5; }" +
                "    .info-label { color: #718096; font-weight: 500; min-width: 130px; }" +
                "    .info-value { color: #2d3748; font-weight: 600; text-align: right; flex-grow: 1; }" +
                "    .total-price { color: #e53e3e !important; font-size: 18px; }" +
                "    .qr-section { text-align: center; margin: 30px 0; padding: 20px; background-color: #fff9f6; border-radius: 8px; border: 1px dashed #ffb4a2; }" +
                "    .qr-image { width: 200px; height: 200px; margin: 12px auto; display: block; border: 4px solid #ffffff; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }" +
                "    .qr-tip { font-size: 13px; color: #ff7e5f; font-weight: 600; margin-top: 10px; }" +
                "    .footer { background-color: #2d3748; color: #a0aec0; text-align: center; padding: 20px; font-size: 12px; line-height: 1.5; }" +
                "    .footer a { color: #feb47b; text-decoration: none; }" +
                "  </style>" +
                "</head>" +
                "<body>" +
                "  <div class='container'>" +
                "    <div class='header'>" +
                "      <h1>QUY NHƠN TRAVEL</h1>" +
                "      <p>Hành Trình Du Lịch Thông Minh Của Bạn</p>" +
                "    </div>" +
                "    <div class='content'>" +
                "      <div class='welcome-text'>" +
                "        Xin chào <strong>" + customerName + "</strong>,<br><br>" +
                "        Cảm ơn bạn đã lựa chọn dịch vụ của Quy Nhơn Travel. Giao dịch thanh toán đặt tour của bạn đã được xác nhận thành công. Dưới đây là thông tin chi tiết vé du lịch của bạn:" +
                "      </div>" +
                "      " +
                "      <div class='card'>" +
                "        <div class='card-title'>Thông tin đặt tour</div>" +
                "        <div class='info-row'><span class='info-label'>Mã đặt tour:</span><span class='info-value' style='font-family: monospace; font-size:13px; color: #4a5568;'>" + bookingId + "</span></div>" +
                "        <div class='info-row'><span class='info-label'>Tên Tour:</span><span class='info-value'>" + tourTitle + "</span></div>" +
                "        <div class='info-row'><span class='info-label'>Ngày khởi hành:</span><span class='info-value'>" + startDate + "</span></div>" +
                "        <div class='info-row'><span class='info-label'>Số lượng khách:</span><span class='info-value'>" + adults + " Người lớn, " + children + " Trẻ em</span></div>" +
                "        <div class='info-row' style='margin-top: 15px; padding-top: 10px; border-top: 1px dashed #e2e8f0;'>" +
                "          <span class='info-label' style='font-weight: 700; color: #2d3748;'>Tổng thanh toán:</span>" +
                "          <span class='info-value total-price'><strong>" + totalPrice + "</strong></span>" +
                "        </div>" +
                "      </div>" +
                "      " +
                "      <div class='qr-section'>" +
                "        <div style='font-size: 15px; font-weight: 700; color: #2d3748;'>VÉ ĐIỆN TỬ & QR CHECK-IN</div>" +
                "        <p style='font-size: 13px; color: #718096; margin: 4px 0 12px 0;'>Vui lòng xuất trình mã QR này cho Hướng dẫn viên du lịch để quét check-in khi lên đoàn.</p>" +
                "        <img class='qr-image' src='" + qrCodeUrl + "' alt='Check-in QR Code'>" +
                "        <div class='qr-tip'>VÉ HỢP LỆ - TRẠNG THÁI: ĐÃ THANH TOÁN</div>" +
                "      </div>" +
                "    </div>" +
                "    <div class='footer'>" +
                "      <p>Mọi thắc mắc vui lòng liên hệ hotline: <strong>1900 1234</strong> hoặc gửi email về <a href='mailto:support@quynhon-travel.com'>support@quynhon-travel.com</a></p>" +
                "      <p>&copy; 2026 Quy Nhơn Travel. All rights reserved.</p>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }
}
