package com.quynhontravel.tourism.modules.loyalty;

import com.quynhontravel.tourism.common.enums.BookingStatus;
import com.quynhontravel.tourism.common.enums.PaymentStatus;
import com.quynhontravel.tourism.modules.booking.entity.Booking;
import com.quynhontravel.tourism.modules.booking.repository.BookingRepository;
import com.quynhontravel.tourism.modules.loyalty.dto.LoyaltyBalanceResponse;
import com.quynhontravel.tourism.modules.loyalty.dto.LoyaltyHistoryResponse;
import com.quynhontravel.tourism.modules.loyalty.service.LoyaltyService;
import com.quynhontravel.tourism.modules.payment.entity.Payment;
import com.quynhontravel.tourism.modules.payment.repository.PaymentRepository;
import com.quynhontravel.tourism.modules.tour.entity.Tour;
import com.quynhontravel.tourism.modules.tour.entity.TourSchedule;
import com.quynhontravel.tourism.modules.tour.repository.TourRepository;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoyaltyServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private TourScheduleRepository tourScheduleRepository;
    @Mock
    private TourRepository tourRepository;

    @InjectMocks
    private LoyaltyService loyaltyService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("test@example.com")
                .loyaltyPoints(150)
                .build();
    }

    @Test
    void getBalance_ShouldReturnPoints() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        LoyaltyBalanceResponse response = loyaltyService.getBalance(userId);

        assertNotNull(response);
        assertEquals(150, response.getPoints());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void getHistory_ShouldReturnSpentAndEarnedPoints() {
        UUID bookingId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        UUID tourId = UUID.randomUUID();

        Booking booking = Booking.builder()
                .id(bookingId)
                .customerId(userId)
                .scheduleId(scheduleId)
                .pointsUsed(50)
                .totalPrice(BigDecimal.valueOf(2500000))
                .status(BookingStatus.PAID)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();

        Tour tour = Tour.builder()
                .id(tourId)
                .title("Kỳ Co Eo Gió")
                .build();

        TourSchedule schedule = TourSchedule.builder()
                .id(scheduleId)
                .tour(tour)
                .build();

        Payment payment = Payment.builder()
                .bookingId(bookingId)
                .status(PaymentStatus.SUCCESS)
                .paymentTime(OffsetDateTime.now())
                .build();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(bookingRepository.findAllByCustomerIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(booking));
        when(tourScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(payment));

        List<LoyaltyHistoryResponse> history = loyaltyService.getHistory(userId);

        assertNotNull(history);
        assertEquals(2, history.size()); // 1 spent and 1 earned

        LoyaltyHistoryResponse earned = history.stream().filter(h -> h.getTransactionType().equals("EARNED")).findFirst().orElse(null);
        assertNotNull(earned);
        assertEquals(2500, earned.getPoints());
        assertTrue(earned.getDescription().contains("Kỳ Co Eo Gió"));

        LoyaltyHistoryResponse spent = history.stream().filter(h -> h.getTransactionType().equals("SPENT")).findFirst().orElse(null);
        assertNotNull(spent);
        assertEquals(50, spent.getPoints());
    }
}
