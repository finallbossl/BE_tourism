package com.quynhontravel.tourism.modules.guide.service;

import com.quynhontravel.tourism.common.enums.BookingStatus;
import com.quynhontravel.tourism.modules.booking.entity.Booking;
import com.quynhontravel.tourism.modules.booking.repository.BookingRepository;
import com.quynhontravel.tourism.modules.guide.dto.TourGuideCheckinResponse;
import com.quynhontravel.tourism.modules.user.entity.User;
import com.quynhontravel.tourism.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TourGuideService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    /**
     * Thực hiện Check-in hành khách bằng QR Code (Booking ID)
     */
    @Transactional
    public TourGuideCheckinResponse checkIn(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã đặt chỗ phù hợp"));

        if (booking.getStatus() != BookingStatus.PAID) {
            throw new RuntimeException("Đơn đặt tour chưa được thanh toán thành công (Trạng thái hiện tại: " + booking.getStatus() + ")");
        }

        if (booking.getCheckInAt() != null) {
            throw new RuntimeException("Vé này đã được Check-in trước đó vào lúc " + booking.getCheckInAt());
        }

        // Cập nhật thông tin check-in hành khách
        booking.setCheckInAt(OffsetDateTime.now());
        bookingRepository.save(booking);

        User user = userRepository.findById(booking.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dữ liệu thông tin khách hàng"));

        log.info("Tour Guide đã thực hiện check-in thành công cho Booking ID: {}", bookingId);

        return TourGuideCheckinResponse.builder()
                .bookingId(booking.getId())
                .customerName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .quantityAdults(booking.getQuantityAdults())
                .quantityChildren(booking.getQuantityChildren())
                .status("CHECKED_IN")
                .build();
    }
}
