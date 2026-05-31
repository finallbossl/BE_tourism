package com.quynhontravel.tourism.modules.review.service;

import com.quynhontravel.tourism.common.enums.BookingStatus;
import com.quynhontravel.tourism.common.enums.SentimentType;
import com.quynhontravel.tourism.modules.ai.service.AiService;
import com.quynhontravel.tourism.modules.booking.repository.BookingRepository;
import com.quynhontravel.tourism.modules.review.dto.CreateReviewRequest;
import com.quynhontravel.tourism.modules.review.dto.ReviewResponse;
import com.quynhontravel.tourism.modules.review.entity.Review;
import com.quynhontravel.tourism.modules.review.repository.ReviewRepository;
import com.quynhontravel.tourism.modules.tour.entity.TourSchedule;
import com.quynhontravel.tourism.modules.tour.repository.TourScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final TourScheduleRepository tourScheduleRepository;
    private final AiService aiService;

    /**
     * Gửi đánh giá tour mới
     */
    @Transactional
    public ReviewResponse submitReview(UUID customerId, CreateReviewRequest request) {
        log.info("Khách hàng {} thực hiện gửi đánh giá cho Tour {}", customerId, request.getTourId());

        // 1. Kiểm tra xem khách hàng đã từng đặt tour này chưa (Bắt buộc phải có Booking trạng thái PAID hoặc COMPLETED)
        List<TourSchedule> schedules = tourScheduleRepository.findAllByTourId(request.getTourId());
        if (schedules.isEmpty()) {
            throw new RuntimeException("Tour này không có lịch trình hoạt động nào.");
        }

        List<UUID> scheduleIds = schedules.stream().map(TourSchedule::getId).collect(Collectors.toList());
        
        boolean hasBooked = bookingRepository.existsByCustomerIdAndScheduleIdInAndStatus(
                customerId, 
                scheduleIds, 
                BookingStatus.PAID
        );

        if (!hasBooked) {
            throw new RuntimeException("Bạn chỉ có thể đánh giá tour du lịch sau khi đã đặt và thanh toán thành công.");
        }

        // 2. Tạo bản ghi Review
        Review review = Review.builder()
                .customerId(customerId)
                .tourId(request.getTourId())
                .rating(request.getRating())
                .comment(request.getComment())
                .aiSentiment(SentimentType.NEUTRAL) // Cảm xúc mặc định ban đầu là NEUTRAL
                .isReported(false)
                .build();

        review = reviewRepository.save(review);
        log.info("Lưu đánh giá thành công cho Review ID: {}", review.getId());

        // 3. Phân tích cảm xúc bất đồng bộ bằng AI (Gemini) ngầm
        try {
            aiService.analyzeReviewSentiment(review.getId());
        } catch (Exception e) {
            log.error("Không thể kích hoạt tiến trình phân tích cảm xúc AI", e);
        }

        return mapToResponse(review);
    }

    /**
     * Lấy toàn bộ đánh giá hợp lệ của một tour (không bao gồm review bị báo cáo vi phạm)
     */
    @Transactional(readOnly = true)
    public List<ReviewResponse> getTourReviews(UUID tourId) {
        // Cần lấy tất cả reviews mà isReported = false hoặc null
        List<Review> reviews = reviewRepository.findAll().stream()
                .filter(r -> r.getTourId().equals(tourId) && (r.getIsReported() == null || !r.getIsReported()))
                .collect(Collectors.toList());

        return reviews.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    /**
     * Báo cáo đánh giá vi phạm (Report Review)
     */
    @Transactional
    public void reportReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá ID: " + reviewId));
        review.setIsReported(true);
        reviewRepository.save(review);
        log.info("Đã đánh dấu báo cáo vi phạm cho Review ID: {}", reviewId);
    }

    /**
     * Xóa đánh giá (Chỉ Admin/Manager thực hiện)
     */
    @Transactional
    public void deleteReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá ID: " + reviewId));
        reviewRepository.delete(review);
        log.info("Admin/Manager xóa thành công đánh giá ID: {}", reviewId);
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .customerId(review.getCustomerId())
                .tourId(review.getTourId())
                .rating(review.getRating())
                .comment(review.getComment())
                .aiSentiment(review.getAiSentiment())
                .isReported(review.getIsReported())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
