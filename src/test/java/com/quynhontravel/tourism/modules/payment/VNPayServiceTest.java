package com.quynhontravel.tourism.modules.payment;

import com.quynhontravel.tourism.common.enums.BookingStatus;
import com.quynhontravel.tourism.common.enums.PaymentStatus;
import com.quynhontravel.tourism.common.enums.UserRole;
import com.quynhontravel.tourism.modules.booking.entity.Booking;
import com.quynhontravel.tourism.modules.booking.repository.BookingRepository;
import com.quynhontravel.tourism.modules.notification.service.NotificationService;
import com.quynhontravel.tourism.modules.payment.entity.Payment;
import com.quynhontravel.tourism.modules.payment.repository.PaymentRepository;
import com.quynhontravel.tourism.modules.payment.service.VNPayService;
import com.quynhontravel.tourism.modules.user.entity.User;
import com.quynhontravel.tourism.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VNPayServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private VNPayService vnPayService;

    private String secret = "1234567890abcdef1234567890abcdef";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(vnPayService, "vnpHashSecret", secret);
    }

    @Test
    void processIpn_SignatureInvalid_ReturnsErrorCode97() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_SecureHash", "invalid_hash");

        String result = vnPayService.processIpn(params);

        assertTrue(result.contains("\"RspCode\":\"97\""));
    }

    @Test
    void processIpn_Success_ReturnsErrorCode00_AndCreditsLoyalty() {
        UUID bookingId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Booking booking = Booking.builder()
                .id(bookingId)
                .customerId(customerId)
                .totalPrice(BigDecimal.valueOf(1000000)) // 1,000,000 VND
                .status(BookingStatus.PENDING_PAYMENT)
                .build();

        User user = User.builder()
                .id(customerId)
                .role(UserRole.ROLE_CUSTOMER)
                .loyaltyPoints(0)
                .email("cust@example.com")
                .build();

        Payment payment = Payment.builder()
                .bookingId(bookingId)
                .vnpTxnRef(bookingId.toString())
                .status(PaymentStatus.PENDING)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(userRepository.findById(customerId)).thenReturn(Optional.of(user));
        when(paymentRepository.findByVnpTxnRef(bookingId.toString())).thenReturn(Optional.of(payment));

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", bookingId.toString());
        params.put("vnp_Amount", "100000000"); // 1,000,000 * 100
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "12345");

        // Calculate valid signature
        String secureHash = calculateHash(params);
        params.put("vnp_SecureHash", secureHash);

        String result = vnPayService.processIpn(params);

        assertTrue(result.contains("\"RspCode\":\"00\""));
        assertEquals(BookingStatus.PAID, booking.getStatus());
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(1000, user.getLoyaltyPoints()); // 1,000,000 / 1000 = 1000 points
        verify(userRepository, times(1)).save(user);
        verify(bookingRepository, times(1)).save(booking);
        verify(paymentRepository, times(1)).save(payment);
        verify(notificationService, times(1)).sendBookingConfirmationEmail(bookingId);
    }

    private String calculateHash(Map<String, String> params) {
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
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()).replace("+", "%20"));
                } catch (Exception e) {
                    // Ignore
                }
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }
        return hmacSHA512(secret, hashData.toString());
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            sha512_HMAC.init(secret_key);
            byte[] bytes = sha512_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
