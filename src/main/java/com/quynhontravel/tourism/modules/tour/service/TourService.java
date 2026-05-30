package com.quynhontravel.tourism.modules.tour.service;

import com.quynhontravel.tourism.common.enums.TourScheduleStatus;
import com.quynhontravel.tourism.modules.tour.dto.TourDetailResponse;
import com.quynhontravel.tourism.modules.tour.dto.TourResponse;
import com.quynhontravel.tourism.modules.tour.dto.TourScheduleResponse;
import com.quynhontravel.tourism.modules.tour.entity.Tour;
import com.quynhontravel.tourism.modules.tour.entity.TourSchedule;
import com.quynhontravel.tourism.modules.tour.repository.TourRepository;
import com.quynhontravel.tourism.modules.tour.repository.TourScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourService {

    private final TourRepository tourRepository;
    private final TourScheduleRepository tourScheduleRepository;

    /**
     * Lấy toàn bộ danh sách tour hoạt động
     */
    @Transactional(readOnly = true)
    public List<TourResponse> getAllTours() {
        return tourRepository.findAllByIsDeletedFalse().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy thông tin chi tiết tour bằng Slug (có tích hợp Cache-Aside với Redis)
     */
    @Cacheable(value = "tours", key = "#slug", unless = "#result == null")
    @Transactional(readOnly = true)
    public TourDetailResponse getTourDetailBySlug(String slug) {
        Tour tour = tourRepository.findBySlugAndIsDeletedFalse(slug)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tour du lịch phù hợp: " + slug));

        // Lấy các lịch trình hoạt động sắp tới của tour
        List<TourSchedule> schedules = tourScheduleRepository
                .findAllByTourIdAndStartDateAfterOrderByStartDateAsc(tour.getId(), OffsetDateTime.now());

        List<TourScheduleResponse> scheduleResponses = schedules.stream()
                .filter(s -> s.getStatus() == TourScheduleStatus.AVAILABLE || s.getStatus() == TourScheduleStatus.FULL)
                .map(this::mapToScheduleResponse)
                .collect(Collectors.toList());

        return TourDetailResponse.builder()
                .tour(mapToResponse(tour))
                .schedules(scheduleResponses)
                .build();
    }

    public TourResponse mapToResponse(Tour tour) {
        return TourResponse.builder()
                .id(tour.getId())
                .title(tour.getTitle())
                .slug(tour.getSlug())
                .description(tour.getDescription())
                .basePrice(tour.getBasePrice())
                .durationDays(tour.getDurationDays())
                .durationNights(tour.getDurationNights())
                .coverImage(tour.getCoverImage())
                .imagesGallery(tour.getImagesGallery())
                .categoryName(tour.getCategory().getName())
                .categorySlug(tour.getCategory().getSlug())
                .build();
    }

    private TourScheduleResponse mapToScheduleResponse(TourSchedule schedule) {
        return TourScheduleResponse.builder()
                .id(schedule.getId())
                .startDate(schedule.getStartDate())
                .endDate(schedule.getEndDate())
                .maxSlots(schedule.getMaxSlots())
                .availableSlots(schedule.getAvailableSlots())
                .currentPrice(schedule.getCurrentPrice())
                .status(schedule.getStatus().name())
                .guideId(schedule.getGuideId())
                .build();
    }
}
