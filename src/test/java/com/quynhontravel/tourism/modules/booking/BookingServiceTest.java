package com.quynhontravel.tourism.modules.booking;

import com.quynhontravel.tourism.common.enums.BookingStatus;
import com.quynhontravel.tourism.common.enums.TourScheduleStatus;
import com.quynhontravel.tourism.modules.booking.dto.BookingResponse;
import com.quynhontravel.tourism.modules.booking.dto.CalculateDiscountRequest;
import com.quynhontravel.tourism.modules.booking.dto.CalculateDiscountResponse;
import com.quynhontravel.tourism.modules.booking.dto.CreateBookingRequest;
import com.quynhontravel.tourism.modules.booking.entity.Booking;
import com.quynhontravel.tourism.modules.booking.repository.BookingRepository;
import com.quynhontravel.tourism.modules.booking.service.BookingService;
import com.quynhontravel.tourism.modules.tour.entity.TourSchedule;
import com.quynhontravel.tourism.modules.tour.repository.TourScheduleRepository;
import com.quynhontravel.tourism.modules.user.entity.User;
import com.quynhontravel.tourism.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TourScheduleRepository tourScheduleRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    private UUID customerId;
    private UUID scheduleId;
    private User user;
    private TourSchedule schedule;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        scheduleId = UUID.randomUUID();

        user = User.builder()
                .id(customerId)
                .loyaltyPoints(100)
                .build();

        schedule = TourSchedule.builder()
                .id(scheduleId)
                .maxSlots(20)
                .availableSlots(10)
                .currentPrice(BigDecimal.valueOf(1500000))
                .status(TourScheduleStatus.AVAILABLE)
                .build();
    }

    @Test
    void createBooking_WithoutPoints_Success() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setScheduleId(scheduleId);
        request.setQuantityAdults(2);
        request.setQuantityChildren(1);
        request.setPointsUsed(0);

        when(userRepository.findById(customerId)).thenReturn(Optional.of(user));
        when(tourScheduleRepository.findByIdWithLock(scheduleId)).thenReturn(Optional.of(schedule));

        Booking savedBooking = Booking.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .scheduleId(scheduleId)
                .quantityAdults(2)
                .quantityChildren(1)
                .totalPrice(BigDecimal.valueOf(4500000))
                .pointsUsed(0)
                .status(BookingStatus.PENDING_PAYMENT)
                .build();

        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        BookingResponse response = bookingService.createBooking(request, customerId);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(4500000), response.getTotalPrice());
        assertEquals(7, schedule.getAvailableSlots());
        verify(bookingRepository, times(1)).save(any(Booking.class));
        verify(tourScheduleRepository, times(1)).save(schedule);
    }

    @Test
    void createBooking_WithPoints_Success() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setScheduleId(scheduleId);
        request.setQuantityAdults(2);
        request.setQuantityChildren(0);
        request.setPointsUsed(50);

        when(userRepository.findById(customerId)).thenReturn(Optional.of(user));
        when(tourScheduleRepository.findByIdWithLock(scheduleId)).thenReturn(Optional.of(schedule));

        Booking savedBooking = Booking.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .scheduleId(scheduleId)
                .quantityAdults(2)
                .quantityChildren(0)
                .totalPrice(BigDecimal.valueOf(2950000))
                .pointsUsed(50)
                .status(BookingStatus.PENDING_PAYMENT)
                .build();

        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        BookingResponse response = bookingService.createBooking(request, customerId);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(2950000), response.getTotalPrice());
        assertEquals(50, user.getLoyaltyPoints());
        verify(userRepository, times(1)).save(user);
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    void createBooking_InsufficientPoints_ThrowsException() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setScheduleId(scheduleId);
        request.setQuantityAdults(1);
        request.setQuantityChildren(0);
        request.setPointsUsed(200);

        when(userRepository.findById(customerId)).thenReturn(Optional.of(user));
        when(tourScheduleRepository.findByIdWithLock(scheduleId)).thenReturn(Optional.of(schedule));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            bookingService.createBooking(request, customerId);
        });

        assertEquals("Tài khoản không đủ điểm tích lũy để thực hiện giảm giá.", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void calculateDiscount_Success() {
        CalculateDiscountRequest request = CalculateDiscountRequest.builder()
                .scheduleId(scheduleId)
                .quantityAdults(2)
                .quantityChildren(0)
                .pointsUsed(50)
                .build();

        when(userRepository.findById(customerId)).thenReturn(Optional.of(user));
        when(tourScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));

        CalculateDiscountResponse response = bookingService.calculateDiscount(request, customerId);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(3000000), response.getTotalRawPrice());
        assertEquals(BigDecimal.valueOf(50000), response.getDiscountAmount());
        assertEquals(BigDecimal.valueOf(2950000), response.getFinalPrice());
        assertEquals(50, response.getPointsBalance());
    }
}
