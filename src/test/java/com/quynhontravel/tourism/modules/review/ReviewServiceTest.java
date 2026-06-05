package com.quynhontravel.tourism.modules.review;

import com.quynhontravel.tourism.common.enums.BookingStatus;
import com.quynhontravel.tourism.common.enums.SentimentType;
import com.quynhontravel.tourism.modules.ai.service.AiService;
import com.quynhontravel.tourism.modules.booking.repository.BookingRepository;
import com.quynhontravel.tourism.modules.review.dto.CreateReviewRequest;
import com.quynhontravel.tourism.modules.review.dto.ReviewResponse;
import com.quynhontravel.tourism.modules.review.entity.Review;
import com.quynhontravel.tourism.modules.review.repository.ReviewRepository;
import com.quynhontravel.tourism.modules.review.service.ReviewService;
import com.quynhontravel.tourism.modules.tour.entity.Tour;
import com.quynhontravel.tourism.modules.tour.entity.TourSchedule;
import com.quynhontravel.tourism.modules.tour.repository.TourScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TourScheduleRepository tourScheduleRepository;
    @Mock
    private AiService aiService;

    @InjectMocks
    private ReviewService reviewService;

    private UUID customerId;
    private UUID tourId;
    private CreateReviewRequest request;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        tourId = UUID.randomUUID();

        request = CreateReviewRequest.builder()
                .tourId(tourId)
                .rating(5)
                .comment("Chuyến đi rất tuyệt vời!")
                .build();
    }

    @Test
    void submitReview_CustomerHasNotBooked_ThrowsException() {
        Tour tour = Tour.builder().id(tourId).build();
        TourSchedule schedule = TourSchedule.builder().id(UUID.randomUUID()).tour(tour).build();
        when(tourScheduleRepository.findAllByTourId(tourId)).thenReturn(List.of(schedule));
        
        when(bookingRepository.existsByCustomerIdAndScheduleIdInAndStatus(
                eq(customerId), any(), eq(BookingStatus.PAID)
        )).thenReturn(false);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            reviewService.submitReview(customerId, request);
        });

        assertEquals("Bạn chỉ có thể đánh giá tour du lịch sau khi đã đặt và thanh toán thành công.", exception.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void submitReview_Success() {
        UUID scheduleId = UUID.randomUUID();
        Tour tour = Tour.builder().id(tourId).build();
        TourSchedule schedule = TourSchedule.builder().id(scheduleId).tour(tour).build();
        when(tourScheduleRepository.findAllByTourId(tourId)).thenReturn(List.of(schedule));
        
        when(bookingRepository.existsByCustomerIdAndScheduleIdInAndStatus(
                eq(customerId), any(), eq(BookingStatus.PAID)
        )).thenReturn(true);

        Review savedReview = Review.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .tourId(tourId)
                .rating(5)
                .comment("Chuyến đi rất tuyệt vời!")
                .aiSentiment(SentimentType.NEUTRAL)
                .build();

        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        ReviewResponse response = reviewService.submitReview(customerId, request);

        assertNotNull(response);
        assertEquals(5, response.getRating());
        assertEquals("Chuyến đi rất tuyệt vời!", response.getComment());
        verify(reviewRepository, times(1)).save(any(Review.class));
        verify(aiService, times(1)).analyzeReviewSentiment(savedReview.getId());
    }
}
