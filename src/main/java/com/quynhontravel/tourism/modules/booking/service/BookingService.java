package com.quynhontravel.tourism.modules.booking.service;

import com.quynhontravel.tourism.common.enums.BookingStatus;
import com.quynhontravel.tourism.common.enums.TourScheduleStatus;
import com.quynhontravel.tourism.modules.booking.dto.BookingResponse;
import com.quynhontravel.tourism.modules.booking.dto.CreateBookingRequest;
import com.quynhontravel.tourism.modules.booking.entity.Booking;
import com.quynhontravel.tourism.modules.booking.repository.BookingRepository;
import com.quynhontravel.tourism.modules.tour.entity.TourSchedule;
import com.quynhontravel.tourism.modules.tour.repository.TourScheduleRepository;
import com.quynhontravel.tourism.modules.user.entity.User;
import com.quynhontravel.tourism.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final UserRepository userRepository;

    /**
     * Tạo mới booking đặt tour (Có kiểm tra tranh chấp chỗ ngồi bằng Pessimistic Lock và khấu trừ điểm loyalty)
     */
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, UUID customerId) {
        // 1. Tìm thông tin khách hàng và kiểm tra hợp lệ
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng."));

        // 2. Lock bản ghi TourSchedule bằng Pessimistic Lock để tránh overbooking khi có luồng đồng thời
        TourSchedule schedule = tourScheduleRepository.findByIdWithLock(request.getScheduleId())
                .orElseThrow(() -> new RuntimeException("Lịch trình khởi hành không tồn tại."));

        if (schedule.getStatus() == TourScheduleStatus.CANCELLED || schedule.getStatus() == TourScheduleStatus.DEPARTED) {
            throw new RuntimeException("Lịch trình này không còn chấp nhận đặt chỗ.");
        }

        int totalSeatsRequested = request.getQuantityAdults() + request.getQuantityChildren();
        if (schedule.getAvailableSlots() < totalSeatsRequested) {
            throw new RuntimeException("Lịch trình không còn đủ chỗ trống (Còn lại: " + schedule.getAvailableSlots() + ").");
        }

        // 3. Khấu trừ chỗ ngồi trống của lịch trình
        schedule.setAvailableSlots(schedule.getAvailableSlots() - totalSeatsRequested);
        if (schedule.getAvailableSlots() == 0) {
            schedule.setStatus(TourScheduleStatus.FULL);
        }
        tourScheduleRepository.save(schedule);

        // 4. Tính toán tổng chi phí đặt tour
        BigDecimal seatPrice = schedule.getCurrentPrice();
        BigDecimal totalRawPrice = seatPrice.multiply(BigDecimal.valueOf(totalSeatsRequested));

        // 5. Khấu trừ loyalty points nếu có yêu cầu (1 point = 1,000 VNĐ)
        BigDecimal discount = BigDecimal.ZERO;
        int pointsToUse = request.getPointsUsed() != null ? request.getPointsUsed() : 0;
        if (pointsToUse > 0) {
            if (user.getLoyaltyPoints() < pointsToUse) {
                throw new RuntimeException("Tài khoản không đủ điểm tích lũy để thực hiện giảm giá.");
            }
            user.setLoyaltyPoints(user.getLoyaltyPoints() - pointsToUse);
            userRepository.save(user);
            discount = BigDecimal.valueOf(pointsToUse).multiply(BigDecimal.valueOf(1000));
        }

        BigDecimal finalPrice = totalRawPrice.subtract(discount);
        if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
            finalPrice = BigDecimal.ZERO;
        }

        // 6. Lưu thông tin Booking vào database
        Booking booking = Booking.builder()
                .customerId(customerId)
                .scheduleId(schedule.getId())
                .quantityAdults(request.getQuantityAdults())
                .quantityChildren(request.getQuantityChildren())
                .totalPrice(finalPrice)
                .pointsUsed(pointsToUse)
                .status(BookingStatus.PENDING_PAYMENT)
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Khách hàng {} tạo thành công booking {} trị giá {}", customerId, savedBooking.getId(), finalPrice);

        return mapToResponse(savedBooking);
    }

    /**
     * Lấy lịch sử đặt tour của khách hàng
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyHistory(UUID customerId) {
        return bookingRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .customerId(booking.getCustomerId())
                .scheduleId(booking.getScheduleId())
                .quantityAdults(booking.getQuantityAdults())
                .quantityChildren(booking.getQuantityChildren())
                .totalPrice(booking.getTotalPrice())
                .pointsUsed(booking.getPointsUsed())
                .status(booking.getStatus().name())
                .checkInAt(booking.getCheckInAt())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
