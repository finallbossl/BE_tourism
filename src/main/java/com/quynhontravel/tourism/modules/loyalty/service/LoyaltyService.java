package com.quynhontravel.tourism.modules.loyalty.service;

import com.quynhontravel.tourism.common.enums.BookingStatus;
import com.quynhontravel.tourism.common.enums.PaymentStatus;
import com.quynhontravel.tourism.common.exception.ResourceNotFoundException;
import com.quynhontravel.tourism.modules.booking.entity.Booking;
import com.quynhontravel.tourism.modules.booking.repository.BookingRepository;
import com.quynhontravel.tourism.modules.loyalty.dto.LoyaltyBalanceResponse;
import com.quynhontravel.tourism.modules.loyalty.dto.LoyaltyHistoryResponse;
import com.quynhontravel.tourism.modules.payment.entity.Payment;
import com.quynhontravel.tourism.modules.payment.repository.PaymentRepository;
import com.quynhontravel.tourism.modules.tour.entity.Tour;
import com.quynhontravel.tourism.modules.tour.entity.TourSchedule;
import com.quynhontravel.tourism.modules.tour.repository.TourRepository;
import com.quynhontravel.tourism.modules.tour.repository.TourScheduleRepository;
import com.quynhontravel.tourism.modules.user.entity.User;
import com.quynhontravel.tourism.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final TourRepository tourRepository;

    /**
     * Lấy số dư điểm tích lũy hiện tại của người dùng
     */
    @Transactional(readOnly = true)
    public LoyaltyBalanceResponse getBalance(UUID customerId) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin người dùng."));
        
        return LoyaltyBalanceResponse.builder()
                .points(user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : 0)
                .build();
    }

    /**
     * Lấy lịch sử biến động điểm tích lũy (Cộng điểm và trừ điểm)
     */
    @Transactional(readOnly = true)
    public List<LoyaltyHistoryResponse> getHistory(UUID customerId) {
        // Kiểm tra xem người dùng có tồn tại không
        if (!userRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Không tìm thấy thông tin người dùng.");
        }

        List<Booking> bookings = bookingRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId);
        List<LoyaltyHistoryResponse> history = new ArrayList<>();

        for (Booking booking : bookings) {
            // Lấy tên Tour để hiển thị trong mô tả giao dịch điểm
            String tourTitle = "Tour du lịch";
            try {
                TourSchedule schedule = tourScheduleRepository.findById(booking.getScheduleId()).orElse(null);
                if (schedule != null && schedule.getTour() != null) {
                    tourTitle = schedule.getTour().getTitle();
                }
            } catch (Exception e) {
                log.error("Lỗi khi truy vấn thông tin Tour cho Booking {}", booking.getId(), e);
            }

            // 1. Kiểm tra điểm đã sử dụng (SPENT)
            if (booking.getPointsUsed() != null && booking.getPointsUsed() > 0) {
                history.add(LoyaltyHistoryResponse.builder()
                        .transactionType("SPENT")
                        .points(booking.getPointsUsed())
                        .description("Sử dụng điểm quy đổi giảm giá cho tour: " + tourTitle)
                        .bookingId(booking.getId())
                        .createdAt(booking.getCreatedAt())
                        .build());
            }

            // 2. Kiểm tra điểm tích lũy được nhận (EARNED) khi đơn hàng PAID hoặc COMPLETED
            if (booking.getStatus() == BookingStatus.PAID || booking.getStatus() == BookingStatus.COMPLETED) {
                int earnedPoints = booking.getTotalPrice().divide(BigDecimal.valueOf(1000), 0, BigDecimal.ROUND_DOWN).intValue();
                if (earnedPoints > 0) {
                    OffsetDateTime earnedAt = booking.getUpdatedAt();
                    
                    // Tìm thông tin payment thành công để lấy thời gian thanh toán chính xác
                    Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
                    if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS && payment.getPaymentTime() != null) {
                        earnedAt = payment.getPaymentTime();
                    }

                    history.add(LoyaltyHistoryResponse.builder()
                            .transactionType("EARNED")
                            .points(earnedPoints)
                            .description("Tích điểm từ thanh toán thành công tour: " + tourTitle)
                            .bookingId(booking.getId())
                            .createdAt(earnedAt)
                            .build());
                }
            }
        }

        // Sắp xếp lịch sử theo thời gian giảm dần (mới nhất lên đầu)
        history.sort(Comparator.comparing(LoyaltyHistoryResponse::getCreatedAt).reversed());

        return history;
    }
}
