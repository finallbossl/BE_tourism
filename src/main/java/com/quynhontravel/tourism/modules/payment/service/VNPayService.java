package com.quynhontravel.tourism.modules.payment.service;

import com.quynhontravel.tourism.common.enums.BookingStatus;
import com.quynhontravel.tourism.common.enums.PaymentStatus;
import com.quynhontravel.tourism.common.enums.UserRole;
import com.quynhontravel.tourism.modules.booking.entity.Booking;
import com.quynhontravel.tourism.modules.booking.repository.BookingRepository;
import com.quynhontravel.tourism.modules.payment.entity.Payment;
import com.quynhontravel.tourism.modules.payment.repository.PaymentRepository;
import com.quynhontravel.tourism.modules.user.entity.User;
import com.quynhontravel.tourism.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Value("${app.vnpay.tmn-code}")
    private String vnpTmnCode;

    @Value("${app.vnpay.hash-secret}")
    private String vnpHashSecret;

    @Value("${app.vnpay.pay-url}")
    private String vnpPayUrl;

    @Value("${app.vnpay.return-url}")
    private String vnpReturnUrl;

    /**
     * Tạo URL thanh toán VNPay cho Booking
     */
    @Transactional
    public String createPaymentUrl(UUID bookingId, String ipAddress) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new RuntimeException("Trạng thái đơn hàng không hợp lệ để thanh toán");
        }

        String vnp_TxnRef = booking.getId().toString();
        
        // Tạo hoặc cập nhật thông tin Payment ban đầu ở trạng thái PENDING
        paymentRepository.findByBookingId(bookingId).ifPresentOrElse(
            payment -> {
                payment.setVnpTxnRef(vnp_TxnRef);
                payment.setAmount(booking.getTotalPrice());
                paymentRepository.save(payment);
            },
            () -> {
                Payment payment = Payment.builder()
                        .bookingId(bookingId)
                        .vnpTxnRef(vnp_TxnRef)
                        .amount(booking.getTotalPrice())
                        .status(PaymentStatus.PENDING)
                        .build();
                paymentRepository.save(payment);
            }
        );

        Map<String, String> vnp_Params = new TreeMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnpTmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(booking.getTotalPrice().multiply(BigDecimal.valueOf(100)).longValue()));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan dat tour Quy Nhon Travel - Booking ID: " + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnpReturnUrl);
        vnp_Params.put("vnp_IpAddr", ipAddress);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime now = LocalDateTime.now();
        vnp_Params.put("vnp_CreateDate", now.format(formatter));
        vnp_Params.put("vnp_ExpireDate", now.plusMinutes(15).format(formatter)); // Thời hạn thanh toán 15 phút

        // Xây dựng câu truy vấn và Hash dữ liệu chữ ký
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<Map.Entry<String, String>> itr = vnp_Params.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, String> entry = itr.next();
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(encode(fieldValue));
                // Query string
                query.append(encode(fieldName));
                query.append('=');
                query.append(encode(fieldValue));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = hmacSHA512(vnpHashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return vnpPayUrl + "?" + queryUrl;
    }

    /**
     * Xử lý IPN Webhook phản hồi kết quả thanh toán từ VNPay (Áp dụng Idempotent Consumer)
     */
    @Transactional
    public String processIpn(Map<String, String> params) {
        String vnp_SecureHash = params.get("vnp_SecureHash");
        
        // 1. Xác thực chữ ký checksum
        Map<String, String> signParams = new TreeMap<>(params);
        signParams.remove("vnp_SecureHash");
        signParams.remove("vnp_SecureHashType");

        StringBuilder hashData = new StringBuilder();
        Iterator<Map.Entry<String, String>> itr = signParams.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, String> entry = itr.next();
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(encode(fieldValue));
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }

        String calculatedHash = hmacSHA512(vnpHashSecret, hashData.toString());
        if (!calculatedHash.equalsIgnoreCase(vnp_SecureHash)) {
            log.warn("VNPay IPN Signature Mismatch!");
            return "{\"RspCode\":\"97\",\"Message\":\"Invalid Signature\"}";
        }

        // 2. Tra cứu Booking & Giao dịch tương ứng
        String vnp_TxnRef = params.get("vnp_TxnRef");
        UUID bookingId = UUID.fromString(vnp_TxnRef);
        
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return "{\"RspCode\":\"01\",\"Message\":\"Order not found\"}";
        }

        // Kiểm tra số tiền giao dịch
        long vnpAmount = Long.parseLong(params.get("vnp_Amount"));
        long expectedAmount = booking.getTotalPrice().multiply(BigDecimal.valueOf(100)).longValue();
        if (vnpAmount != expectedAmount) {
            return "{\"RspCode\":\"04\",\"Message\":\"Invalid Amount\"}";
        }

        // 3. Thực hiện Idempotent Check: Nếu đơn hàng đã hoàn tất xử lý trước đó
        if (booking.getStatus() == BookingStatus.PAID) {
            return "{\"RspCode\":\"02\",\"Message\":\"Order already confirmed\"}";
        }

        // 4. Kiểm tra mã giao dịch VNPay
        String responseCode = params.get("vnp_ResponseCode");
        String vnp_TransactionNo = params.get("vnp_TransactionNo");

        Payment payment = paymentRepository.findByVnpTxnRef(vnp_TxnRef)
                .orElseThrow(() -> new RuntimeException("Giao dịch thanh toán không khớp"));

        if ("00".equals(responseCode)) {
            // Thanh toán THÀNH CÔNG
            booking.setStatus(BookingStatus.PAID);
            bookingRepository.save(booking);

            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setVnpTransactionNo(vnp_TransactionNo);
            payment.setPaymentTime(OffsetDateTime.now());
            paymentRepository.save(payment);

            // Tích điểm Loyalty Points thưởng cho khách hàng (1% số tiền thanh toán thực tế)
            User user = userRepository.findById(booking.getCustomerId()).orElse(null);
            if (user != null && user.getRole() == UserRole.ROLE_CUSTOMER) {
                int earnedPoints = booking.getTotalPrice().divide(BigDecimal.valueOf(1000)).intValue(); // 1 point mỗi 1,000 VND thanh toán
                user.setLoyaltyPoints(user.getLoyaltyPoints() + earnedPoints);
                userRepository.save(user);
                log.info("Cộng {} điểm tích lũy thành công cho khách hàng {}", earnedPoints, user.getEmail());
            }

            log.info("Xử lý thanh toán thành công IPN cho Booking ID: {}", bookingId);
            return "{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}";
        } else {
            // Thanh toán THẤT BẠI
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            payment.setStatus(PaymentStatus.FAILED);
            payment.setVnpTransactionNo(vnp_TransactionNo);
            paymentRepository.save(payment);

            log.info("VNPay báo lỗi thanh toán thất bại cho Booking ID: {}, Response Code: {}", bookingId, responseCode);
            return "{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}";
        }
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20");
        } catch (Exception e) {
            return "";
        }
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            log.error("Lỗi tính toán chữ ký HMAC-SHA512 VNPay", ex);
            return "";
        }
    }
}
